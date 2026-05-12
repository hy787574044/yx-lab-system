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

/**
 * 首页看板统计查询服务，负责聚合各流程节点的核心计数。
 */
@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final LabReportMapper labReportMapper;

    /**
     * 统计样品总数。
     *
     * @return 样品总数
     */
    public long sampleTotal() {
        return labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>());
    }

    /**
     * 统计待审查的检测主流程数量。
     *
     * @return 待审查数量
     */
    public long pendingReviewTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED));
    }

    /**
     * 统计已审核通过的检测主流程数量。
     *
     * @return 已通过数量
     */
    public long approvedDetectionTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.APPROVED));
    }

    /**
     * 统计已发布报告数量。
     *
     * @return 已发布报告数量
     */
    public long publishedReportTotal() {
        return labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getReportStatus, LabWorkflowConstants.ReportStatus.PUBLISHED));
    }

    /**
     * 统计检测结果为正常的主流程数量。
     *
     * @return 正常结果数量
     */
    public long normalResultTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.NORMAL));
    }

    /**
     * 统计检测结果为异常的主流程数量。
     *
     * @return 异常结果数量
     */
    public long abnormalResultTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL));
    }
}
