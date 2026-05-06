package com.yx.lab.modules.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final LabReportMapper labReportMapper;

    @GetMapping("/overview")
    public ApiResponse<DashboardVO> overview() {
        DashboardVO vo = new DashboardVO();
        vo.setSampleTotal(labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()));
        vo.setPendingReviewTotal(detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, "SUBMITTED")));
        vo.setApprovedTotal(detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, "APPROVED")));
        vo.setPublishedReportTotal(labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getReportStatus, "PUBLISHED")));

        Map<String, Long> resultSummary = new LinkedHashMap<>();
        resultSummary.put("正常", detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, "NORMAL")));
        resultSummary.put("异常", detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, "ABNORMAL")));
        vo.setResultSummary(resultSummary);

        List<String> quickActions = Arrays.asList("样品登录", "检测录入", "审核审批", "报告发布");
        vo.setQuickActions(quickActions);
        return ApiResponse.success(vo);
    }

    @Data
    public static class DashboardVO {
        private Long sampleTotal;
        private Long pendingReviewTotal;
        private Long approvedTotal;
        private Long publishedReportTotal;
        private Map<String, Long> resultSummary;
        private List<String> quickActions;
    }
}
