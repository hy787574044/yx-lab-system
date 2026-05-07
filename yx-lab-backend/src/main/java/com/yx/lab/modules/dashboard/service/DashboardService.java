package com.yx.lab.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.modules.dashboard.vo.DashboardOverviewVO;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final LabReportMapper labReportMapper;

    public DashboardOverviewVO overview() {
        DashboardOverviewVO vo = new DashboardOverviewVO();
        vo.setSampleTotal(labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()));
        vo.setPendingReviewTotal(detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, "SUBMITTED")));
        vo.setApprovedTotal(detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, "APPROVED")));
        vo.setPublishedReportTotal(labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getReportStatus, "PUBLISHED")));
        vo.setResultSummary(buildResultSummary());
        vo.setQuickActions(buildQuickActions());
        return vo;
    }

    private Map<String, Long> buildResultSummary() {
        Map<String, Long> resultSummary = new LinkedHashMap<>();
        resultSummary.put("正常", detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, "NORMAL")));
        resultSummary.put("异常", detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, "ABNORMAL")));
        return resultSummary;
    }

    private List<String> buildQuickActions() {
        return Arrays.asList("样品登录", "检测录入", "审核审批", "报告发布");
    }
}
