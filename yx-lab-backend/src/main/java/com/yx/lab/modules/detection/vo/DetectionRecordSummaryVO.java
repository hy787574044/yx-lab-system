package com.yx.lab.modules.detection.vo;

import lombok.Data;

@Data
public class DetectionRecordSummaryVO {

    private long total;

    private long waitAssignCount;

    private long waitDetectCount;

    private long pendingReviewCount;

    private long approvedCount;

    private long rejectedCount;
}
