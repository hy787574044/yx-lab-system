ALTER TABLE `lab_detection_method`
    ADD COLUMN `parameter_id` BIGINT NULL COMMENT '绑定检测参数ID' AFTER `method_code`,
    ADD COLUMN `parameter_name` VARCHAR(100) NULL COMMENT '绑定检测参数名称' AFTER `parameter_id`;

CREATE INDEX `idx_detection_method_parameter_id` ON `lab_detection_method` (`parameter_id`);
