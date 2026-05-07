package com.yx.lab.modules.review.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@TableName("lab_review_record")
@EqualsAndHashCode(callSuper = true)
public class ReviewRecord extends BaseEntity {

    private Long detectionRecordId;

    private Long sampleId;

    private String sampleNo;

    private String sealNo;

    private Long reviewerId;

    private String reviewerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;

    private String reviewResult;

    private String rejectReason;

    private String reviewRemark;
}
