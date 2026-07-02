package com.yx.lab.modules.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import com.yx.lab.modules.statistics.vo.StatisticsDimensionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页看板统计查询服务，负责聚合各流程节点的核心计数。
 */
@Service
@RequiredArgsConstructor
public class DashboardQueryService {

    private final LabSampleMapper labSampleMapper;

    private final SamplingTaskMapper samplingTaskMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final LabReportMapper labReportMapper;

    /**
     * 统计样品总数。
     *
     * @return 样品总数
     */
    public long sampleTotal() {
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>());
        return toLong(count);
    }

    /**
     * 统计待审查的检测主流程数量。
     *
     * @return 待审查数量
     */
    public long pendingReviewTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED));
        return toLong(count);
    }

    /**
     * 统计已审核通过的检测主流程数量。
     *
     * @return 已通过数量
     */
    public long approvedDetectionTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.APPROVED));
        return toLong(count);
    }

    /**
     * 统计已发布报告数量。
     *
     * @return 已发布报告数量
     */
    public long publishedReportTotal() {
        Long count = labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getReportStatus, LabWorkflowConstants.ReportStatus.PUBLISHED));
        return toLong(count);
    }

    /**
     * 统计检测结果为正常的主流程数量。
     *
     * @return 正常结果数量
     */
    public long normalResultTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.NORMAL));
        return toLong(count);
    }

    /**
     * 统计检测结果为异常的主流程数量。
     *
     * @return 异常结果数量
     */
    public long abnormalResultTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL));
        return toLong(count);
    }

    /**
     * 按创建时间统计样品数量。
     *
     * @param startTime 起始时间
     * @return 样品数量
     */
    public long sampleTotalFrom(LocalDateTime startTime) {
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .ge(startTime != null, LabSample::getCreatedTime, startTime));
        return toLong(count);
    }

    /**
     * 按创建时间统计报告数量。
     *
     * @param startTime 起始时间
     * @return 报告数量
     */
    public long reportTotalFrom(LocalDateTime startTime) {
        Long count = labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .ge(startTime != null, LabReport::getCreatedTime, startTime));
        return toLong(count);
    }

    /**
     * 按检测时间统计已完成检测数量。
     *
     * @param startTime 起始时间
     * @return 检测数量
     */
    public long completedDetectionTotalFrom(LocalDateTime startTime) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .isNotNull(DetectionRecord::getDetectionResult)
                .ge(startTime != null, DetectionRecord::getDetectionTime, startTime));
        return toLong(count);
    }

    /**
     * 统计实时进行中的样品数量。
     *
     * @return 进行中样品数量
     */
    public long runningSampleTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .in(DetectionRecord::getDetectionStatus,
                        LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN,
                        LabWorkflowConstants.DetectionStatus.WAIT_DETECT,
                        LabWorkflowConstants.DetectionStatus.SUBMITTED,
                        LabWorkflowConstants.DetectionStatus.REJECTED));
        return toLong(count);
    }

    /**
     * 统计实时超标预警数量。
     *
     * @return 超标预警数量
     */
    public long abnormalWarningTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL)
                .ne(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.APPROVED));
        return toLong(count);
    }

    /**
     * 按样品状态统计数量。
     *
     * @param sampleStatus 样品状态
     * @return 数量
     */
    public long sampleStatusTotal(String sampleStatus) {
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getSampleStatus, sampleStatus));
        return toLong(count);
    }

    /**
     * 按采样任务状态统计数量。
     *
     * @param taskStatuses 采样任务状态
     * @return 数量
     */
    public long samplingTaskStatusTotal(String... taskStatuses) {
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<>();
        if (taskStatuses != null && taskStatuses.length > 0) {
            wrapper.in(SamplingTask::getTaskStatus, Arrays.asList(taskStatuses));
        }
        Long count = samplingTaskMapper.selectCount(wrapper);
        return count == null ? 0L : count.longValue();
    }

    /**
     * 统计已完成采样但尚未样品登录的任务数量。
     *
     * @return 数量
     */
    public long unregisteredCompletedSamplingTaskTotal() {
        Long count = samplingTaskMapper.selectCount(new LambdaQueryWrapper<SamplingTask>()
                .eq(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .isNull(SamplingTask::getSampleId)
                .and(wrapper -> wrapper
                        .isNull(SamplingTask::getSampleRegisterStatus)
                        .or()
                        .ne(SamplingTask::getSampleRegisterStatus, LabWorkflowConstants.SampleRegisterStatus.REGISTERED)));
        return count == null ? 0L : count.longValue();
    }

    /**
     * 按检测状态统计数量。
     *
     * @param detectionStatus 检测状态
     * @return 数量
     */
    public long detectionStatusTotal(String detectionStatus) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, detectionStatus));
        return toLong(count);
    }

    /**
     * 按报告状态统计数量。
     *
     * @param reportStatus 报告状态
     * @return 数量
     */
    public long reportStatusTotal(String reportStatus) {
        Long count = labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getReportStatus, reportStatus));
        return toLong(count);
    }

    /**
     * 按时间区间统计检测结果数量。
     *
     * @param result 检测结果
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @return 数量
     */
    public long detectionResultTotalBetween(String result, LocalDateTime startTime, LocalDateTime endTime) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, result)
                .ge(startTime != null, DetectionRecord::getDetectionTime, startTime)
                .lt(endTime != null, DetectionRecord::getDetectionTime, endTime));
        return toLong(count);
    }

    /**
     * 统计超过指定时间仍未审核的检测记录。
     *
     * @param deadline 超时截止时间
     * @return 超时数量
     */
    public long pendingReviewTimeoutTotal(LocalDateTime deadline) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, LabWorkflowConstants.DetectionStatus.SUBMITTED)
                .le(deadline != null, DetectionRecord::getUpdatedTime, deadline));
        return toLong(count);
    }

    /**
     * 统计超标后超过指定时间仍未处理的检测记录。
     *
     * @param deadline 超时截止时间
     * @return 超时数量
     */
    public long abnormalUnhandledTotal(LocalDateTime deadline) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL)
                .in(DetectionRecord::getDetectionStatus,
                        LabWorkflowConstants.DetectionStatus.SUBMITTED,
                        LabWorkflowConstants.DetectionStatus.REJECTED)
                .le(deadline != null, DetectionRecord::getUpdatedTime, deadline));
        return toLong(count);
    }

    /**
     * 统计检测员本月完成的检测项排行。
     *
     * @param startTime 起始时间
     * @return 检测员工作量排行
     */
    public List<StatisticsDimensionItemVO> detectorWorkloadRanking(LocalDateTime startTime) {
        QueryWrapper<DetectionItem> wrapper = new QueryWrapper<>();
        wrapper.select("detector_name AS name", "COUNT(1) AS value")
                .eq("deleted", 0)
                .isNotNull("detector_name")
                .ne("detector_name", "")
                .ge(startTime != null, "updated_time", startTime)
                .in("item_status",
                        LabWorkflowConstants.DetectionStatus.SUBMITTED,
                        LabWorkflowConstants.DetectionStatus.APPROVED)
                .groupBy("detector_name")
                .last("ORDER BY value DESC LIMIT 8");
        return toDimensionItems(detectionItemMapper.selectMaps(wrapper));
    }

    private List<StatisticsDimensionItemVO> toDimensionItems(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> new StatisticsDimensionItemVO(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("value")).longValue()))
                .collect(Collectors.toList());
    }

    private long toLong(Long count) {
        return count == null ? 0L : count.longValue();
    }
}
