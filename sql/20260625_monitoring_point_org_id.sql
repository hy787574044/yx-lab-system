-- ============================================
-- 监测点位：所属水厂 → 所属机构
-- ============================================

USE yx_lab;

-- 1. 监测点位表新增 org_id 字段（如果不存在）
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'yx_lab'
    AND TABLE_NAME = 'lab_monitoring_point'
    AND COLUMN_NAME = 'org_id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE lab_monitoring_point ADD COLUMN org_id BIGINT AFTER point_name',
    'SELECT "org_id column already exists"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 添加索引（如果不存在）
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = 'yx_lab'
    AND TABLE_NAME = 'lab_monitoring_point'
    AND INDEX_NAME = 'idx_lab_monitoring_point_org_id'
);

SET @sql = IF(@index_exists = 0,
    'ALTER TABLE lab_monitoring_point ADD INDEX idx_lab_monitoring_point_org_id (org_id)',
    'SELECT "index already exists"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 迁移现有数据（如果有 region_name 字段）
SET @region_column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'yx_lab'
    AND TABLE_NAME = 'lab_monitoring_point'
    AND COLUMN_NAME = 'region_name'
);

SET @sql = IF(@region_column_exists > 0,
    'UPDATE lab_monitoring_point mp SET org_id = (SELECT o.id FROM lab_org o WHERE o.org_name = mp.region_name AND o.parent_id IS NOT NULL LIMIT 1) WHERE org_id IS NULL',
    'SELECT "region_name column not exists, skip migration"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 删除 region_name 字段（如果存在）
SET @sql = IF(@region_column_exists > 0,
    'ALTER TABLE lab_monitoring_point DROP COLUMN region_name',
    'SELECT "region_name column not exists, skip drop"'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 删除 water_plant 数据字典
DELETE FROM lab_dict WHERE dict_code = 'water_plant';
