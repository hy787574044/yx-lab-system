SET @source_system_flag_exists = (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND COLUMN_NAME = 'source_system_flag'
);

SET @drop_source_system_flag_sql = IF(
    @source_system_flag_exists > 0,
    'ALTER TABLE lab_sample DROP COLUMN source_system_flag',
    'SELECT 1'
);

PREPARE drop_source_system_flag_stmt FROM @drop_source_system_flag_sql;
EXECUTE drop_source_system_flag_stmt;
DEALLOCATE PREPARE drop_source_system_flag_stmt;
