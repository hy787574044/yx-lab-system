ALTER TABLE lab_sampling_plan
    ADD COLUMN detection_type_id BIGINT NULL COMMENT '检测套餐ID' AFTER sample_type,
    ADD COLUMN detection_type_name VARCHAR(128) NULL COMMENT '检测套餐名称' AFTER detection_type_id,
    ADD COLUMN detection_config_snapshot TEXT NULL COMMENT '检测配置快照' AFTER detection_type_name;

ALTER TABLE lab_sampling_task
    ADD COLUMN detection_type_id BIGINT NULL COMMENT '检测套餐ID' AFTER detection_items,
    ADD COLUMN detection_type_name VARCHAR(128) NULL COMMENT '检测套餐名称' AFTER detection_type_id,
    ADD COLUMN detection_config_snapshot TEXT NULL COMMENT '检测配置快照' AFTER detection_type_name;

UPDATE lab_sampling_task task
JOIN lab_detection_type type_config
    ON type_config.type_name = task.detection_items
    AND type_config.deleted = 0
SET task.detection_type_id = type_config.id,
    task.detection_type_name = type_config.type_name
WHERE task.detection_type_id IS NULL
    AND task.detection_items IS NOT NULL
    AND task.detection_items <> '';
