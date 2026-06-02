-- 保留系统管理配置，清理并重建业务演示数据。
-- 保留不清理：角色、组织、字典、用户、流程、报告模板、检测参数、检测方法、检测套餐、检测步骤等系统/基础配置。
-- 本脚本会补齐：检测参数参数类别、检测方法采样容量、检测套餐参数-方法绑定、检测方法-设备型号绑定。
-- 本脚本会清理：监测点、采样计划、采样任务、样品、检测、审核、报告、仪器台账、文档、登录日志等业务数据。

USE yx_lab;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 兼容性补丁：不使用 MySQL 8 才支持的 ADD COLUMN IF NOT EXISTS。
CREATE TABLE IF NOT EXISTS lab_detection_project_group (
    id BIGINT PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    enabled TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='检测项目组表';

CREATE TABLE IF NOT EXISTS lab_detection_method (
    id BIGINT PRIMARY KEY,
    method_name VARCHAR(128) NOT NULL,
    method_code VARCHAR(100),
    parameter_id BIGINT,
    parameter_name VARCHAR(100),
    standard_code VARCHAR(100),
    sample_volume VARCHAR(64),
    method_basis VARCHAR(1000),
    apply_scope VARCHAR(500),
    enabled TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='检测方法表';

CREATE TABLE IF NOT EXISTS lab_sample_no_sequence (
    sequence_date VARCHAR(8) PRIMARY KEY,
    current_value BIGINT NOT NULL DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lab_detection_method_instrument_model_binding (
    id BIGINT NOT NULL PRIMARY KEY,
    method_id BIGINT NOT NULL,
    method_name VARCHAR(128) NOT NULL,
    instrument_model VARCHAR(128) NOT NULL,
    manufacturer VARCHAR(128) NOT NULL DEFAULT '',
    instrument_count INT DEFAULT 0,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_method_id (method_id),
    KEY idx_instrument_model (instrument_model),
    KEY idx_model_manufacturer (instrument_model, manufacturer)
) COMMENT='检测方法与设备型号绑定关系';

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_detection_parameter ADD COLUMN parameter_category VARCHAR(32) NOT NULL DEFAULT ''实验室测定'' COMMENT ''参数类别'' AFTER parameter_name',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_detection_parameter'
      AND COLUMN_NAME = 'parameter_category'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_detection_method ADD COLUMN sample_volume VARCHAR(64) NULL COMMENT ''采样容量'' AFTER standard_code',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_detection_method'
      AND COLUMN_NAME = 'sample_volume'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_plan ADD COLUMN detection_type_id BIGINT NULL COMMENT ''检测套餐ID'' AFTER sample_type',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_plan'
      AND COLUMN_NAME = 'detection_type_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_plan ADD COLUMN detection_type_name VARCHAR(128) NULL COMMENT ''检测套餐名称'' AFTER detection_type_id',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_plan'
      AND COLUMN_NAME = 'detection_type_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_plan ADD COLUMN detection_config_snapshot TEXT NULL COMMENT ''检测配置快照'' AFTER detection_type_name',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_plan'
      AND COLUMN_NAME = 'detection_config_snapshot'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_plan ADD COLUMN sampling_basis VARCHAR(1000) NULL COMMENT ''采样依据'' AFTER detection_config_snapshot',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_plan'
      AND COLUMN_NAME = 'sampling_basis'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN sample_no VARCHAR(64) NULL COMMENT ''样品编号'' AFTER task_no',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'sample_no'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN detection_type_id BIGINT NULL COMMENT ''检测套餐ID'' AFTER detection_items',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'detection_type_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN detection_type_name VARCHAR(128) NULL COMMENT ''检测套餐名称'' AFTER detection_type_id',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'detection_type_name'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN detection_config_snapshot TEXT NULL COMMENT ''检测配置快照'' AFTER detection_type_name',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'detection_config_snapshot'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN sampling_basis VARCHAR(1000) NULL COMMENT ''采样依据'' AFTER detection_config_snapshot',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'sampling_basis'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN weather VARCHAR(64) NULL COMMENT ''天气'' AFTER onsite_metrics',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'weather'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN temperature VARCHAR(32) NULL COMMENT ''温度'' AFTER weather',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'temperature'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN address VARCHAR(255) NULL COMMENT ''采样地址'' AFTER remark',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'address'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN latitude VARCHAR(32) NULL COMMENT ''纬度'' AFTER address',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'latitude'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_sampling_task ADD COLUMN longitude VARCHAR(32) NULL COMMENT ''经度'' AFTER latitude',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_sampling_task'
      AND COLUMN_NAME = 'longitude'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE lab_detection_record ADD COLUMN remark VARCHAR(500) NULL COMMENT ''备注'' AFTER abnormal_remark',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'lab_detection_record'
      AND COLUMN_NAME = 'remark'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 保留系统配置，只清理业务运行数据。
TRUNCATE TABLE lab_report_push_record;
TRUNCATE TABLE lab_report;
TRUNCATE TABLE lab_review_record;
TRUNCATE TABLE lab_detection_item;
TRUNCATE TABLE lab_detection_record;
TRUNCATE TABLE lab_sample;
TRUNCATE TABLE lab_sampling_task;
TRUNCATE TABLE lab_sample_no_sequence;
TRUNCATE TABLE lab_sampling_plan;
TRUNCATE TABLE lab_monitoring_point;
TRUNCATE TABLE lab_instrument_maintenance;
TRUNCATE TABLE lab_instrument;
TRUNCATE TABLE lab_document_share;
TRUNCATE TABLE lab_document;
TRUNCATE TABLE lab_login_log;

-- 参数类别字典补齐，不清空已有字典。
INSERT INTO lab_dict (
    id, dict_code, dict_name, module_name, item_text, status, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES (
    2026052801,
    'detection_parameter_category',
    '参数类别',
    '检测管理',
    'IN_SITU=原位检测\nFIELD=现场测定\nLABORATORY=实验室测定',
    1,
    '检测参数基础台账参数类别',
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
)
ON DUPLICATE KEY UPDATE
    dict_name = VALUES(dict_name),
    module_name = VALUES(module_name),
    item_text = VALUES(item_text),
    status = 1,
    deleted = 0,
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name),
    updated_time = NOW();

-- 补齐演示账号，不清理或删除已有系统用户。
INSERT INTO lab_user (
    id, username, password, real_name, org_id, org_name, role_code, phone, avatar_url,
    status, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(1101, 'sampler01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '陈采样', 802, '采样组', 'SAMPLER', '13810001001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1102, 'sampler02', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '李采样', 802, '采样组', 'SAMPLER', '13810001002', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1201, 'detector01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '王检测', 803, '检测审核组', 'DETECTOR', '13810002001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1202, 'detector02', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '赵检测', 803, '检测审核组', 'DETECTOR', '13810002002', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1203, 'detector03', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '周检测', 803, '检测审核组', 'DETECTOR', '13810002003', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1301, 'reviewer01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '刘审核', 803, '检测审核组', 'REVIEWER', '13810003001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1302, 'reviewer02', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '何审核', 803, '检测审核组', 'REVIEWER', '13810003002', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1401, 'reporter01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '孙报告', 803, '检测审核组', 'REPORTER', '13810004001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW())
ON DUPLICATE KEY UPDATE
    real_name = VALUES(real_name),
    org_id = VALUES(org_id),
    org_name = VALUES(org_name),
    role_code = VALUES(role_code),
    phone = VALUES(phone),
    status = 1,
    deleted = 0,
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name),
    updated_time = NOW();

-- 检测基础配置补齐：不清空整表，按固定演示 ID 补齐/更新。
INSERT INTO lab_detection_project_group (
    id, group_name, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(705001, '常规理化组', 1, '覆盖 pH、浊度、耗氧量等日常理化指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705002, '消毒指标组', 1, '覆盖余氯、氨氮等消毒与氮素指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705003, '金属指标组', 1, '覆盖铁、锰等金属风险指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705004, '感官指标组', 1, '覆盖色度、臭和味、肉眼可见物', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW())
ON DUPLICATE KEY UPDATE
    group_name = VALUES(group_name),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    deleted = 0,
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name),
    updated_time = NOW();

INSERT INTO lab_detection_parameter (
    id, parameter_name, parameter_category, standard_min, standard_max, unit, exceed_rule,
    reference_standard, enabled, remark, deleted, created_by, created_name, created_time,
    updated_by, updated_name, updated_time
)
VALUES
(700001, 'pH', '原位检测', 6.50, 8.50, '', 'OUT_OF_RANGE', 'GB 5749-2022', 1, '生活饮用水酸碱度', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700002, '浊度', '现场测定', 0.00, 1.00, 'NTU', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '反映水体悬浮物情况', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700003, '余氯', '现场测定', 0.05, 2.00, 'mg/L', 'OUT_OF_RANGE', 'GB 5749-2022', 1, '消毒剂余量控制指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700004, '氨氮', '实验室测定', 0.00, 0.50, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '氮素污染风险指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700005, '色度', '现场测定', 0.00, 15.00, '度', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官性状指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700006, '臭和味', '现场测定', 0.00, 0.00, '级', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官性状指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700007, '肉眼可见物', '现场测定', 0.00, 0.00, '项', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官性状指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700008, '铁', '实验室测定', 0.00, 0.30, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '金属指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700009, '锰', '实验室测定', 0.00, 0.10, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '金属指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700010, '耗氧量', '实验室测定', 0.00, 3.00, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '有机污染综合指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW())
ON DUPLICATE KEY UPDATE
    parameter_name = VALUES(parameter_name),
    parameter_category = VALUES(parameter_category),
    standard_min = VALUES(standard_min),
    standard_max = VALUES(standard_max),
    unit = VALUES(unit),
    exceed_rule = VALUES(exceed_rule),
    reference_standard = VALUES(reference_standard),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    deleted = 0,
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name),
    updated_time = NOW();

UPDATE lab_detection_parameter
SET parameter_category = CASE
    WHEN parameter_name = 'pH' THEN '原位检测'
    WHEN parameter_name IN ('浊度', '余氯', '色度', '臭和味', '肉眼可见物') THEN '现场测定'
    ELSE '实验室测定'
END
WHERE deleted = 0
  AND (parameter_category IS NULL OR parameter_category = '' OR parameter_name IN ('pH', '浊度', '余氯', '色度', '臭和味', '肉眼可见物', '氨氮', '铁', '锰', '耗氧量'));

INSERT INTO lab_detection_method (
    id, method_name, method_code, parameter_id, parameter_name, standard_code, sample_volume,
    method_basis, apply_scope, enabled, remark, deleted, created_by, created_name, created_time,
    updated_by, updated_name, updated_time
)
VALUES
(710001, '玻璃电极法', 'DEMO-PH-01', 700001, 'pH', 'GB/T 5750.4', '100mL', '校准 pH 计后直接测定，记录温度补偿后的稳定读数。', '出厂水、原水、管网水 pH 检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710002, '散射光浊度法', 'DEMO-TURB-01', 700002, '浊度', 'GB/T 5750.4', '50mL', '摇匀样品后装入比色瓶，使用浊度仪读取 NTU 值。', '生活饮用水浊度检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710003, 'DPD 分光光度法', 'DEMO-CL-01', 700003, '余氯', 'GB/T 5750.11', '10mL', '加入 DPD 试剂显色，在规定波长下测定吸光度并换算浓度。', '出厂水和管网水余氯检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710004, '纳氏试剂分光光度法', 'DEMO-NH3N-01', 700004, '氨氮', 'HJ 535', '50mL', '样品经预处理后加入纳氏试剂显色，使用分光光度计测定。', '原水和异常复检样品氨氮检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710005, '铂钴标准比色法', 'DEMO-COLOR-01', 700005, '色度', 'GB/T 5750.4', '50mL', '与铂钴标准色列比对，读取最接近色度值。', '感官指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710006, '嗅味直接判断法', 'DEMO-ODOR-01', 700006, '臭和味', 'GB/T 5750.4', '250mL', '按标准温度条件嗅辨并记录等级。', '感官指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710007, '目视观察法', 'DEMO-VISIBLE-01', 700007, '肉眼可见物', 'GB/T 5750.4', '250mL', '取样后在自然光下目视观察是否存在可见悬浮物。', '感官指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710008, '原子吸收分光光度法-铁', 'DEMO-FE-01', 700008, '铁', 'GB/T 5750.6', '100mL', '消解后使用原子吸收分光光度计测定铁含量。', '金属指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710009, '原子吸收分光光度法-锰', 'DEMO-MN-01', 700009, '锰', 'GB/T 5750.6', '100mL', '消解后使用原子吸收分光光度计测定锰含量。', '金属指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710010, '酸性高锰酸钾滴定法', 'DEMO-CODMN-01', 700010, '耗氧量', 'GB/T 5750.7', '100mL', '酸性条件下高锰酸钾氧化，滴定计算耗氧量。', '有机污染综合检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW())
ON DUPLICATE KEY UPDATE
    method_name = VALUES(method_name),
    method_code = VALUES(method_code),
    parameter_id = VALUES(parameter_id),
    parameter_name = VALUES(parameter_name),
    standard_code = VALUES(standard_code),
    sample_volume = VALUES(sample_volume),
    method_basis = VALUES(method_basis),
    apply_scope = VALUES(apply_scope),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    deleted = 0,
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name),
    updated_time = NOW();

SET @binding_common = '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700003,"methodIds":[710003]},{"parameterId":700004,"methodIds":[710004]},{"parameterId":700005,"methodIds":[710005]},{"parameterId":700006,"methodIds":[710006]},{"parameterId":700007,"methodIds":[710007]},{"parameterId":700008,"methodIds":[710008]},{"parameterId":700010,"methodIds":[710010]}]';
SET @binding_raw = '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700004,"methodIds":[710004]},{"parameterId":700009,"methodIds":[710009]},{"parameterId":700010,"methodIds":[710010]}]';
SET @binding_terminal = '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700003,"methodIds":[710003]},{"parameterId":700007,"methodIds":[710007]}]';
SET @binding_emergency = '[{"parameterId":700002,"methodIds":[710002]},{"parameterId":700004,"methodIds":[710004]},{"parameterId":700008,"methodIds":[710008]},{"parameterId":700009,"methodIds":[710009]},{"parameterId":700010,"methodIds":[710010]}]';

INSERT INTO lab_detection_type (
    id, type_name, group_id, group_name, detector_id, detector_name, parameter_ids, parameter_names,
    parameter_method_bindings, enabled, remark, deleted, created_by, created_name, created_time,
    updated_by, updated_name, updated_time
)
VALUES
(720001, '出厂水常规九项', 705001, '常规理化组', 1201, '王检测', '700001,700002,700003,700004,700005,700006,700007,700008,700010', 'pH,浊度,余氯,氨氮,色度,臭和味,肉眼可见物,铁,耗氧量', @binding_common, 1, '出厂水日常检测套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720002, '原水重点五项', 705002, '消毒指标组', 1202, '赵检测', '700001,700002,700004,700009,700010', 'pH,浊度,氨氮,锰,耗氧量', @binding_raw, 1, '原水重点风险指标套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720003, '管网末梢四项', 705004, '感官指标组', 1203, '周检测', '700001,700002,700003,700007', 'pH,浊度,余氯,肉眼可见物', @binding_terminal, 1, '管网末梢巡检套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720004, '应急复检套餐', 705003, '金属指标组', 1201, '王检测', '700002,700004,700008,700009,700010', '浊度,氨氮,铁,锰,耗氧量', @binding_emergency, 1, '异常样品复检套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW())
ON DUPLICATE KEY UPDATE
    type_name = VALUES(type_name),
    group_id = VALUES(group_id),
    group_name = VALUES(group_name),
    detector_id = VALUES(detector_id),
    detector_name = VALUES(detector_name),
    parameter_ids = VALUES(parameter_ids),
    parameter_names = VALUES(parameter_names),
    parameter_method_bindings = VALUES(parameter_method_bindings),
    enabled = VALUES(enabled),
    remark = VALUES(remark),
    deleted = 0,
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name),
    updated_time = NOW();

INSERT INTO lab_detection_step (
    id, type_id, type_name, step_name, step_order, step_description, reagent_requirement,
    operation_requirement, remark, deleted, created_by, created_name, created_time,
    updated_by, updated_name, updated_time
)
VALUES
(730001, 720001, '出厂水常规九项', '样品核验与预处理', 1, '核对样品编号、采样点位、保存条件并混匀样品。', '无', '确认样品外观无泄漏，必要时记录异常。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730002, 720001, '出厂水常规九项', '理化与感官检测', 2, '依次完成 pH、浊度、色度、臭和味等指标检测。', '标准缓冲液、比色试剂', '按检测步骤逐项记录原始值。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730003, 720002, '原水重点五项', '原水重点指标检测', 1, '完成原水 pH、浊度、氨氮、锰、耗氧量检测。', '纳氏试剂、标准溶液', '对异常指标进行复测确认。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730004, 720003, '管网末梢四项', '末梢快速检测', 1, '对末梢水 pH、浊度、余氯、肉眼可见物进行快速检测。', 'DPD 试剂', '余氯需现场快速检测并记录时间。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730005, 720004, '应急复检套餐', '异常指标复检', 1, '对被驳回或异常样品执行重点复检。', '复检标准液', '复检结果需附异常说明。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW())
ON DUPLICATE KEY UPDATE
    type_id = VALUES(type_id),
    type_name = VALUES(type_name),
    step_name = VALUES(step_name),
    step_order = VALUES(step_order),
    step_description = VALUES(step_description),
    reagent_requirement = VALUES(reagent_requirement),
    operation_requirement = VALUES(operation_requirement),
    remark = VALUES(remark),
    deleted = 0,
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name),
    updated_time = NOW();

SET @snapshot_common = '[{"parameterId":700001,"parameterName":"pH","parameterCategory":"原位检测","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","sampleVolume":"100mL","methodBasis":"校准 pH 计后直接测定，记录温度补偿后的稳定读数。"},{"parameterId":700002,"parameterName":"浊度","parameterCategory":"现场测定","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","sampleVolume":"50mL","methodBasis":"摇匀样品后装入比色瓶，使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","parameterCategory":"现场测定","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD 分光光度法","sampleVolume":"10mL","methodBasis":"加入 DPD 试剂显色，在规定波长下测定吸光度并换算浓度。"},{"parameterId":700004,"parameterName":"氨氮","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":0.50,"referenceStandard":"GB 5749-2022","methodId":710004,"methodName":"纳氏试剂分光光度法","sampleVolume":"50mL","methodBasis":"样品经预处理后加入纳氏试剂显色，使用分光光度计测定。"},{"parameterId":700005,"parameterName":"色度","parameterCategory":"现场测定","unit":"度","standardMin":0.00,"standardMax":15.00,"referenceStandard":"GB 5749-2022","methodId":710005,"methodName":"铂钴标准比色法","sampleVolume":"50mL","methodBasis":"与铂钴标准色列比对，读取最接近色度值。"},{"parameterId":700006,"parameterName":"臭和味","parameterCategory":"现场测定","unit":"级","standardMin":0.00,"standardMax":0.00,"referenceStandard":"GB 5749-2022","methodId":710006,"methodName":"嗅味直接判断法","sampleVolume":"250mL","methodBasis":"按标准温度条件嗅辨并记录等级。"},{"parameterId":700007,"parameterName":"肉眼可见物","parameterCategory":"现场测定","unit":"项","standardMin":0.00,"standardMax":0.00,"referenceStandard":"GB 5749-2022","methodId":710007,"methodName":"目视观察法","sampleVolume":"250mL","methodBasis":"取样后在自然光下目视观察是否存在可见悬浮物。"},{"parameterId":700008,"parameterName":"铁","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":0.30,"referenceStandard":"GB 5749-2022","methodId":710008,"methodName":"原子吸收分光光度法-铁","sampleVolume":"100mL","methodBasis":"消解后使用原子吸收分光光度计测定铁含量。"},{"parameterId":700010,"parameterName":"耗氧量","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":3.00,"referenceStandard":"GB 5749-2022","methodId":710010,"methodName":"酸性高锰酸钾滴定法","sampleVolume":"100mL","methodBasis":"酸性条件下高锰酸钾氧化，滴定计算耗氧量。"}]';
SET @snapshot_raw = '[{"parameterId":700001,"parameterName":"pH","parameterCategory":"原位检测","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","sampleVolume":"100mL","methodBasis":"校准 pH 计后直接测定，记录温度补偿后的稳定读数。"},{"parameterId":700002,"parameterName":"浊度","parameterCategory":"现场测定","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","sampleVolume":"50mL","methodBasis":"摇匀样品后装入比色瓶，使用浊度仪读取 NTU 值。"},{"parameterId":700004,"parameterName":"氨氮","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":0.50,"referenceStandard":"GB 5749-2022","methodId":710004,"methodName":"纳氏试剂分光光度法","sampleVolume":"50mL","methodBasis":"样品经预处理后加入纳氏试剂显色，使用分光光度计测定。"},{"parameterId":700009,"parameterName":"锰","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":0.10,"referenceStandard":"GB 5749-2022","methodId":710009,"methodName":"原子吸收分光光度法-锰","sampleVolume":"100mL","methodBasis":"消解后使用原子吸收分光光度计测定锰含量。"},{"parameterId":700010,"parameterName":"耗氧量","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":3.00,"referenceStandard":"GB 5749-2022","methodId":710010,"methodName":"酸性高锰酸钾滴定法","sampleVolume":"100mL","methodBasis":"酸性条件下高锰酸钾氧化，滴定计算耗氧量。"}]';
SET @snapshot_terminal = '[{"parameterId":700001,"parameterName":"pH","parameterCategory":"原位检测","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","sampleVolume":"100mL","methodBasis":"校准 pH 计后直接测定，记录温度补偿后的稳定读数。"},{"parameterId":700002,"parameterName":"浊度","parameterCategory":"现场测定","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","sampleVolume":"50mL","methodBasis":"摇匀样品后装入比色瓶，使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","parameterCategory":"现场测定","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD 分光光度法","sampleVolume":"10mL","methodBasis":"加入 DPD 试剂显色，在规定波长下测定吸光度并换算浓度。"},{"parameterId":700007,"parameterName":"肉眼可见物","parameterCategory":"现场测定","unit":"项","standardMin":0.00,"standardMax":0.00,"referenceStandard":"GB 5749-2022","methodId":710007,"methodName":"目视观察法","sampleVolume":"250mL","methodBasis":"取样后在自然光下目视观察是否存在可见悬浮物。"}]';
SET @snapshot_emergency = '[{"parameterId":700002,"parameterName":"浊度","parameterCategory":"现场测定","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","sampleVolume":"50mL","methodBasis":"摇匀样品后装入比色瓶，使用浊度仪读取 NTU 值。"},{"parameterId":700004,"parameterName":"氨氮","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":0.50,"referenceStandard":"GB 5749-2022","methodId":710004,"methodName":"纳氏试剂分光光度法","sampleVolume":"50mL","methodBasis":"样品经预处理后加入纳氏试剂显色，使用分光光度计测定。"},{"parameterId":700008,"parameterName":"铁","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":0.30,"referenceStandard":"GB 5749-2022","methodId":710008,"methodName":"原子吸收分光光度法-铁","sampleVolume":"100mL","methodBasis":"消解后使用原子吸收分光光度计测定铁含量。"},{"parameterId":700009,"parameterName":"锰","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":0.10,"referenceStandard":"GB 5749-2022","methodId":710009,"methodName":"原子吸收分光光度法-锰","sampleVolume":"100mL","methodBasis":"消解后使用原子吸收分光光度计测定锰含量。"},{"parameterId":700010,"parameterName":"耗氧量","parameterCategory":"实验室测定","unit":"mg/L","standardMin":0.00,"standardMax":3.00,"referenceStandard":"GB 5749-2022","methodId":710010,"methodName":"酸性高锰酸钾滴定法","sampleVolume":"100mL","methodBasis":"酸性条件下高锰酸钾氧化，滴定计算耗氧量。"}]';

SET @sampling_basis_routine = 'GB/T 5750.2-2023 生活饮用水标准检验方法 水样采集与保存; CJ/T 206-2005 城市供水水质标准';
SET @sampling_basis_raw = 'HJ 91.1-2019 污水监测技术规范; GB/T 5750.2-2023 水样采集与保存';
SET @sampling_basis_terminal = '生活饮用水卫生标准 GB 5749-2022; 管网末梢巡检作业指导书';

SET @currentDate = DATE_FORMAT(CURDATE(), '%Y%m%d');
SET @sampleNo1 = CONCAT('YX', @currentDate, '0001');
SET @sampleNo2 = CONCAT('YX', @currentDate, '0002');
SET @sampleNo3 = CONCAT('YX', @currentDate, '0003');
SET @sampleNo4 = CONCAT('YX', @currentDate, '0004');
SET @sampleNo5 = CONCAT('YX', @currentDate, '0005');
SET @sampleNo6 = CONCAT('YX', @currentDate, '0006');
SET @sampleNo7 = CONCAT('YX', @currentDate, '0007');
SET @sampleNo8 = CONCAT('YX', @currentDate, '0008');
SET @sampleNo9 = CONCAT('YX', @currentDate, '0009');

SET @reviewFlowId = COALESCE((SELECT id FROM lab_flow_config WHERE flow_type = 'REVIEW' AND status = 1 AND deleted = 0 ORDER BY default_flag DESC, id LIMIT 1), 9601);
SET @reviewFlowName = COALESCE((SELECT flow_name FROM lab_flow_config WHERE id = @reviewFlowId LIMIT 1), '常规三级审核');
SET @reviewNodeId = COALESCE((SELECT id FROM lab_flow_node WHERE flow_id = @reviewFlowId AND required_flag = 1 AND deleted = 0 ORDER BY node_order, id LIMIT 1), 9611);
SET @reviewNodeName = COALESCE((SELECT node_name FROM lab_flow_node WHERE id = @reviewNodeId LIMIT 1), '初审');
SET @reviewNodeOrder = COALESCE((SELECT node_order FROM lab_flow_node WHERE id = @reviewNodeId LIMIT 1), 1);
SET @reviewRequiredFlag = COALESCE((SELECT required_flag FROM lab_flow_node WHERE id = @reviewNodeId LIMIT 1), 1);
SET @publishFlowId = COALESCE((SELECT id FROM lab_flow_config WHERE flow_type = 'PUBLISH' AND status = 1 AND deleted = 0 ORDER BY default_flag DESC, id LIMIT 1), 9602);
SET @publishFlowName = COALESCE((SELECT flow_name FROM lab_flow_config WHERE id = @publishFlowId LIMIT 1), '报告发布审批');

-- 1. 监测点位。
INSERT INTO lab_monitoring_point (
    id, point_name, longitude, latitude, region_name, service_population, frequency_type,
    owner_id, owner_name, contact_phone, point_type, point_status, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(940001, '城东水厂出厂水', '115.2121', '30.2211', '阳新县城东片区', 36000, 'DAILY', 1101, '陈采样', '13810001001', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 30 DAY, 1001, '系统管理员', NOW()),
(940002, '城西水厂出厂水', '115.1781', '30.2051', '阳新县城西片区', 42000, 'DAILY', 1102, '李采样', '13810001002', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 28 DAY, 1001, '系统管理员', NOW()),
(940003, '富河原水取水口', '115.0912', '30.1548', '富河流域', 68000, 'WEEKLY', 1101, '陈采样', '13810001001', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 25 DAY, 1001, '系统管理员', NOW()),
(940004, '兴国大道管网末梢', '115.2266', '30.2199', '兴国大道', 12000, 'DAILY', 1102, '李采样', '13810001002', 'TERMINAL', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW()),
(940005, '莲花湖社区末梢点', '115.2442', '30.2366', '莲花湖社区', 9800, 'WEEKLY', 1101, '陈采样', '13810001001', 'TERMINAL', 'DISABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 15 DAY, 1001, '系统管理员', NOW()),
(940006, '应急加密监测点', '115.2622', '30.2411', '城南片区', 6000, 'DAILY', 1102, '李采样', '13810001002', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 12 DAY, 1001, '系统管理员', NOW());

-- 2. 采样计划，检测套餐与检测参数快照前置到计划。
INSERT INTO lab_sampling_plan (
    id, plan_name, point_id, point_name, start_time, end_time, sampler_id, sampler_name,
    sampling_type, sample_type, detection_type_id, detection_type_name, detection_config_snapshot,
    sampling_basis, cycle_type, plan_status, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(950001, '城东水厂每日出厂水计划', 940001, '城东水厂出厂水', CURDATE() - INTERVAL 20 DAY, CURDATE() + INTERVAL 40 DAY, 1101, '陈采样', 'ROUTINE', 'FACTORY', 720001, '出厂水常规九项', @snapshot_common, @sampling_basis_routine, 'DAILY', 'ACTIVE', '每日早班采集出厂水，套餐在计划阶段确认。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(950002, '富河原水每周计划', 940003, '富河原水取水口', CURDATE() - INTERVAL 16 DAY, CURDATE() + INTERVAL 60 DAY, 1101, '陈采样', 'ROUTINE', 'RAW', 720002, '原水重点五项', @snapshot_raw, @sampling_basis_raw, 'WEEKLY', 'DISPATCHED', '每周一采集原水样品。', 0, 1001, '系统管理员', NOW() - INTERVAL 16 DAY, 1001, '系统管理员', NOW()),
(950003, '兴国大道管网末梢巡检计划', 940004, '兴国大道管网末梢', CURDATE() - INTERVAL 14 DAY, CURDATE() + INTERVAL 45 DAY, 1102, '李采样', 'ROUTINE', 'TERMINAL', 720003, '管网末梢四项', @snapshot_terminal, @sampling_basis_terminal, 'DAILY', 'ACTIVE', '管网末梢每日巡检。', 0, 1001, '系统管理员', NOW() - INTERVAL 14 DAY, 1001, '系统管理员', NOW()),
(950004, '城西水厂暂停计划', 940002, '城西水厂出厂水', CURDATE() - INTERVAL 10 DAY, CURDATE() + INTERVAL 20 DAY, 1102, '李采样', 'ROUTINE', 'FACTORY', 720001, '出厂水常规九项', @snapshot_common, @sampling_basis_routine, 'DAILY', 'PAUSED', '演示暂停计划。', 0, 1001, '系统管理员', NOW() - INTERVAL 10 DAY, 1001, '系统管理员', NOW()),
(950005, '应急加密一次性计划', 940006, '应急加密监测点', CURDATE() - INTERVAL 7 DAY, CURDATE() + INTERVAL 7 DAY, 1102, '李采样', 'ROUTINE', 'RAW', 720004, '应急复检套餐', @snapshot_emergency, @sampling_basis_raw, 'ONCE', 'COMPLETED', '异常天气后的应急复检计划。', 0, 1001, '系统管理员', NOW() - INTERVAL 7 DAY, 1001, '系统管理员', NOW());

-- 3. 采样任务，样品编号从任务生成阶段写入，不再使用封签号。
INSERT INTO lab_sampling_task (
    id, task_no, sample_no, plan_id, point_id, point_name, sampling_time, sampler_id, sampler_name,
    sample_type, sample_register_status, sample_id, detection_items, detection_type_id, detection_type_name,
    detection_config_snapshot, sampling_basis, task_status, started_time, onsite_metrics, weather, temperature,
    photo_urls, abandon_reason, finished_time, remark, address, latitude, longitude, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(960001, CONCAT('TASK-', @currentDate, '-001'), @sampleNo1, 950001, 940001, '城东水厂出厂水', NOW() - INTERVAL 6 DAY, 1101, '陈采样', 'FACTORY', 'REGISTERED', 970001, '出厂水常规九项', 720001, '出厂水常规九项', @snapshot_common, @sampling_basis_routine, 'COMPLETED', NOW() - INTERVAL 6 DAY + INTERVAL 1 HOUR, '现场余氯 0.42mg/L；水温 21.3℃', '晴', '21.3℃', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/photo-001.jpg', NULL, NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR, '样品已登录，待检测分样。', '城东水厂采样口', '30.2211', '115.2121', 0, 1101, '陈采样', NOW() - INTERVAL 6 DAY, 1101, '陈采样', NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR),
(960002, CONCAT('TASK-', @currentDate, '-002'), @sampleNo2, 950001, 940001, '城东水厂出厂水', NOW() - INTERVAL 1 DAY, 1101, '陈采样', 'FACTORY', 'UNREGISTERED', NULL, '出厂水常规九项', 720001, '出厂水常规九项', @snapshot_common, @sampling_basis_routine, 'COMPLETED', NOW() - INTERVAL 1 DAY + INTERVAL 1 HOUR, '现场状态正常，样品待登录。', '多云', '22.0℃', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/photo-002.jpg', NULL, NOW() - INTERVAL 1 DAY + INTERVAL 2 HOUR, '采样完成，样品登录页面应可登录。', '城东水厂采样口', '30.2211', '115.2121', 0, 1101, '陈采样', NOW() - INTERVAL 1 DAY, 1101, '陈采样', NOW() - INTERVAL 1 DAY + INTERVAL 2 HOUR),
(960003, CONCAT('TASK-', @currentDate, '-003'), @sampleNo3, 950002, 940003, '富河原水取水口', NOW(), 1101, '陈采样', 'RAW', 'UNREGISTERED', NULL, '原水重点五项', 720002, '原水重点五项', @snapshot_raw, @sampling_basis_raw, 'IN_PROGRESS', NOW() - INTERVAL 2 HOUR, '水位偏高，正在采样。', '小雨', '19.6℃', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/photo-003.jpg', NULL, NULL, '进行中任务，APP 可继续完成。', '富河取水泵房', '30.1548', '115.0912', 0, 1101, '陈采样', NOW(), 1101, '陈采样', NOW()),
(960004, CONCAT('TASK-', @currentDate, '-004'), @sampleNo4, 950003, 940004, '兴国大道管网末梢', NOW() + INTERVAL 1 DAY, 1102, '李采样', 'TERMINAL', 'UNREGISTERED', NULL, '管网末梢四项', 720003, '管网末梢四项', @snapshot_terminal, @sampling_basis_terminal, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '待处理任务，移动端优先展示。', '兴国大道末梢消火栓', '30.2199', '115.2266', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(960005, CONCAT('TASK-', @currentDate, '-005'), @sampleNo5, 950004, 940002, '城西水厂出厂水', NOW() - INTERVAL 4 DAY, 1102, '李采样', 'FACTORY', 'UNREGISTERED', NULL, '出厂水常规九项', 720001, '出厂水常规九项', @snapshot_common, @sampling_basis_routine, 'ABANDONED', NULL, NULL, NULL, NULL, NULL, '现场检修无法到达采样口。', NULL, '废弃任务演示。', '城西水厂采样口', '30.2051', '115.1781', 0, 1102, '李采样', NOW() - INTERVAL 4 DAY, 1102, '李采样', NOW() - INTERVAL 4 DAY),
(960006, CONCAT('TASK-', @currentDate, '-006'), @sampleNo6, 950002, 940003, '富河原水取水口', NOW() - INTERVAL 8 DAY, 1101, '陈采样', 'RAW', 'REGISTERED', 970002, '原水重点五项', 720002, '原水重点五项', @snapshot_raw, @sampling_basis_raw, 'COMPLETED', NOW() - INTERVAL 8 DAY + INTERVAL 1 HOUR, '原水浊度略高。', '阴', '18.9℃', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/photo-006.jpg', NULL, NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR, '已登录并完成检测分样。', '富河取水泵房', '30.1548', '115.0912', 0, 1101, '陈采样', NOW() - INTERVAL 8 DAY, 1101, '陈采样', NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR),
(960007, CONCAT('TASK-', @currentDate, '-007'), @sampleNo7, 950003, 940004, '兴国大道管网末梢', NOW() - INTERVAL 5 DAY, 1102, '李采样', 'TERMINAL', 'REGISTERED', 970003, '管网末梢四项', 720003, '管网末梢四项', @snapshot_terminal, @sampling_basis_terminal, 'COMPLETED', NOW() - INTERVAL 5 DAY + INTERVAL 1 HOUR, '末梢余氯偏低。', '晴', '23.1℃', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/photo-007.jpg', NULL, NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR, '检测已提交，待审核。', '兴国大道末梢消火栓', '30.2199', '115.2266', 0, 1102, '李采样', NOW() - INTERVAL 5 DAY, 1102, '李采样', NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR),
(960008, CONCAT('TASK-', @currentDate, '-008'), @sampleNo8, 950005, 940006, '应急加密监测点', NOW() - INTERVAL 3 DAY, 1102, '李采样', 'RAW', 'REGISTERED', 970004, '应急复检套餐', 720004, '应急复检套餐', @snapshot_emergency, @sampling_basis_raw, 'COMPLETED', NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR, '应急复检样品，现场浊度偏高。', '雷阵雨', '20.8℃', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/photo-008.jpg', NULL, NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, '审核驳回，进入重检。', '城南应急监测点', '30.2411', '115.2622', 0, 1102, '李采样', NOW() - INTERVAL 3 DAY, 1102, '李采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
(960009, CONCAT('TASK-', @currentDate, '-009'), @sampleNo9, 950001, 940001, '城东水厂出厂水', NOW() - INTERVAL 12 DAY, 1101, '陈采样', 'FACTORY', 'REGISTERED', 970005, '出厂水常规九项', 720001, '出厂水常规九项', @snapshot_common, @sampling_basis_routine, 'COMPLETED', NOW() - INTERVAL 12 DAY + INTERVAL 1 HOUR, '指标稳定。', '晴', '20.5℃', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/photo-009.jpg', NULL, NOW() - INTERVAL 12 DAY + INTERVAL 2 HOUR, '审核通过并已发布报告。', '城东水厂采样口', '30.2211', '115.2121', 0, 1101, '陈采样', NOW() - INTERVAL 12 DAY, 1101, '陈采样', NOW() - INTERVAL 12 DAY + INTERVAL 2 HOUR);

INSERT INTO lab_sample_no_sequence(sequence_date, current_value)
VALUES (@currentDate, 9)
ON DUPLICATE KEY UPDATE
    current_value = GREATEST(current_value, VALUES(current_value)),
    updated_time = NOW();

-- 4. 样品台账。
INSERT INTO lab_sample (
    id, sample_no, task_id, point_id, point_name, sample_type, quality_control_type,
    detection_items, detection_type_id, detection_type_name, detection_config_snapshot,
    review_flow_id, review_flow_name, publish_flow_id, publish_flow_name,
    sampling_time, sampler_id, sampler_name, weather, storage_condition,
    sample_status, result_summary, remark, trace_log, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(970001, @sampleNo1, 960001, 940001, '城东水厂出厂水', 'FACTORY', 'PARALLEL', '出厂水常规九项', 720001, '出厂水常规九项', @snapshot_common, @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, NOW() - INTERVAL 6 DAY, 1101, '陈采样', '晴', '4℃冷藏避光', 'LOGGED', '待检测分样', '样品已登录，等待检测分样。', CONCAT('样品登录：样品编号=', @sampleNo1, '，采样员=陈采样'), 0, 1101, '陈采样', NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR, 1101, '陈采样', NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR),
(970002, @sampleNo6, 960006, 940003, '富河原水取水口', 'RAW', NULL, '原水重点五项', 720002, '原水重点五项', @snapshot_raw, @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, NOW() - INTERVAL 8 DAY, 1101, '陈采样', '阴', '4℃冷藏送检', 'LOGGED', '已分配检测', '样品已分配检测员，待检测结果录入。', CONCAT('样品登录：样品编号=', @sampleNo6, '，检测已分配。'), 0, 1101, '陈采样', NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR, 1202, '赵检测', NOW() - INTERVAL 8 DAY + INTERVAL 3 HOUR),
(970003, @sampleNo7, 960007, 940004, '兴国大道管网末梢', 'TERMINAL', 'BLANK', '管网末梢四项', 720003, '管网末梢四项', @snapshot_terminal, @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, NOW() - INTERVAL 5 DAY, 1102, '李采样', '晴', '常温送检', 'REVIEWING', '异常待审核', '余氯偏低，检测已提交等待审核。', CONCAT('样品登录：样品编号=', @sampleNo7, '，检测结果=异常。'), 0, 1102, '李采样', NOW() - INTERVAL 5 DAY + INTERVAL 2 HOUR, 1203, '周检测', NOW() - INTERVAL 5 DAY + INTERVAL 5 HOUR),
(970004, @sampleNo8, 960008, 940006, '应急加密监测点', 'RAW', 'QUALITY_CONTROL', '应急复检套餐', 720004, '应急复检套餐', @snapshot_emergency, @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, NOW() - INTERVAL 3 DAY, 1102, '李采样', '雷阵雨', '4℃冷藏送检', 'RETEST', '审核驳回待重检', '异常样品已被驳回，等待复检。', CONCAT('样品登录：样品编号=', @sampleNo8, '，审核驳回进入重检。'), 0, 1102, '李采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 1302, '何审核', NOW() - INTERVAL 2 DAY),
(970005, @sampleNo9, 960009, 940001, '城东水厂出厂水', 'FACTORY', NULL, '出厂水常规九项', 720001, '出厂水常规九项', @snapshot_common, @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, NOW() - INTERVAL 12 DAY, 1101, '陈采样', '晴', '4℃冷藏避光', 'COMPLETED', '正常', '审核通过并已发布报告。', CONCAT('样品登录：样品编号=', @sampleNo9, '，检测正常，报告已发布。'), 0, 1101, '陈采样', NOW() - INTERVAL 12 DAY + INTERVAL 2 HOUR, 1401, '孙报告', NOW() - INTERVAL 10 DAY);

-- 5. 检测记录与检测参数子流程。
INSERT INTO lab_detection_record (
    id, sample_id, sample_no, detection_type_id, detection_type_name, detection_time,
    detector_id, detector_name, detection_result, abnormal_remark, remark, detection_status, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(980001, 970001, @sampleNo1, 720001, '出厂水常规九项', NULL, NULL, NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 1101, '陈采样', NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR, 1101, '陈采样', NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR),
(980002, 970002, @sampleNo6, 720002, '原水重点五项', NOW() - INTERVAL 8 DAY + INTERVAL 4 HOUR, 1202, '赵检测', NULL, NULL, '已分配检测员，等待录入结果。', 'WAIT_DETECT', 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY + INTERVAL 3 HOUR, 1202, '赵检测', NOW() - INTERVAL 8 DAY + INTERVAL 4 HOUR),
(980003, 970003, @sampleNo7, 720003, '管网末梢四项', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR, 1203, '周检测', 'ABNORMAL', '余氯低于控制要求，需要审核确认。', '现场复核建议尽快补氯。', 'SUBMITTED', 0, 1203, '周检测', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR, 1203, '周检测', NOW() - INTERVAL 5 DAY + INTERVAL 5 HOUR),
(980004, 970004, @sampleNo8, 720004, '应急复检套餐', NOW() - INTERVAL 3 DAY + INTERVAL 5 HOUR, 1201, '王检测', 'ABNORMAL', '氨氮与浊度异常，审核驳回重检。', '需补充现场复核说明。', 'REJECTED', 0, 1201, '王检测', NOW() - INTERVAL 3 DAY + INTERVAL 5 HOUR, 1302, '何审核', NOW() - INTERVAL 2 DAY),
(980005, 970005, @sampleNo9, 720001, '出厂水常规九项', NOW() - INTERVAL 12 DAY + INTERVAL 4 HOUR, 1201, '王检测', 'NORMAL', '全部指标正常。', '指标稳定，符合要求。', 'APPROVED', 0, 1201, '王检测', NOW() - INTERVAL 12 DAY + INTERVAL 4 HOUR, 1301, '刘审核', NOW() - INTERVAL 11 DAY);

INSERT INTO lab_detection_item (
    id, record_id, parameter_id, parameter_name, standard_min, standard_max, result_value,
    unit, reference_standard, method_id, method_name, detector_id, detector_name,
    item_status, exceed_flag, deleted, created_by, created_name, created_time,
    updated_by, updated_name, updated_time
)
VALUES
(990001, 980001, 700001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', 710001, '玻璃电极法', NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1101, '陈采样', NOW() - INTERVAL 6 DAY, 1101, '陈采样', NOW() - INTERVAL 6 DAY),
(990002, 980001, 700002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1101, '陈采样', NOW() - INTERVAL 6 DAY, 1101, '陈采样', NOW() - INTERVAL 6 DAY),
(990003, 980002, 700001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', 710001, '玻璃电极法', 1202, '赵检测', 'WAIT_DETECT', 0, 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY),
(990004, 980002, 700004, '氨氮', 0.00, 0.50, NULL, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1202, '赵检测', 'WAIT_DETECT', 0, 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY),
(990005, 980003, 700001, 'pH', 6.50, 8.50, 7.18, '', 'GB 5749-2022', 710001, '玻璃电极法', 1203, '周检测', 'SUBMITTED', 0, 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1203, '周检测', NOW() - INTERVAL 5 DAY),
(990006, 980003, 700003, '余氯', 0.05, 2.00, 0.02, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1203, '周检测', 'SUBMITTED', 1, 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1203, '周检测', NOW() - INTERVAL 5 DAY),
(990007, 980004, 700002, '浊度', 0.00, 1.00, 1.42, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1201, '王检测', 'REJECTED', 1, 0, 1201, '王检测', NOW() - INTERVAL 3 DAY, 1302, '何审核', NOW() - INTERVAL 2 DAY),
(990008, 980004, 700004, '氨氮', 0.00, 0.50, 0.86, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1201, '王检测', 'REJECTED', 1, 0, 1201, '王检测', NOW() - INTERVAL 3 DAY, 1302, '何审核', NOW() - INTERVAL 2 DAY),
(990009, 980005, 700001, 'pH', 6.50, 8.50, 7.25, '', 'GB 5749-2022', 710001, '玻璃电极法', 1201, '王检测', 'APPROVED', 0, 0, 1201, '王检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 11 DAY),
(990010, 980005, 700002, '浊度', 0.00, 1.00, 0.36, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1201, '王检测', 'APPROVED', 0, 0, 1201, '王检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 11 DAY),
(990011, 980005, 700003, '余氯', 0.05, 2.00, 0.42, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1202, '赵检测', 'APPROVED', 0, 0, 1202, '赵检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 11 DAY),
(990012, 980005, 700010, '耗氧量', 0.00, 3.00, 1.18, 'mg/L', 'GB 5749-2022', 710010, '酸性高锰酸钾滴定法', 1203, '周检测', 'APPROVED', 0, 0, 1203, '周检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 11 DAY);

-- 6. 审核记录。
INSERT INTO lab_review_record (
    id, detection_record_id, sample_id, sample_no, flow_id, flow_node_id, flow_node_name,
    flow_node_order, required_flag, reviewer_id, reviewer_name, review_time, review_result,
    reject_reason, review_remark, deleted, created_by, created_name, created_time,
    updated_by, updated_name, updated_time
)
VALUES
(800001, 980003, 970003, @sampleNo7, @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1301, '刘审核', NULL, NULL, NULL, '待审核演示记录。', 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1203, '周检测', NOW() - INTERVAL 5 DAY),
(800002, 980004, 970004, @sampleNo8, @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1302, '何审核', NOW() - INTERVAL 2 DAY, 'REJECTED', '氨氮与浊度异常，需要复检并补充现场说明。', '审核驳回，进入重检。', 0, 1302, '何审核', NOW() - INTERVAL 2 DAY, 1302, '何审核', NOW() - INTERVAL 2 DAY),
(800003, 980005, 970005, @sampleNo9, @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1301, '刘审核', NOW() - INTERVAL 11 DAY, 'APPROVED', NULL, '指标稳定，审核通过。', 0, 1301, '刘审核', NOW() - INTERVAL 11 DAY, 1301, '刘审核', NOW() - INTERVAL 11 DAY);

-- 7. 报告与推送。
INSERT INTO lab_report (
    id, report_name, report_type, generated_time, sample_id, sample_no, detection_record_id,
    report_status, published_time, published_by, published_by_name, file_path, content_snapshot,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(810001, CONCAT(@sampleNo9, '-检测报告草稿'), 'DAILY', NOW() - INTERVAL 11 DAY, 970005, @sampleNo9, 980005, 'GENERATED', NULL, NULL, NULL, 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/report/generated-sample-0009.pdf', CONCAT('样品编号：', @sampleNo9, '\n检测单位：阳新水质化验室\n检测结果：正常\n结论：本次检测符合要求。'), 0, 1301, '刘审核', NOW() - INTERVAL 11 DAY, 1301, '刘审核', NOW() - INTERVAL 11 DAY),
(810002, CONCAT(@sampleNo9, '-检测报告正式版'), 'DAILY', NOW() - INTERVAL 10 DAY, 970005, @sampleNo9, 980005, 'PUBLISHED', NOW() - INTERVAL 10 DAY, 1401, '孙报告', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/report/published-sample-0009.pdf', CONCAT('样品编号：', @sampleNo9, '\n检测单位：阳新水质化验室\n检测结果：正常\n结论：本次检测符合要求。'), 0, 1401, '孙报告', NOW() - INTERVAL 10 DAY, 1401, '孙报告', NOW() - INTERVAL 10 DAY);

INSERT INTO lab_report_push_record (
    id, report_id, sample_id, sample_no, recipient_user_id, recipient_name, recipient_phone,
    push_channel, push_status, push_message, push_time, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(820001, 810002, 970005, @sampleNo9, 1401, '孙报告', '13810004001', 'UNIFIED', 'SUCCESS', '报告已成功推送至业务系统。', NOW() - INTERVAL 10 DAY, 0, 1401, '孙报告', NOW() - INTERVAL 10 DAY, 1401, '孙报告', NOW() - INTERVAL 10 DAY),
(820002, 810001, 970005, @sampleNo9, 1401, '孙报告', '13810004001', 'UNIFIED', 'PENDING', '等待正式发布后推送。', NULL, 0, 1401, '孙报告', NOW() - INTERVAL 11 DAY, 1401, '孙报告', NOW() - INTERVAL 11 DAY);

-- 8. 仪器台账、维保、检测方法-设备绑定。
INSERT INTO lab_instrument (
    id, instrument_name, instrument_model, manufacturer, purchase_date, service_life_years,
    calibration_cycle, owner_name, instrument_status, storage_location, certificate_url, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(830001, 'pH计', 'SevenCompact', '梅特勒', CURDATE() - INTERVAL 500 DAY, 5, '6个月', '周检测', 'NORMAL', '理化室-A03', NULL, '用于 pH 原位检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 60 DAY, 1001, '系统管理员', NOW()),
(830002, '浊度仪', '2100Q', 'HACH', CURDATE() - INTERVAL 620 DAY, 6, '6个月', '赵检测', 'NORMAL', '理化室-A02', NULL, '用于浊度检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 58 DAY, 1001, '系统管理员', NOW()),
(830003, '紫外可见分光光度计', 'UV-2600i', '岛津', CURDATE() - INTERVAL 900 DAY, 8, '12个月', '王检测', 'NORMAL', '理化室-A01', NULL, '用于余氯、氨氮比色检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 55 DAY, 1001, '系统管理员', NOW()),
(830004, '色度比色仪', 'SD901', 'HACH', CURDATE() - INTERVAL 720 DAY, 6, '12个月', '周检测', 'NORMAL', '感官室-B01', NULL, '用于色度比对。', 0, 1001, '系统管理员', NOW() - INTERVAL 50 DAY, 1001, '系统管理员', NOW()),
(830005, '感官检验套装', 'ORG-SET', '实验室自备', CURDATE() - INTERVAL 200 DAY, 3, '12个月', '陈采样', 'NORMAL', '感官室-B02', NULL, '用于臭和味、肉眼可见物检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 45 DAY, 1001, '系统管理员', NOW()),
(830006, '原子吸收分光光度计', 'AA-7000', '岛津', CURDATE() - INTERVAL 1200 DAY, 10, '12个月', '王检测', 'CALIBRATING', '金属室-C01', NULL, '用于铁、锰检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 42 DAY, 1001, '系统管理员', NOW()),
(830007, '数字滴定器', 'Titrette', 'BRAND', CURDATE() - INTERVAL 360 DAY, 5, '12个月', '周检测', 'NORMAL', '理化室-A04', NULL, '用于耗氧量滴定。', 0, 1001, '系统管理员', NOW() - INTERVAL 40 DAY, 1001, '系统管理员', NOW());

DELETE FROM lab_detection_method_instrument_model_binding
WHERE id BETWEEN 831001 AND 831020
   OR method_id BETWEEN 710001 AND 710010;

INSERT INTO lab_detection_method_instrument_model_binding (
    id, method_id, method_name, instrument_model, manufacturer, instrument_count, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(831001, 710001, '玻璃电极法', 'SevenCompact', '梅特勒', 1, 'pH 方法绑定 pH 计型号。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831002, 710002, '散射光浊度法', '2100Q', 'HACH', 1, '浊度方法绑定浊度仪型号。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831003, 710003, 'DPD 分光光度法', 'UV-2600i', '岛津', 1, '余氯方法绑定分光光度计。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831004, 710004, '纳氏试剂分光光度法', 'UV-2600i', '岛津', 1, '氨氮方法绑定分光光度计。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831005, 710005, '铂钴标准比色法', 'SD901', 'HACH', 1, '色度方法绑定色度比色仪。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831006, 710006, '嗅味直接判断法', 'ORG-SET', '实验室自备', 1, '臭和味方法绑定感官检验套装。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831007, 710007, '目视观察法', 'ORG-SET', '实验室自备', 1, '肉眼可见物方法绑定感官检验套装。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831008, 710008, '原子吸收分光光度法-铁', 'AA-7000', '岛津', 1, '铁方法绑定原子吸收分光光度计。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831009, 710009, '原子吸收分光光度法-锰', 'AA-7000', '岛津', 1, '锰方法绑定原子吸收分光光度计。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831010, 710010, '酸性高锰酸钾滴定法', 'Titrette', 'BRAND', 1, '耗氧量方法绑定数字滴定器。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_instrument_maintenance (
    id, instrument_id, instrument_name, maintenance_time, maintenance_reason, maintainer_name,
    maintenance_company, maintenance_result, maintenance_cost, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(840001, 830006, '原子吸收分光光度计', NOW() - INTERVAL 2 DAY, '灯源能量波动，例行校准。', '周工', '内部校准', '校准中，待复测。', 0.00, '仪器管理演示维保记录。', 0, 1203, '周检测', NOW() - INTERVAL 2 DAY, 1203, '周检测', NOW() - INTERVAL 2 DAY),
(840002, 830002, '浊度仪', NOW() - INTERVAL 10 DAY, '季度期间核查。', '张工', '武汉精仪维保有限公司', '期间核查通过。', 600.00, '仪器管理演示期间核查记录。', 0, 1001, '系统管理员', NOW() - INTERVAL 10 DAY, 1001, '系统管理员', NOW() - INTERVAL 10 DAY);

-- 9. 文档与共享。
INSERT INTO lab_document (
    id, document_name, document_category, file_type, file_size, file_url, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(850001, '生活饮用水检测作业指导书', '操作规程', 'pdf', 286720, 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/docs/drinking-water-sop.pdf', '演示文档。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(850002, '采样编号管理制度', '管理制度', 'docx', 143360, 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/docs/sample-no-management.docx', '演示文档。', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW()),
(850003, '仪器期间核查模板', '表单模板', 'xlsx', 102400, 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/docs/instrument-check-template.xlsx', '演示文档。', 0, 1001, '系统管理员', NOW() - INTERVAL 12 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_document_share (
    id, document_id, user_id, username, real_name, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(860001, 850001, 1201, 'detector01', '王检测', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860002, 850001, 1202, 'detector02', '赵检测', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860003, 850002, 1101, 'sampler01', '陈采样', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860004, 850003, 1203, 'detector03', '周检测', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

-- 10. 登录日志。
INSERT INTO lab_login_log (
    id, user_id, username, real_name, role_code, login_channel, login_status, login_time,
    remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(870001, 1101, 'sampler01', '陈采样', 'SAMPLER', 'PC', 'SUCCESS', NOW() - INTERVAL 5 DAY, '采样员登录成功。', 0, 1101, '陈采样', NOW() - INTERVAL 5 DAY, 1101, '陈采样', NOW() - INTERVAL 5 DAY),
(870002, 1201, 'detector01', '王检测', 'DETECTOR', 'MOBILE', 'SUCCESS', NOW() - INTERVAL 4 DAY, '检测员移动端登录成功。', 0, 1201, '王检测', NOW() - INTERVAL 4 DAY, 1201, '王检测', NOW() - INTERVAL 4 DAY),
(870003, 1301, 'reviewer01', '刘审核', 'REVIEWER', 'PC', 'SUCCESS', NOW() - INTERVAL 3 DAY, '审核员登录成功。', 0, 1301, '刘审核', NOW() - INTERVAL 3 DAY, 1301, '刘审核', NOW() - INTERVAL 3 DAY),
(870004, 1401, 'reporter01', '孙报告', 'REPORTER', 'PC', 'SUCCESS', NOW() - INTERVAL 2 DAY, '报告员登录成功。', 0, 1401, '孙报告', NOW() - INTERVAL 2 DAY, 1401, '孙报告', NOW() - INTERVAL 2 DAY),
(870005, 1102, 'sampler02', '李采样', 'SAMPLER', 'PC', 'FAILED', NOW() - INTERVAL 1 DAY, '验证码错误。', 0, 1102, '李采样', NOW() - INTERVAL 1 DAY, 1102, '李采样', NOW() - INTERVAL 1 DAY);

SET FOREIGN_KEY_CHECKS = 1;

-- 执行结果核对。
SELECT 'lab_detection_parameter_category_ready' AS table_name, COUNT(*) AS total
FROM lab_detection_parameter
WHERE deleted = 0 AND parameter_category IN ('原位检测', '现场测定', '实验室测定')
UNION ALL SELECT 'lab_detection_method_sample_volume_ready', COUNT(*) FROM lab_detection_method WHERE deleted = 0 AND sample_volume IS NOT NULL AND sample_volume <> ''
UNION ALL SELECT 'lab_detection_method_instrument_model_binding', COUNT(*) FROM lab_detection_method_instrument_model_binding WHERE id BETWEEN 831001 AND 831020
UNION ALL SELECT 'lab_monitoring_point', COUNT(*) FROM lab_monitoring_point
UNION ALL SELECT 'lab_sampling_plan', COUNT(*) FROM lab_sampling_plan
UNION ALL SELECT 'lab_sampling_task', COUNT(*) FROM lab_sampling_task
UNION ALL SELECT 'lab_sample', COUNT(*) FROM lab_sample
UNION ALL SELECT 'lab_detection_record', COUNT(*) FROM lab_detection_record
UNION ALL SELECT 'lab_detection_item', COUNT(*) FROM lab_detection_item
UNION ALL SELECT 'lab_review_record', COUNT(*) FROM lab_review_record
UNION ALL SELECT 'lab_report', COUNT(*) FROM lab_report
UNION ALL SELECT 'lab_report_push_record', COUNT(*) FROM lab_report_push_record
UNION ALL SELECT 'lab_instrument', COUNT(*) FROM lab_instrument
UNION ALL SELECT 'lab_instrument_maintenance', COUNT(*) FROM lab_instrument_maintenance
UNION ALL SELECT 'lab_document', COUNT(*) FROM lab_document
UNION ALL SELECT 'lab_document_share', COUNT(*) FROM lab_document_share
UNION ALL SELECT 'lab_login_log', COUNT(*) FROM lab_login_log;
