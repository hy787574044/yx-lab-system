package com.yx.lab.modules.mobile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.mobile.vo.MobileDetectionHistoryVO;
import com.yx.lab.modules.mobile.vo.MobileDetectionTodoVO;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MobileDetectionQueryService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    public List<MobileDetectionTodoVO> detectionTodo() {
        return labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                        .in(LabSample::getSampleStatus, LabWorkflowConstants.DETECTABLE_SAMPLE_STATUSES)
                        .orderByDesc(LabSample::getSealTime)
                        .orderByDesc(LabSample::getCreatedTime))
                .stream()
                .map(this::toDetectionTodoVO)
                .collect(Collectors.toList());
    }

    public List<MobileDetectionHistoryVO> detectionHistory() {
        CurrentUser currentUser = requireCurrentUser();
        return detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                        .eq(DetectionRecord::getDetectorId, currentUser.getUserId())
                        .orderByDesc(DetectionRecord::getDetectionTime))
                .stream()
                .map(this::toDetectionHistoryVO)
                .collect(Collectors.toList());
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录");
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
