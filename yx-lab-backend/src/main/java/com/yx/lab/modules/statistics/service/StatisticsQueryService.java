package com.yx.lab.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.statistics.vo.StatisticsDimensionItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统计查询服务，提供统计页面需要的基础计数指标。
 */
@Service
@RequiredArgsConstructor
public class StatisticsQueryService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final ReviewRecordMapper reviewRecordMapper;

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
     * 统计正常结果数量。
     *
     * @return 正常结果数量
     */
    public long normalTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.NORMAL));
        return toLong(count);
    }

    /**
     * 统计异常结果数量。
     *
     * @return 异常结果数量
     */
    public long abnormalTotal() {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL));
        return toLong(count);
    }

    /**
     * 统计审查记录总数。
     *
     * @return 审查记录总数
     */
    public long reviewTotal() {
        Long count = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>());
        return toLong(count);
    }

    /**
     * 统计审查通过数量。
     *
     * @return 审查通过数量
     */
    public long approvedReviewTotal() {
        Long count = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.APPROVED));
        return toLong(count);
    }

    /**
     * 统计审查驳回数量。
     *
     * @return 审查驳回数量
     */
    public long rejectedReviewTotal() {
        Long count = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.REJECTED));
        return toLong(count);
    }

    public long sampleTotalFrom(LocalDateTime startTime) {
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .ge(startTime != null, LabSample::getCreatedTime, startTime));
        return toLong(count);
    }

    public long detectionTotalFrom(LocalDateTime startTime) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .ge(startTime != null, DetectionRecord::getCreatedTime, startTime));
        return toLong(count);
    }

    public long reviewTotalFrom(LocalDateTime startTime) {
        Long count = reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .ge(startTime != null, ReviewRecord::getCreatedTime, startTime));
        return toLong(count);
    }

    public long reportTotalFrom(LocalDateTime startTime) {
        Long count = labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .ge(startTime != null, LabReport::getCreatedTime, startTime));
        return toLong(count);
    }

    public List<StatisticsDimensionItemVO> sampleTypeDistribution() {
        return Arrays.asList(
                new StatisticsDimensionItemVO("水源水", sampleTypeTotal(LabWorkflowConstants.SampleType.SOURCE_WATER)),
                new StatisticsDimensionItemVO("滤前水", sampleTypeTotal(LabWorkflowConstants.SampleType.RAW)),
                new StatisticsDimensionItemVO("出厂水", sampleTypeTotal(LabWorkflowConstants.SampleType.FACTORY)),
                new StatisticsDimensionItemVO("末梢水", sampleTypeTotal(LabWorkflowConstants.SampleType.TERMINAL)));
    }

    public List<StatisticsDimensionItemVO> sampleStatusDistribution() {
        return Arrays.asList(
                new StatisticsDimensionItemVO("已登录", sampleStatusTotal(LabWorkflowConstants.SampleStatus.LOGGED)),
                new StatisticsDimensionItemVO("审核中", sampleStatusTotal(LabWorkflowConstants.SampleStatus.REVIEWING)),
                new StatisticsDimensionItemVO("待重检", sampleStatusTotal(LabWorkflowConstants.SampleStatus.RETEST)),
                new StatisticsDimensionItemVO("已完成", sampleStatusTotal(LabWorkflowConstants.SampleStatus.COMPLETED)));
    }

    public List<StatisticsDimensionItemVO> detectionStatusDistribution() {
        return Arrays.asList(
                new StatisticsDimensionItemVO("待分配", detectionStatusTotal(LabWorkflowConstants.DetectionStatus.WAIT_ASSIGN)),
                new StatisticsDimensionItemVO("待检测", detectionStatusTotal(LabWorkflowConstants.DetectionStatus.WAIT_DETECT)),
                new StatisticsDimensionItemVO("待审核", detectionStatusTotal(LabWorkflowConstants.DetectionStatus.SUBMITTED)),
                new StatisticsDimensionItemVO("审核通过", detectionStatusTotal(LabWorkflowConstants.DetectionStatus.APPROVED)),
                new StatisticsDimensionItemVO("审核驳回", detectionStatusTotal(LabWorkflowConstants.DetectionStatus.REJECTED)));
    }

    public List<StatisticsDimensionItemVO> detectionResultDistribution() {
        return Arrays.asList(
                new StatisticsDimensionItemVO("正常", normalTotal()),
                new StatisticsDimensionItemVO("异常", abnormalTotal()));
    }

    public List<StatisticsDimensionItemVO> reviewResultDistribution() {
        return Arrays.asList(
                new StatisticsDimensionItemVO("审核通过", approvedReviewTotal()),
                new StatisticsDimensionItemVO("审核驳回", rejectedReviewTotal()));
    }

    public List<StatisticsDimensionItemVO> reportStatusDistribution() {
        return Arrays.asList(
                new StatisticsDimensionItemVO("草稿", reportStatusTotal(LabWorkflowConstants.ReportStatus.DRAFT)),
                new StatisticsDimensionItemVO("已生成", reportStatusTotal(LabWorkflowConstants.ReportStatus.GENERATED)),
                new StatisticsDimensionItemVO("已发布", reportStatusTotal(LabWorkflowConstants.ReportStatus.PUBLISHED)));
    }

    public List<StatisticsDimensionItemVO> detectionTypeRanking() {
        QueryWrapper<DetectionRecord> wrapper = new QueryWrapper<>();
        wrapper.select("detection_type_name AS name", "COUNT(1) AS value")
                .eq("deleted", 0)
                .isNotNull("detection_type_name")
                .ne("detection_type_name", "")
                .groupBy("detection_type_name")
                .last("ORDER BY value DESC LIMIT 8");
        return toDimensionItems(detectionRecordMapper.selectMaps(wrapper));
    }

    private List<StatisticsDimensionItemVO> toDimensionItems(List<java.util.Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> new StatisticsDimensionItemVO(
                        String.valueOf(row.get("name")),
                        ((Number) row.get("value")).longValue()))
                .collect(Collectors.toList());
    }

    private long sampleTypeTotal(String sampleType) {
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getSampleType, sampleType));
        return toLong(count);
    }

    private long sampleStatusTotal(String sampleStatus) {
        Long count = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getSampleStatus, sampleStatus));
        return toLong(count);
    }

    private long detectionStatusTotal(String detectionStatus) {
        Long count = detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionStatus, detectionStatus));
        return toLong(count);
    }

    private long reportStatusTotal(String reportStatus) {
        Long count = labReportMapper.selectCount(new LambdaQueryWrapper<LabReport>()
                .eq(LabReport::getReportStatus, reportStatus));
        return toLong(count);
    }

    private long toLong(Long count) {
        return count == null ? 0L : count.longValue();
    }
}
