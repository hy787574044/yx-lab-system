package com.yx.lab.modules.statistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 统计查询服务，提供统计页面需要的基础计数指标。
 */
@Service
@RequiredArgsConstructor
public class StatisticsQueryService {

    private final LabSampleMapper labSampleMapper;

    private final DetectionRecordMapper detectionRecordMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    /**
     * 统计样品总数。
     *
     * @return 样品总数
     */
    public long sampleTotal() {
        return labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>());
    }

    /**
     * 统计正常结果数量。
     *
     * @return 正常结果数量
     */
    public long normalTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.NORMAL));
    }

    /**
     * 统计异常结果数量。
     *
     * @return 异常结果数量
     */
    public long abnormalTotal() {
        return detectionRecordMapper.selectCount(new LambdaQueryWrapper<DetectionRecord>()
                .eq(DetectionRecord::getDetectionResult, LabWorkflowConstants.DetectionResult.ABNORMAL));
    }

    /**
     * 统计审查记录总数。
     *
     * @return 审查记录总数
     */
    public long reviewTotal() {
        return reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>());
    }

    /**
     * 统计审查通过数量。
     *
     * @return 审查通过数量
     */
    public long approvedReviewTotal() {
        return reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.APPROVED));
    }

    /**
     * 统计审查驳回数量。
     *
     * @return 审查驳回数量
     */
    public long rejectedReviewTotal() {
        return reviewRecordMapper.selectCount(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getReviewResult, LabWorkflowConstants.ReviewResult.REJECTED));
    }
}
