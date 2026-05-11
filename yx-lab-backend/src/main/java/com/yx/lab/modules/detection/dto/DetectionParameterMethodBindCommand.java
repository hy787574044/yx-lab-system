package com.yx.lab.modules.detection.dto;

import lombok.Data;

import java.util.List;

@Data
public class DetectionParameterMethodBindCommand {

    private List<Long> methodIds;
}
