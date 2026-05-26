package com.yx.lab.modules.mobile.service;

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
import com.yx.lab.modules.detection.dto.DetectionRecordQuery;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.mobile.vo.MobileReviewHistoryVO;
import com.yx.lab.modules.mobile.vo.MobileReviewTodoVO;
import com.yx.lab.modules.review.dto.ReviewQuery;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 移动端审核查询服务。
 */
@Service
@RequiredArgsConstructor
public class MobileReviewQueryService {

    private final DetectionRecordMapper detectionRecordMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    private final DataScopeHelper dataScopeHelper;

    /**
     * 分页查询当前审核人的审核历史。
     *
     * @param query 审核记录分页查询条件
     * @return 审核历史分页结果
     */
    public PageResult<MobileReviewHistoryVO> reviewHistory(ReviewQuery query) {
        CurrentUser currentUser = requireCurrentUser();
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        Page<ReviewRecord> page = reviewRecordMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<ReviewRecord>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(ReviewRecord::getSampleNo, keyword)
                                .or()
                                .like(ReviewRecord::getSealNo, keyword))
                        .eq(StrUtil.isNotBlank(query.getReviewResult()),
                                ReviewRecord::getReviewResult,
                                query.getReviewResult())
                        .eq(ReviewRecord::getReviewerId, currentUser.getUserId())
                        .orderByDesc(ReviewRecord::getReviewTime));
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(this::toReviewHistoryVO)
                .collect(Collectors.toList()));
    }

    /**
     * 分页查询移动端审核待办。
     *
     * @param query 检测流程分页查询条件
     * @return 审核待办分页结果
     */
    public PageResult<MobileReviewTodoVO> reviewTodo(DetectionRecordQuery query) {
        CurrentUser currentUser = requireCurrentUser();
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        
        // 获取当前审查员待审核的检测记录ID列表
        List<Long> pendingRecordIds = reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
                        .eq(ReviewRecord::getReviewerId, currentUser.getUserId())
                        .isNull(ReviewRecord::getReviewResult))
                .stream()
                .map(ReviewRecord::getDetectionRecordId)
                .collect(Collectors.toList());
        
        if (pendingRecordIds.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }
        
        Page<DetectionRecord> page = detectionRecordMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionRecord>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(DetectionRecord::getSampleNo, keyword)
                                .or()
                                .like(DetectionRecord::getSealNo, keyword)
                                .or()
                                .like(DetectionRecord::getDetectionTypeName, keyword))
                        .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED)
                        // 审查员只能查看分配给自己的审核任务
                        .in(DetectionRecord::getId, pendingRecordIds)
                        .orderByDesc(DetectionRecord::getDetectionTime)
                        .orderByDesc(DetectionRecord::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(this::toReviewTodoVO)
                .collect(Collectors.toList()));
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录。");
        }
        return currentUser;
    }

    private MobileReviewHistoryVO toReviewHistoryVO(ReviewRecord record) {
        MobileReviewHistoryVO vo = new MobileReviewHistoryVO();
        vo.setId(record.getId());
        vo.setDetectionRecordId(record.getDetectionRecordId());
        vo.setSampleId(record.getSampleId());
        vo.setSampleNo(record.getSampleNo());
        vo.setReviewerId(record.getReviewerId());
        vo.setReviewerName(record.getReviewerName());
        vo.setReviewTime(record.getReviewTime());
        vo.setReviewResult(record.getReviewResult());
        vo.setReviewResultDesc(LabWorkflowConstants.getReviewResultLabel(record.getReviewResult()));
        vo.setRejectReason(record.getRejectReason());
        vo.setReviewRemark(record.getReviewRemark());
        return vo;
    }

    private MobileReviewTodoVO toReviewTodoVO(DetectionRecord record) {
        MobileReviewTodoVO vo = new MobileReviewTodoVO();
        vo.setId(record.getId());
        vo.setSampleId(record.getSampleId());
        vo.setSampleNo(record.getSampleNo());
        vo.setDetectionTypeId(record.getDetectionTypeId());
        vo.setDetectionTypeName(record.getDetectionTypeName());
        vo.setDetectionTime(record.getDetectionTime());
        vo.setDetectorId(record.getDetectorId());
        vo.setDetectorName(record.getDetectorName());
        vo.setDetectionResult(record.getDetectionResult());
        vo.setDetectionResultDesc(LabWorkflowConstants.getDetectionResultLabel(record.getDetectionResult()));
        vo.setAbnormalRemark(record.getAbnormalRemark());
        vo.setDetectionStatus(record.getDetectionStatus());
        vo.setDetectionStatusDesc(LabWorkflowConstants.getDetectionStatusLabel(record.getDetectionStatus()));
        return vo;
    }
}