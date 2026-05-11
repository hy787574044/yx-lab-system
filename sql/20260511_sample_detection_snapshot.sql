ALTER TABLE lab_sample
    ADD COLUMN detection_type_id BIGINT NULL COMMENT '样品登录时选择的检测套餐ID' AFTER detection_items,
    ADD COLUMN detection_type_name VARCHAR(128) NULL COMMENT '样品登录时选择的检测套餐名称' AFTER detection_type_id,
    ADD COLUMN detection_config_snapshot TEXT NULL COMMENT '样品登录时确认的检测参数与方法快照JSON' AFTER detection_type_name;
