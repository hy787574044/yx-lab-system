package com.yx.lab.modules.detection.vo;

import com.yx.lab.modules.detection.entity.DetectionMethodInstrumentModelBinding;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DetectionMethodInstrumentModelBindingVO {

    private Long id;

    private String methodName;

    private String methodCode;

    private Long parameterId;

    private String parameterName;

    private String standardCode;

    private String sampleVolume;

    private String methodBasis;

    private String applyScope;

    private Integer enabled;

    private String remark;

    private LocalDateTime updatedTime;

    private Integer instrumentModelCount;

    private String instrumentModelNames;

    private String instrumentDisplayNames;

    private List<DetectionMethodInstrumentModelBinding> instrumentModelBindings = new ArrayList<>();
}
