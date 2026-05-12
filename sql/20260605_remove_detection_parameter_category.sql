SET @ddl = (
    SELECT IF(COUNT(*) > 0,
        'ALTER TABLE lab_detection_parameter DROP COLUMN parameter_category',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_detection_parameter'
      AND COLUMN_NAME = 'parameter_category'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DELETE FROM lab_dict
WHERE dict_code = 'detection_parameter_category';
