package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("lab_detection_type")
@EqualsAndHashCode(callSuper = true)
public class DetectionType extends BaseEntity {

    private String typeName;

    private Long groupId;

    private String groupName;

    private Long detectorId;

    private String detectorName;

    private String parameterIds;

    private String parameterNames;

    /**
     * 检测套餐内“检测参数 -> 检测方法”绑定关系，JSON 数组文本。
     */
    private String parameterMethodBindings;

    /**
     * 前端列表展示用的参数-方法摘要，不落库。
     */
    @TableField(exist = false)
    private String parameterMethodNames;

    private Integer enabled;

    private String remark;
}
