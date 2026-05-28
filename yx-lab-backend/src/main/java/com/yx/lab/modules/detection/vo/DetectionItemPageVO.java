package com.yx.lab.modules.detection.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DetectionItemPageVO {

    private Long id;

    private Long recordId;

    private Long sampleId;

    private String sampleNo;

    private Long detectionTypeId;

    private String detectionTypeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectionTime;

    private Long parameterId;

    private String parameterName;

    private BigDecimal standardMin;

    private BigDecimal standardMax;

    private BigDecimal resultValue;

    private String unit;

    private String referenceStandard;

    private Long methodId;

    private String methodName;

    private String methodBasis;

    private Long detectorId;

    private String detectorName;

    private String itemStatus;

    private String itemStatusDesc;

    private Integer exceedFlag;

    private String abnormalRemark;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
