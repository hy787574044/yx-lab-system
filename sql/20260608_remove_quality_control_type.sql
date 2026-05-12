-- 移除样品质控类型字段与对应字典。

SET @quality_control_type_column_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND COLUMN_NAME = 'quality_control_type'
);

SET @drop_quality_control_type_column_sql = IF(
    @quality_control_type_column_exists > 0,
    'ALTER TABLE lab_sample DROP COLUMN quality_control_type',
    'SELECT 1'
);

PREPARE drop_quality_control_type_column_stmt FROM @drop_quality_control_type_column_sql;
EXECUTE drop_quality_control_type_column_stmt;
DEALLOCATE PREPARE drop_quality_control_type_column_stmt;

UPDATE lab_dict
SET deleted = 1,
    status = 0,
    updated_time = NOW()
WHERE dict_code = 'quality_control_type';
