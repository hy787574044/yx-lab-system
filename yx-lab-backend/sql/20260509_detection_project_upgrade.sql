ALTER TABLE `lab_detection_type`
    ADD COLUMN `group_id` BIGINT NULL COMMENT '检测项目组ID' AFTER `type_name`,
    ADD COLUMN `group_name` VARCHAR(100) NULL COMMENT '检测项目组名称' AFTER `group_id`,
    ADD COLUMN `detector_id` BIGINT NULL COMMENT '绑定检测员ID' AFTER `group_name`,
    ADD COLUMN `detector_name` VARCHAR(100) NULL COMMENT '绑定检测员姓名' AFTER `detector_id`;

CREATE TABLE `lab_detection_project_group` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `group_name` VARCHAR(100) NOT NULL COMMENT '项目组名称',
    `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：1启用 0停用',
    `remark` VARCHAR(500) NULL COMMENT '备注',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    `created_by` BIGINT NULL COMMENT '创建人ID',
    `created_name` VARCHAR(100) NULL COMMENT '创建人姓名',
    `created_time` DATETIME NULL COMMENT '创建时间',
    `updated_by` BIGINT NULL COMMENT '更新人ID',
    `updated_name` VARCHAR(100) NULL COMMENT '更新人姓名',
    `updated_time` DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_detection_project_group_name` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测项目组';

CREATE INDEX `idx_detection_type_group_id` ON `lab_detection_type` (`group_id`);
CREATE INDEX `idx_detection_type_detector_id` ON `lab_detection_type` (`detector_id`);
