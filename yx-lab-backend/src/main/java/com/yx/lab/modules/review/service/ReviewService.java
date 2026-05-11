package com.yx.lab.modules.review.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.service.ReportService;
import com.yx.lab.modules.review.dto.ReviewCommand;
import com.yx.lab.modules.review.dto.ReviewItemCommand;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.service.LabSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 检测结果审查服务。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRecordMapper reviewRecordMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final LabSampleMapper labSampleMapper;

    private final LabSampleService labSampleService;

    private final ReportService reportService;

    /**
     * 分页查询审查记录。
     */
    public PageResult<ReviewRecord> page(ReviewQuery query) {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        Page<ReviewRecord> page = reviewRecordMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<ReviewRecord>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(ReviewRecord::getSampleNo, query.getKeyword())
                                .or()
                                .like(ReviewRecord::getSealNo, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getReviewResult()), ReviewRecord::getReviewResult, query.getReviewResult())
                        .eq(Boolean.TRUE.equals(query.getMine()), ReviewRecord::getReviewerId, currentUser.getUserId())
                        .orderByDesc(ReviewRecord::getReviewTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /**
     * 提交审查结果。
     */
    @Transactional(rollbackFor = Exception.class)
    public void review(ReviewCommand command) {
        DetectionRecord record = detectionRecordMapper.selectById(command.getDetectionRecordId());
        if (record == null) {
            throw new BusinessException("检测记录不存在");
        }
        if (!LabWorkflowConstants.canReviewDetection(record.getDetectionStatus())) {
            throw new BusinessException("当前检测记录不在待审查状态");
        }

        LabSample sample = labSampleMapper.selectById(record.getSampleId());
        if (sample == null) {
            throw new BusinessException("样品不存在");
        }

        CurrentUser currentUser = SecurityContext.getCurrentUser();
        List<DetectionItem> recordItems = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, record.getId())
                .orderByAsc(DetectionItem::getCreatedTime));
        if (recordItems.isEmpty()) {
            throw new BusinessException("当前检测记录下没有可审核的子流程");
        }

        List<DetectionItem> pendingReviewItems = recordItems.stream()
                .filter(item -> LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus()))
                .collect(Collectors.toList());
        if (pendingReviewItems.isEmpty()) {
            throw new BusinessException("当前检测记录下没有待审核的子流程");
        }

        Map<Long, ReviewItemCommand> reviewItemMap = validateReviewItems(command, pendingReviewItems);
        boolean anyRejected = false;
        StringBuilder rejectSummaryBuilder = new StringBuilder();
        for (DetectionItem item : pendingReviewItems) {
            ReviewItemCommand itemCommand = reviewItemMap.get(item.getId());
            if (itemCommand == null) {
                continue;
            }
            if (LabWorkflowConstants.ReviewResult.APPROVED.equals(itemCommand.getReviewResult())) {
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.APPROVED);
            } else {
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.REJECTED);
                anyRejected = true;
                appendRejectSummary(rejectSummaryBuilder, item, itemCommand);
            }
            detectionItemMapper.updateById(item);
        }

        String rejectReason = rejectSummaryBuilder.toString();
        String reviewRemark = StrUtil.trim(command.getReviewRemark());
        String overallReviewResult = anyRejected
                ? LabWorkflowConstants.ReviewResult.REJECTED
                : LabWorkflowConstants.ReviewResult.APPROVED;

        ReviewRecord reviewRecord = new ReviewRecord();
        reviewRecord.setDetectionRecordId(record.getId());
        reviewRecord.setSampleId(record.getSampleId());
        reviewRecord.setSampleNo(record.getSampleNo());
        reviewRecord.setSealNo(sample.getSealNo());
        reviewRecord.setReviewerId(currentUser.getUserId());
        reviewRecord.setReviewerName(currentUser.getRealName());
        reviewRecord.setReviewTime(LocalDateTime.now());
        reviewRecord.setReviewResult(overallReviewResult);
        reviewRecord.setRejectReason(rejectReason);
        reviewRecord.setReviewRemark(reviewRemark);
        reviewRecordMapper.insert(reviewRecord);

        if (!anyRejected && recordItems.stream().allMatch(item -> LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()))) {
            record.setDetectionStatus(LabWorkflowConstants.DetectionStatus.APPROVED);
            record.setAbnormalRemark(StrUtil.blankToDefault(reviewRemark, "全部子流程审核通过"));
            detectionRecordMapper.updateById(record);
            labSampleService.updateStatus(
                    sample.getId(),
                    LabWorkflowConstants.SampleStatus.COMPLETED,
                    record.getDetectionResult(),
                    "审查通过：封签号=" + sample.getSealNo()
                            + "，审查人=" + currentUser.getRealName()
                            + "，结果=" + LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult()));
            reportService.createApprovedReport(sample, record);
            return;
        }

        record.setDetectionStatus(resolveRetestRecordStatus(recordItems));
        record.setDetectionResult(null);
        record.setAbnormalRemark(buildRetestSummary(rejectReason, reviewRemark));
        detectionRecordMapper.updateById(record);

        labSampleService.updateStatus(
                sample.getId(),
                LabWorkflowConstants.SampleStatus.RETEST,
                buildRetestSummary(rejectReason, reviewRemark),
                "审查驳回：封签号=" + sample.getSealNo()
                        + "，审查人=" + currentUser.getRealName()
                        + "，原因=" + rejectReason);
    }

    private Map<Long, ReviewItemCommand> validateReviewItems(ReviewCommand command, List<DetectionItem> pendingReviewItems) {
        if (command.getItems() == null || command.getItems().isEmpty()) {
            throw new BusinessException("请至少审核一条子流程");
        }
        Map<Long, DetectionItem> pendingItemMap = pendingReviewItems.stream()
                .collect(Collectors.toMap(DetectionItem::getId, item -> item, (left, right) -> left));
        Map<Long, ReviewItemCommand> reviewItemMap = new LinkedHashMap<>();
        for (ReviewItemCommand itemCommand : command.getItems()) {
            if (reviewItemMap.put(itemCommand.getItemId(), itemCommand) != null) {
                throw new BusinessException("同一子流程不能重复审核");
            }
            DetectionItem item = pendingItemMap.get(itemCommand.getItemId());
            if (item == null) {
                throw new BusinessException("当前子流程不属于本次待审核范围：" + itemCommand.getItemId());
            }
            if (!LabWorkflowConstants.ReviewResult.APPROVED.equals(itemCommand.getReviewResult())
                    && !LabWorkflowConstants.ReviewResult.REJECTED.equals(itemCommand.getReviewResult())) {
                throw new BusinessException("子流程审核结果只能为通过或驳回");
            }
            if (LabWorkflowConstants.ReviewResult.REJECTED.equals(itemCommand.getReviewResult())
                    && StrUtil.isBlank(itemCommand.getRejectReason())) {
                throw new BusinessException("子流程驳回时必须填写驳回原因：" + item.getParameterName());
            }
        }
        if (reviewItemMap.size() != pendingReviewItems.size()) {
            throw new BusinessException("请完成当前主流程下全部待审核子流程的审核判定");
        }
        return reviewItemMap;
    }

    private void appendRejectSummary(StringBuilder builder, DetectionItem item, ReviewItemCommand itemCommand) {
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append(StrUtil.blankToDefault(item.getParameterName(), "未命名参数"))
                .append("：")
                .append(StrUtil.blankToDefault(StrUtil.trim(itemCommand.getRejectReason()), "审核未通过"));
        if (StrUtil.isNotBlank(itemCommand.getReviewRemark())) {
            builder.append("（")
                    .append(StrUtil.trim(itemCommand.getReviewRemark()))
                    .append("）");
        }
    }

    private String resolveRetestRecordStatus(List<DetectionItem> items) {
        boolean hasUnassigned = items.stream().anyMatch(item -> item.getDetectorId() == null
                || LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN.equals(item.getItemStatus()));
        return hasUnassigned ? LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN : LabWorkflowConstants.DetectionStatus.WAIT_DETECT;
    }

    private String buildRetestSummary(String rejectReason, String reviewRemark) {
        if (StrUtil.isNotBlank(rejectReason)) {
            return "退回重检：" + rejectReason;
        }
        if (StrUtil.isNotBlank(reviewRemark)) {
            return "退回重检：" + reviewRemark;
        }
        return "退回重检";
    }
}
