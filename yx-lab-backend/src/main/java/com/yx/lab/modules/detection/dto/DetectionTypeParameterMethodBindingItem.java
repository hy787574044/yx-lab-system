package com.yx.lab.modules.detection.dto;

import lombok.Data;

import java.util.List;

/**
 * 检测套餐内单个检测参数的检测方法绑定项。
 */
@Data
public class DetectionTypeParameterMethodBindingItem {

    /**
     * 检测参数主键。
     */
    private Long parameterId;

    /**
     * 检测参数名称，后端保存时会按实时数据回填。
     */
    private String parameterName;

    /**
     * 当前参数下选中的检测方法主键集合。
     */
    private List<Long> methodIds;

    /**
     * 当前参数下选中的检测方法名称集合，后端保存时会按实时数据回填。
     */
    private List<String> methodNames;
}
