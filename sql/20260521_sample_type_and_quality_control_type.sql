-- 样品类型与质控类型补充：
-- 1. 样品类型新增“水源水”
-- 2. 样品登录新增“质控类型”，用于标识平行样、空白样、质控样

SET @quality_control_type_column_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sample'
      AND COLUMN_NAME = 'quality_control_type'
);
SET @quality_control_type_column_sql = IF(
    @quality_control_type_column_exists = 0,
    'ALTER TABLE lab_sample ADD COLUMN quality_control_type VARCHAR(32) NULL COMMENT ''质控类型'' AFTER sample_type',
    'SELECT 1'
);
PREPARE quality_control_type_column_stmt FROM @quality_control_type_column_sql;
EXECUTE quality_control_type_column_stmt;
DEALLOCATE PREPARE quality_control_type_column_stmt;

UPDATE lab_dict
SET item_text = CONCAT(
        'FACTORY=出厂水', CHAR(10),
        'RAW=原水', CHAR(10),
        'TERMINAL=管网末梢', CHAR(10),
        'SOURCE_WATER=水源水'
    ),
    updated_time = NOW()
WHERE dict_code = 'sample_type';

INSERT INTO lab_dict (
    id,
    dict_code,
    dict_name,
    module_name,
    item_text,
    status,
    remark,
    deleted,
    created_time,
    updated_time
)
SELECT
    CAST((UNIX_TIMESTAMP(NOW(3)) * 1000) AS UNSIGNED),
    'quality_control_type',
    '质控类型',
    '采样管理',
    CONCAT(
        'PARALLEL=平行样', CHAR(10),
        'BLANK=空白样', CHAR(10),
        'QUALITY_CONTROL=质控样'
    ),
    1,
    '系统内置：样品登录时用于标识本次样品的质控属性。',
    0,
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM lab_dict
    WHERE dict_code = 'quality_control_type'
);

UPDATE lab_dict
SET item_text = CONCAT(
        'PARALLEL=平行样', CHAR(10),
        'BLANK=空白样', CHAR(10),
        'QUALITY_CONTROL=质控样'
    ),
    dict_name = '质控类型',
    module_name = '采样管理',
    status = 1,
    updated_time = NOW()
WHERE dict_code = 'quality_control_type';
