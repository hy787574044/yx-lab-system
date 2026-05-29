-- Add sampling completion fields and onsite/field detection result status support.

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'lab_sampling_task'
          AND COLUMN_NAME = 'sample_total_volume'
    ),
    'ALTER TABLE lab_sampling_task ADD COLUMN sample_total_volume VARCHAR(64) NULL COMMENT ''采样总容量'' AFTER temperature',
    'SELECT ''lab_sampling_task.sample_total_volume already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'lab_sampling_task'
          AND COLUMN_NAME = 'sample_bottle_count'
    ),
    'ALTER TABLE lab_sampling_task ADD COLUMN sample_bottle_count VARCHAR(32) NULL COMMENT ''采样瓶数'' AFTER sample_total_volume',
    'SELECT ''lab_sampling_task.sample_bottle_count already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'lab_sample'
          AND COLUMN_NAME = 'sample_total_volume'
    ),
    'ALTER TABLE lab_sample ADD COLUMN sample_total_volume VARCHAR(64) NULL COMMENT ''采样总容量'' AFTER sampling_time',
    'SELECT ''lab_sample.sample_total_volume already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'lab_sample'
          AND COLUMN_NAME = 'sample_bottle_count'
    ),
    'ALTER TABLE lab_sample ADD COLUMN sample_bottle_count VARCHAR(32) NULL COMMENT ''采样瓶数'' AFTER sample_total_volume',
    'SELECT ''lab_sample.sample_bottle_count already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
