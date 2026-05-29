SET @add_sampling_task_sampling_basis_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN sampling_basis VARCHAR(1000) NULL COMMENT ''采样依据'' AFTER detection_config_snapshot',
        'SELECT ''lab_sampling_task.sampling_basis already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'sampling_basis'
);
PREPARE add_sampling_task_sampling_basis_stmt FROM @add_sampling_task_sampling_basis_sql;
EXECUTE add_sampling_task_sampling_basis_stmt;
DEALLOCATE PREPARE add_sampling_task_sampling_basis_stmt;

UPDATE lab_sampling_task task
JOIN lab_sampling_plan plan
    ON plan.id = task.plan_id
    AND plan.deleted = 0
SET task.sampling_basis = plan.sampling_basis
WHERE (task.sampling_basis IS NULL OR task.sampling_basis = '')
  AND plan.sampling_basis IS NOT NULL
  AND plan.sampling_basis <> '';
