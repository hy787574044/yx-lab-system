package com.yx.lab.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final LabReportMapper labReportMapper;

    public long sampleTotal() {
        return labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>());
    }

    public long pendingReviewTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED));
    }

    public long approvedDetectionTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.APPROVED));
    }

    public long publishedReportTotal() {
        return labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getReportStatus, LabWorkflowConstants.ReportStatus.PUBLISHED));
    }

    public long normalResultTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.NORMAL));
    }

    public long abnormalResultTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL));
    }
}
