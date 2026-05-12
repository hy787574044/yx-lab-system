-- 移除样品登录阶段选择的发布流程字段。

SET @sample_publish_flow_index_exists = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND INDEX_NAME = 'idx_lab_sample_publish_flow_id'
);

SET @drop_sample_publish_flow_index_sql = IF(
    @sample_publish_flow_index_exists > 0,
    'ALTER TABLE lab_sample DROP INDEX idx_lab_sample_publish_flow_id',
    'SELECT 1'
);

PREPARE drop_sample_publish_flow_index_stmt FROM @drop_sample_publish_flow_index_sql;
EXECUTE drop_sample_publish_flow_index_stmt;
DEALLOCATE PREPARE drop_sample_publish_flow_index_stmt;

SET @sample_publish_flow_id_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND COLUMN_NAME = 'publish_flow_id'
);

SET @drop_sample_publish_flow_id_sql = IF(
    @sample_publish_flow_id_exists > 0,
    'ALTER TABLE lab_sample DROP COLUMN publish_flow_id',
    'SELECT 1'
);

PREPARE drop_sample_publish_flow_id_stmt FROM @drop_sample_publish_flow_id_sql;
EXECUTE drop_sample_publish_flow_id_stmt;
DEALLOCATE PREPARE drop_sample_publish_flow_id_stmt;

SET @sample_publish_flow_name_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND COLUMN_NAME = 'publish_flow_name'
);

SET @drop_sample_publish_flow_name_sql = IF(
    @sample_publish_flow_name_exists > 0,
    'ALTER TABLE lab_sample DROP COLUMN publish_flow_name',
    'SELECT 1'
);

PREPARE drop_sample_publish_flow_name_stmt FROM @drop_sample_publish_flow_name_sql;
EXECUTE drop_sample_publish_flow_name_stmt;
DEALLOCATE PREPARE drop_sample_publish_flow_name_stmt;
