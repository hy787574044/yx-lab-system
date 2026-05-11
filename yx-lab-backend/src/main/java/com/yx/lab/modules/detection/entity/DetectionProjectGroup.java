package com.yx.lab.modules.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yx.lab.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 检测项目组实体。
 */
@Data
@TableName("lab_detection_project_group")
@EqualsAndHashCode(callSuper = true)
public class DetectionProjectGroup extends BaseEntity {

    private String groupName;

    private Integer enabled;

    private String remark;
}
