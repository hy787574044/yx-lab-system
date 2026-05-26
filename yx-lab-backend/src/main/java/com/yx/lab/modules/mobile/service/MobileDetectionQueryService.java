package com.yx.lab.modules.mobile.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.dto.DetectionRecordQuery;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.mobile.vo.MobileDetectionHistoryVO;
import com.yx.lab.modules.mobile.vo.MobileDetectionTodoVO;
import com.yx.lab.modules.sample.dto.LabSampleQuery;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 移动端检测查询服务。
 */
@Service
@RequiredArgsConstructor
public class MobileDetectionQueryService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    /**
     * 分页查询移动端检测待办。
     *
     * @param query 样品分页查询条件
     * @return 检测待办分页结果
     */
    public PageResult<MobileDetectionTodoVO> detectionTodo(LabSampleQuery query) {
        Page<LabSample> page = labSampleMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabSample>()
                        .and(StrUtil.isNotBlank(StrUtil.trim(query.getKeyword())), wrapper -> wrapper
                                .like(LabSample::getSampleNo, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabSample::getSealNo, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabSample::getPointName, StrUtil.trim(query.getKeyword())))
                        .eq(StrUtil.isNotBlank(query.getSampleStatus()), LabSample::getSampleStatus, query.getSampleStatus())
                        .eq(StrUtil.isNotBlank(query.getSampleType()), LabSample::getSampleType, query.getSampleType())
                        .in(LabSample::getSampleStatus, LabWorkflowConstants.DETECTABLE_SAMPLE_STATUSES)
                        .orderByDesc(LabSample::getSealTime)
                        .orderByDesc(LabSample::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(this::toDetectionTodoVO)
                .collect(Collectors.toList()));
    }

    /**
     * 分页查询当前检测员的移动端检测历史。
     *
     * @param query 检测流程分页查询条件
     * @return 检测历史分页结果
     */
    public PageResult<MobileDetectionHistoryVO> detectionHistory(DetectionRecordQuery query) {
        CurrentUser currentUser = requireCurrentUser();
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        Page<DetectionRecord> page = detectionRecordMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionRecord>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(DetectionRecord::getSampleNo, keyword)
                                .or()
                                .like(DetectionRecord::getSealNo, keyword)
                                .or()
                                .like(DetectionRecord::getDetectionTypeName, keyword))
                        .eq(DetectionRecord::getDetectorId, currentUser.getUserId())
                        .eq(StrUtil.isNotBlank(query.getDetectionStatus()),
                                DetectionRecord::getDetectionStatus,
                                query.getDetectionStatus())
                        .orderByDesc(DetectionRecord::getDetectionTime)
                        .orderByDesc(DetectionRecord::getCreatedTime));
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(this::toDetectionHistoryVO)
                .collect(Collectors.toList()));
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录。");
        }
        return currentUser;
    }

    private MobileDetectionTodoVO toDetectionTodoVO(LabSample sample) {
        MobileDetectionTodoVO vo = new MobileDetectionTodoVO();
        vo.setSampleId(sample.getId());
        vo.setSampleNo(sample.getSampleNo());
        vo.setSealNo(sample.getSealNo());
        vo.setPointName(sample.getPointName());
        vo.setSampleType(sample.getSampleType());
        vo.setDetectionItems(sample.getDetectionItems());
        vo.setSampleStatus(sample.getSampleStatus());
        vo.setResultSummary(sample.getResultSummary());
        vo.setSamplerName(sample.getSamplerName());
        vo.setSamplingTime(sample.getSamplingTime());
        vo.setTraceLog(sample.getTraceLog());
        return vo;
    }

    private MobileDetectionHistoryVO toDetectionHistoryVO(DetectionRecord record) {
        MobileDetectionHistoryVO vo = new MobileDetectionHistoryVO();
        vo.setId(record.getId());
        vo.setSampleId(record.getSampleId());
        vo.setSampleNo(record.getSampleNo());
        vo.setSealNo(record.getSealNo());
        vo.setDetectionTypeId(record.getDetectionTypeId());
        vo.setDetectionTypeName(record.getDetectionTypeName());
        vo.setDetectionResult(record.getDetectionResult());
        vo.setDetectionStatus(record.getDetectionStatus());
        vo.setAbnormalRemark(record.getAbnormalRemark());
        vo.setDetectionTime(record.getDetectionTime());
        return vo;
    }
}