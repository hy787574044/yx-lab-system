-- 采样计划检测套餐字段补丁。
-- 适用于已经执行过旧版 20260528_sampling_plan_detection_type.sql，
-- 但缺少 detection_config_snapshot 字段或部分字段的数据库。

SET @schema_name = DATABASE();

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE lab_sampling_plan ADD COLUMN detection_type_id BIGINT NULL COMMENT ''检测套餐ID'' AFTER sample_type',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'lab_sampling_plan'
        AND COLUMN_NAME = 'detection_type_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE lab_sampling_plan ADD COLUMN detection_type_name VARCHAR(128) NULL COMMENT ''检测套餐名称'' AFTER detection_type_id',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'lab_sampling_plan'
        AND COLUMN_NAME = 'detection_type_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE lab_sampling_plan ADD COLUMN detection_config_snapshot TEXT NULL COMMENT ''检测配置快照'' AFTER detection_type_name',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'lab_sampling_plan'
        AND COLUMN_NAME = 'detection_config_snapshot'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN detection_type_id BIGINT NULL COMMENT ''检测套餐ID'' AFTER detection_items',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'lab_sampling_task'
        AND COLUMN_NAME = 'detection_type_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN detection_type_name VARCHAR(128) NULL COMMENT ''检测套餐名称'' AFTER detection_type_id',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'lab_sampling_task'
        AND COLUMN_NAME = 'detection_type_name'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN detection_config_snapshot TEXT NULL COMMENT ''检测配置快照'' AFTER detection_type_name',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
        AND TABLE_NAME = 'lab_sampling_task'
        AND COLUMN_NAME = 'detection_config_snapshot'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE lab_sampling_task task
JOIN lab_detection_type type_config
    ON type_config.type_name = task.detection_items
    AND type_config.deleted = 0
SET task.detection_type_id = type_config.id,
    task.detection_type_name = type_config.type_name
WHERE task.detection_type_id IS NULL
    AND task.detection_items IS NOT NULL
    AND task.detection_items <> '';
