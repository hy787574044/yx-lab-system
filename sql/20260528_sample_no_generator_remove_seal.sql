CREATE TABLE IF NOT EXISTS lab_sample_no_sequence (
    sequence_date VARCHAR(8) PRIMARY KEY,
    current_value BIGINT NOT NULL DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'sample_no'
);
SET @sql := IF(@column_exists = 0,
    'ALTER TABLE lab_sampling_task ADD COLUMN sample_no VARCHAR(64) NULL COMMENT ''样品编号'' AFTER task_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND INDEX_NAME = 'idx_lab_sampling_task_sample_no'
);
SET @sql := IF(@index_exists = 0,
    'CREATE INDEX idx_lab_sampling_task_sample_no ON lab_sampling_task(sample_no)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE lab_sampling_task task
JOIN lab_sample sample ON sample.task_id = task.id
SET task.sample_no = sample.sample_no
WHERE (task.sample_no IS NULL OR task.sample_no = '')
  AND sample.sample_no IS NOT NULL
  AND sample.sample_no <> '';

INSERT INTO lab_sample_no_sequence(sequence_date, current_value)
SELECT sequence_date, MAX(current_value) AS current_value
FROM (
    SELECT SUBSTRING(sample_no, 3, 8) AS sequence_date,
           CAST(SUBSTRING(sample_no, 11) AS UNSIGNED) AS current_value
    FROM lab_sample
    WHERE sample_no REGEXP '^YX[0-9]{12,}$'
    UNION ALL
    SELECT SUBSTRING(sample_no, 3, 8) AS sequence_date,
           CAST(SUBSTRING(sample_no, 11) AS UNSIGNED) AS current_value
    FROM lab_sampling_task
    WHERE sample_no REGEXP '^YX[0-9]{12,}$'
) existing_sample_no
GROUP BY sequence_date
ON DUPLICATE KEY UPDATE
    current_value = GREATEST(lab_sample_no_sequence.current_value, VALUES(current_value)),
    updated_time = NOW();

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND INDEX_NAME = 'idx_lab_sampling_task_seal_no'
);
SET @sql := IF(@index_exists > 0,
    'ALTER TABLE lab_sampling_task DROP INDEX idx_lab_sampling_task_seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND INDEX_NAME = 'idx_lab_sample_seal_no'
);
SET @sql := IF(@index_exists > 0,
    'ALTER TABLE lab_sample DROP INDEX idx_lab_sample_seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'seal_no'
);
SET @sql := IF(@column_exists > 0,
    'ALTER TABLE lab_sampling_task DROP COLUMN seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND COLUMN_NAME = 'seal_no'
);
SET @sql := IF(@column_exists > 0,
    'ALTER TABLE lab_sample DROP COLUMN seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND COLUMN_NAME = 'seal_time'
);
SET @sql := IF(@column_exists > 0,
    'ALTER TABLE lab_sample DROP COLUMN seal_time',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_detection_record'
      AND COLUMN_NAME = 'seal_no'
);
SET @sql := IF(@column_exists > 0,
    'ALTER TABLE lab_detection_record DROP COLUMN seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_review_record'
      AND COLUMN_NAME = 'seal_no'
);
SET @sql := IF(@column_exists > 0,
    'ALTER TABLE lab_review_record DROP COLUMN seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_report'
      AND COLUMN_NAME = 'seal_no'
);
SET @sql := IF(@column_exists > 0,
    'ALTER TABLE lab_report DROP COLUMN seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_report_push_record'
      AND COLUMN_NAME = 'seal_no'
);
SET @sql := IF(@column_exists > 0,
    'ALTER TABLE lab_report_push_record DROP COLUMN seal_no',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
