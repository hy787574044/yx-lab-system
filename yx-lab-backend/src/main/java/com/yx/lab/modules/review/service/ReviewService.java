package com.yx.lab.modules.review.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.DataScopeHelper;
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
import com.yx.lab.modules.system.entity.LabFlowNode;
import com.yx.lab.modules.system.service.FlowConfigManagementService;
import com.yx.lab.modules.system.service.FlowNodeGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 结果审查服务，负责对子流程检测结果逐项审核并驱动主流程回退或通过。
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

    private final FlowNodeGateService flowNodeGateService;

    private final DataScopeHelper dataScopeHelper;

    /**
     * 分页查询审查记录列表。
     *
     * @param query 查询条件
     * @return 审查记录分页结果
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
                        .eq(Boolean.FALSE.equals(query.getMine()) && dataScopeHelper.onlySelfScope(),
                                ReviewRecord::getReviewerId,
                                dataScopeHelper.currentUserId())
                        .orderByDesc(ReviewRecord::getReviewTime));
        fillReviewRecordSummaries(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Transactional(rollbackFor = Exception.class)
    /**
     * 对检测主流程及其子流程执行审查通过或驳回。
     *
     * @param command 审查参数
     */
    public void review(ReviewCommand command) {
        DetectionRecord record = detectionRecordMapper.selectById(command.getDetectionRecordId());
        if (record == null) {
            throw new BusinessException("检测记录不存在。");
        }
        if (!LabWorkflowConstants.canReviewDetection(record.getDetectionStatus())) {
            throw new BusinessException("当前检测记录不在待审查状态。");
        }

        LabSample sample = labSampleMapper.selectById(record.getSampleId());
        if (sample == null) {
            throw new BusinessException("样品不存在。");
        }

        CurrentUser currentUser = SecurityContext.getCurrentUser();
        Set<Long> approvedReviewNodeIds = loadApprovedReviewNodeIds(record.getId());
        LabFlowNode currentReviewNode = flowNodeGateService.resolveCurrentRequiredNode(
                sample.getReviewFlowId(),
                FlowConfigManagementService.FLOW_TYPE_REVIEW,
                currentUser,
                "审核",
                approvedReviewNodeIds);
        List<DetectionItem> recordItems = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, record.getId())
                .orderByAsc(DetectionItem::getCreatedTime));
        if (recordItems.isEmpty()) {
            throw new BusinessException("当前检测记录下没有可审查的子流程。");
        }

        List<DetectionItem> pendingReviewItems = recordItems.stream()
                .filter(item -> LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus()))
                .collect(Collectors.toList());
        if (pendingReviewItems.isEmpty()) {
            throw new BusinessException("当前检测记录下没有待审查的子流程。");
        }

        // 审查以“待审子流程”为最小单位，支持逐项通过或驳回，再汇总回主流程。
        Map<Long, ReviewItemCommand> reviewItemMap = validateReviewItems(command, pendingReviewItems);
        boolean anyRejected = hasRejectedItem(reviewItemMap);
        StringBuilder rejectSummaryBuilder = new StringBuilder();
        if (anyRejected) {
            applyRejectedReviewItems(pendingReviewItems, reviewItemMap, rejectSummaryBuilder);
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
        fillReviewNodeInfo(reviewRecord, sample, currentReviewNode);
        reviewRecord.setReviewerId(currentUser.getUserId());
        reviewRecord.setReviewerName(currentUser.getRealName());
        reviewRecord.setReviewTime(LocalDateTime.now());
        reviewRecord.setReviewResult(overallReviewResult);
        reviewRecord.setRejectReason(rejectReason);
        reviewRecord.setReviewRemark(reviewRemark);
        reviewRecordMapper.insert(reviewRecord);

        if (currentReviewNode != null && LabWorkflowConstants.ReviewResult.APPROVED.equals(overallReviewResult)) {
            approvedReviewNodeIds.add(currentReviewNode.getId());
        }

        // 多级审核时，前置必审节点只记录节点通过，不改变检测项最终状态，也不生成报告。
        if (!anyRejected && flowNodeGateService.hasRemainingRequiredNode(
                sample.getReviewFlowId(),
                FlowConfigManagementService.FLOW_TYPE_REVIEW,
                approvedReviewNodeIds)) {
            record.setAbnormalRemark(buildPendingReviewSummary(currentReviewNode));
            detectionRecordMapper.updateById(record);
            return;
        }

        if (!anyRejected) {
            markItemsApproved(pendingReviewItems);
        }

        // 只有全部子流程都审核通过，且审核流程全部必审节点通过，主流程和样品状态才会整体流转到“已完成/已出报告”。
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

        // 只要存在任一驳回子流程，就整体回退到检测环节，由原检测人员重新化验后再提交。
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
            throw new BusinessException("请至少审核一条子流程。");
        }
        Map<Long, DetectionItem> pendingItemMap = pendingReviewItems.stream()
                .collect(Collectors.toMap(DetectionItem::getId, item -> item, (left, right) -> left));
        Map<Long, ReviewItemCommand> reviewItemMap = new LinkedHashMap<>();
        for (ReviewItemCommand itemCommand : command.getItems()) {
            if (reviewItemMap.put(itemCommand.getItemId(), itemCommand) != null) {
                throw new BusinessException("同一子流程不能重复审核。");
            }
            DetectionItem item = pendingItemMap.get(itemCommand.getItemId());
            if (item == null) {
                throw new BusinessException("当前子流程不属于本次待审核范围：" + itemCommand.getItemId());
            }
            if (!LabWorkflowConstants.ReviewResult.APPROVED.equals(itemCommand.getReviewResult())
                    && !LabWorkflowConstants.ReviewResult.REJECTED.equals(itemCommand.getReviewResult())) {
                throw new BusinessException("子流程审核结果只允许为通过或驳回。");
            }
            if (LabWorkflowConstants.ReviewResult.REJECTED.equals(itemCommand.getReviewResult())
                    && StrUtil.isBlank(itemCommand.getRejectReason())) {
                throw new BusinessException("子流程驳回时必须填写驳回原因：" + item.getParameterName());
            }
        }
        if (reviewItemMap.size() != pendingReviewItems.size()) {
            throw new BusinessException("请完成当前主流程下全部待审查子流程的审核判定。");
        }
        return reviewItemMap;
    }

    private Set<Long> loadApprovedReviewNodeIds(Long detectionRecordId) {
        if (detectionRecordId == null) {
            return new HashSet<>();
        }
        ReviewRecord latestRejectedRecord = reviewRecordMapper.selectOne(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getDetectionRecordId, detectionRecordId)
                .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.REJECTED)
                .orderByDesc(ReviewRecord::getReviewTime)
                .orderByDesc(ReviewRecord::getCreatedTime)
                .orderByDesc(ReviewRecord::getId)
                .last("LIMIT 1"));
        return reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
                        .eq(ReviewRecord::getDetectionRecordId, detectionRecordId)
                        .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.APPROVED)
                        .gt(latestRejectedRecord != null && latestRejectedRecord.getId() != null,
                                ReviewRecord::getId,
                                latestRejectedRecord == null ? null : latestRejectedRecord.getId())
                        .isNotNull(ReviewRecord::getFlowNodeId))
                .stream()
                .map(ReviewRecord::getFlowNodeId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean hasRejectedItem(Map<Long, ReviewItemCommand> reviewItemMap) {
        return reviewItemMap.values().stream()
                .anyMatch(item -> LabWorkflowConstants.ReviewResult.REJECTED.equals(item.getReviewResult()));
    }

    private void applyRejectedReviewItems(List<DetectionItem> pendingReviewItems,
                                          Map<Long, ReviewItemCommand> reviewItemMap,
                                          StringBuilder rejectSummaryBuilder) {
        for (DetectionItem item : pendingReviewItems) {
            ReviewItemCommand itemCommand = reviewItemMap.get(item.getId());
            if (itemCommand == null) {
                continue;
            }
            if (LabWorkflowConstants.ReviewResult.APPROVED.equals(itemCommand.getReviewResult())) {
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.APPROVED);
            } else {
                item.setItemStatus(LabWorkflowConstants.DetectionStatus.REJECTED);
                appendRejectSummary(rejectSummaryBuilder, item, itemCommand);
            }
            detectionItemMapper.updateById(item);
        }
    }

    private void markItemsApproved(List<DetectionItem> pendingReviewItems) {
        for (DetectionItem item : pendingReviewItems) {
            item.setItemStatus(LabWorkflowConstants.DetectionStatus.APPROVED);
            detectionItemMapper.updateById(item);
        }
    }

    private void fillReviewNodeInfo(ReviewRecord reviewRecord, LabSample sample, LabFlowNode currentReviewNode) {
        reviewRecord.setFlowId(sample.getReviewFlowId());
        if (currentReviewNode == null) {
            return;
        }
        reviewRecord.setFlowNodeId(currentReviewNode.getId());
        reviewRecord.setFlowNodeName(currentReviewNode.getNodeName());
        reviewRecord.setFlowNodeOrder(currentReviewNode.getNodeOrder());
        reviewRecord.setRequiredFlag(currentReviewNode.getRequiredFlag());
    }

    private String buildPendingReviewSummary(LabFlowNode currentReviewNode) {
        if (currentReviewNode == null || StrUtil.isBlank(currentReviewNode.getNodeName())) {
            return "当前审核节点已通过，待下一审核节点处理";
        }
        return currentReviewNode.getNodeName() + "已通过，待下一审核节点处理";
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

    private void fillReviewRecordSummaries(List<ReviewRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> detectionRecordIds = records.stream()
                .map(ReviewRecord::getDetectionRecordId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (detectionRecordIds.isEmpty()) {
            return;
        }

        Map<Long, DetectionRecord> detectionRecordMap = detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                        .in(DetectionRecord::getId, detectionRecordIds))
                .stream()
                .collect(Collectors.toMap(DetectionRecord::getId, item -> item, (left, right) -> left));
        Map<Long, List<DetectionItem>> itemGroup = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                        .in(DetectionItem::getRecordId, detectionRecordIds))
                .stream()
                .collect(Collectors.groupingBy(DetectionItem::getRecordId));

        for (ReviewRecord record : records) {
            Long detectionRecordId = record.getDetectionRecordId();
            if (detectionRecordId == null) {
                continue;
            }
            DetectionRecord detectionRecord = detectionRecordMap.get(detectionRecordId);
            List<DetectionItem> items = itemGroup.getOrDefault(detectionRecordId, Collections.emptyList());
            if (detectionRecord != null) {
                record.setDetectionTypeName(detectionRecord.getDetectionTypeName());
            }
            record.setDetectorName(resolveDetectorSummary(items, detectionRecord == null ? null : detectionRecord.getDetectorName()));
            record.setParameterCount(items.size());
            record.setCompletedCount(countCompletedItems(items));
        }
    }

    private String resolveDetectorSummary(List<DetectionItem> items, String fallbackDetectorName) {
        LinkedHashSet<String> detectorNames = items.stream()
                .map(DetectionItem::getDetectorName)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!detectorNames.isEmpty()) {
            return String.join("、", detectorNames);
        }
        return StrUtil.trim(fallbackDetectorName);
    }

    private int countCompletedItems(List<DetectionItem> items) {
        return (int) items.stream()
                .filter(item -> LabWorkflowConstants.DetectionStatus.SUBMITTED.equals(item.getItemStatus())
                        || LabWorkflowConstants.DetectionStatus.APPROVED.equals(item.getItemStatus()))
                .count();
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
