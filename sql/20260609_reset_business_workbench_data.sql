-- 清理非系统管理数据，并按当前业务流程重建工作台演示数据。
-- 注意：本脚本会清空业务表、检测配置表、资产/文档业务台账表。
-- 保留：角色、用户、组织、字典、流程配置、流程节点、报告模板等系统管理数据。
-- 执行前请先备份数据库；确认数据库后再执行。如需指定库，可取消下一行注释。
-- USE yx_lab;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
    apply_scope VARCHAR(1000),
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
) COMMENT='样品编号序列表';

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

-- 1. 清理业务链路与业务配置。
TRUNCATE TABLE lab_report_push_record;
TRUNCATE TABLE lab_report;
TRUNCATE TABLE lab_review_record;
TRUNCATE TABLE lab_detection_item;
TRUNCATE TABLE lab_detection_record;
TRUNCATE TABLE lab_sample;
TRUNCATE TABLE lab_sampling_task;
TRUNCATE TABLE lab_sampling_plan;
TRUNCATE TABLE lab_monitoring_point;

TRUNCATE TABLE lab_detection_method_instrument_model_binding;
TRUNCATE TABLE lab_detection_step;
TRUNCATE TABLE lab_detection_type;
TRUNCATE TABLE lab_detection_method;
TRUNCATE TABLE lab_detection_parameter;
TRUNCATE TABLE lab_detection_project_group;

TRUNCATE TABLE lab_instrument_maintenance;
TRUNCATE TABLE lab_instrument;
TRUNCATE TABLE lab_document_share;
TRUNCATE TABLE lab_document;
TRUNCATE TABLE lab_sample_no_sequence;

-- 2. 检测配置：参数、方法、套餐、步骤。
INSERT INTO lab_detection_project_group (
    id, group_name, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(705001, '常规理化指标', 1, '饮用水日常检测基础指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705002, '消毒与感官指标', 1, '余氯、色度、臭和味等现场关注指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705003, '原水风险指标', 1, '原水氨氮、耗氧量等风险指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_detection_parameter (
    id, parameter_name, standard_min, standard_max, unit, exceed_rule, reference_standard,
    enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(700001, 'pH', 6.50, 8.50, '', 'OUT_OF_RANGE', 'GB 5749-2022', 1, '酸碱度', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700002, '浊度', 0.00, 1.00, 'NTU', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '水体浊度', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700003, '余氯', 0.05, 2.00, 'mg/L', 'OUT_OF_RANGE', 'GB 5749-2022', 1, '消毒剂余量', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700004, '氨氮', 0.00, 0.50, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '原水风险指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700005, '色度', 0.00, 15.00, '度', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700006, '肉眼可见物', 0.00, 0.00, '项', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700007, '耗氧量', 0.00, 3.00, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '有机污染综合指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_detection_method (
    id, method_name, method_code, parameter_id, parameter_name, standard_code, sample_volume,
    method_basis, apply_scope, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(710001, '玻璃电极法', 'YX-PH-01', 700001, 'pH', 'GB/T 5750.4', '100mL', '校准 pH 计后直接测定稳定读数。', '出厂水、原水、管网水 pH 检测', 1, '常规方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710002, '散射光浊度法', 'YX-TURB-01', 700002, '浊度', 'GB/T 5750.4', '50mL', '混匀样品后使用浊度仪读取 NTU 值。', '生活饮用水浊度检测', 1, '常规方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710003, 'DPD 分光光度法', 'YX-CL-01', 700003, '余氯', 'GB/T 5750.11', '10mL', '加入 DPD 试剂显色后测定。', '出厂水和管网水余氯检测', 1, '常规方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710004, '纳氏试剂分光光度法', 'YX-NH3N-01', 700004, '氨氮', 'HJ 535', '50mL', '预处理后加入纳氏试剂显色测定。', '原水氨氮检测', 1, '常规方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710005, '铂钴标准比色法', 'YX-COLOR-01', 700005, '色度', 'GB/T 5750.4', '50mL', '与铂钴标准色列比对。', '感官指标检测', 1, '常规方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710006, '目视观察法', 'YX-VISIBLE-01', 700006, '肉眼可见物', 'GB/T 5750.4', '250mL', '自然光下目视观察样品。', '感官指标检测', 1, '常规方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710007, '酸性高锰酸钾滴定法', 'YX-CODMN-01', 700007, '耗氧量', 'GB/T 5750.7', '100mL', '酸性条件下氧化并滴定计算耗氧量。', '有机污染综合检测', 1, '常规方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

SET @snapshot_factory = '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","sampleVolume":"100mL","methodBasis":"校准 pH 计后直接测定稳定读数。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","sampleVolume":"50mL","methodBasis":"混匀样品后使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD 分光光度法","sampleVolume":"10mL","methodBasis":"加入 DPD 试剂显色后测定。"},{"parameterId":700005,"parameterName":"色度","unit":"度","standardMin":0.00,"standardMax":15.00,"referenceStandard":"GB 5749-2022","methodId":710005,"methodName":"铂钴标准比色法","sampleVolume":"50mL","methodBasis":"与铂钴标准色列比对。"}]';
SET @snapshot_terminal = '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","sampleVolume":"100mL","methodBasis":"校准 pH 计后直接测定稳定读数。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","sampleVolume":"50mL","methodBasis":"混匀样品后使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD 分光光度法","sampleVolume":"10mL","methodBasis":"加入 DPD 试剂显色后测定。"},{"parameterId":700006,"parameterName":"肉眼可见物","unit":"项","standardMin":0.00,"standardMax":0.00,"referenceStandard":"GB 5749-2022","methodId":710006,"methodName":"目视观察法","sampleVolume":"250mL","methodBasis":"自然光下目视观察样品。"}]';
SET @snapshot_raw = '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","sampleVolume":"100mL","methodBasis":"校准 pH 计后直接测定稳定读数。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","sampleVolume":"50mL","methodBasis":"混匀样品后使用浊度仪读取 NTU 值。"},{"parameterId":700004,"parameterName":"氨氮","unit":"mg/L","standardMin":0.00,"standardMax":0.50,"referenceStandard":"GB 5749-2022","methodId":710004,"methodName":"纳氏试剂分光光度法","sampleVolume":"50mL","methodBasis":"预处理后加入纳氏试剂显色测定。"},{"parameterId":700007,"parameterName":"耗氧量","unit":"mg/L","standardMin":0.00,"standardMax":3.00,"referenceStandard":"GB 5749-2022","methodId":710007,"methodName":"酸性高锰酸钾滴定法","sampleVolume":"100mL","methodBasis":"酸性条件下氧化并滴定计算耗氧量。"}]';

INSERT INTO lab_detection_type (
    id, type_name, group_id, group_name, detector_id, detector_name, sample_type,
    parameter_ids, parameter_names, parameter_method_bindings, enabled, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(720001, '出厂水常规四项', 705001, '常规理化指标', 1002, '员工', 'FACTORY', '700001,700002,700003,700005', 'pH,浊度,余氯,色度', '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700003,"methodIds":[710003]},{"parameterId":700005,"methodIds":[710005]}]', 1, '出厂水样品默认套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720002, '管网末梢四项', 705002, '消毒与感官指标', 1002, '员工', 'TERMINAL', '700001,700002,700003,700006', 'pH,浊度,余氯,肉眼可见物', '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700003,"methodIds":[710003]},{"parameterId":700006,"methodIds":[710006]}]', 1, '管网末梢样品默认套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720003, '原水风险四项', 705003, '原水风险指标', 1002, '员工', 'RAW', '700001,700002,700004,700007', 'pH,浊度,氨氮,耗氧量', '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700004,"methodIds":[710004]},{"parameterId":700007,"methodIds":[710007]}]', 1, '原水样品默认套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_detection_step (
    id, type_id, type_name, step_name, step_order, step_description, reagent_requirement,
    operation_requirement, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(730001, 720001, '出厂水常规四项', '样品核验', 1, '核对样品编号、点位和保存条件。', '无', '确认样品外观正常后开始检测。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730002, 720001, '出厂水常规四项', '理化检测', 2, '完成 pH、浊度、余氯、色度检测。', '标准缓冲液、DPD 试剂', '按检测方法逐项录入结果。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730003, 720002, '管网末梢四项', '末梢水检测', 1, '完成 pH、浊度、余氯、肉眼可见物检测。', 'DPD 试剂', '余氯需及时检测并记录。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730004, 720003, '原水风险四项', '原水重点指标检测', 1, '完成原水 pH、浊度、氨氮、耗氧量检测。', '纳氏试剂、滴定标准液', '异常指标需复核确认。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

-- 3. 监测点、采样计划与采样任务。
INSERT INTO lab_monitoring_point (
    id, point_name, address, longitude, latitude, region_name, point_type, point_status,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(740001, '城东水厂出水口', '阳新县城东水厂出水口', '115.220100', '30.215600', '城东水厂', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(740002, '城西水厂出水口', '阳新县城西水厂出水口', '115.181500', '30.226800', '城西水厂', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(740003, '兴国大道末梢点', '兴国大道末梢供水点', '115.205800', '30.217700', '城东水厂', 'TERMINAL', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 19 DAY, 1001, '系统管理员', NOW()),
(740004, '莲花湖取水口', '莲花湖原水取水口', '115.155200', '30.190300', '城西水厂', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW()),
(740005, '富池镇末梢点', '富池镇居民供水末梢点', '115.312900', '30.112600', '城东水厂', 'TERMINAL', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 17 DAY, 1001, '系统管理员', NOW()),
(740006, '王英水库取水口', '王英水库原水取水口', '114.982400', '29.885900', '城西水厂', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 16 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_sampling_plan (
    id, plan_name, point_id, point_name, address, latitude, longitude, start_time, end_time,
    sampler_id, sampler_ids, sampler_name, sampling_type, sample_type, detection_type_id,
    detection_type_name, detection_config_snapshot, cycle_type, plan_status, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(750001, '城东出厂水每日采样计划', 740001, '城东水厂出水口', '阳新县城东水厂出水口', '30.215600', '115.220100', CURDATE(), CURDATE() + INTERVAL 30 DAY, 1002, ',1002,', '员工', 'ROUTINE', 'FACTORY', 720001, '出厂水常规四项', @snapshot_factory, 'DAILY', 'ACTIVE', '工作台采样计划演示数据', 0, 1001, '系统管理员', NOW() - INTERVAL 8 DAY, 1001, '系统管理员', NOW()),
(750002, '兴国大道末梢水周采样计划', 740003, '兴国大道末梢点', '兴国大道末梢供水点', '30.217700', '115.205800', CURDATE(), CURDATE() + INTERVAL 30 DAY, 1002, ',1002,', '员工', 'ROUTINE', 'TERMINAL', 720002, '管网末梢四项', @snapshot_terminal, 'WEEKLY', 'ACTIVE', '工作台采样计划演示数据', 0, 1001, '系统管理员', NOW() - INTERVAL 7 DAY, 1001, '系统管理员', NOW()),
(750003, '莲花湖原水月采样计划', 740004, '莲花湖取水口', '莲花湖原水取水口', '30.190300', '115.155200', CURDATE(), CURDATE() + INTERVAL 60 DAY, 1002, ',1002,', '员工', 'ROUTINE', 'RAW', 720003, '原水风险四项', @snapshot_raw, 'MONTHLY', 'ACTIVE', '工作台采样计划演示数据', 0, 1001, '系统管理员', NOW() - INTERVAL 6 DAY, 1001, '系统管理员', NOW()),
(750004, '城西出厂水临时采样计划', 740002, '城西水厂出水口', '阳新县城西水厂出水口', '30.226800', '115.181500', CURDATE() - INTERVAL 2 DAY, CURDATE() + INTERVAL 5 DAY, 1002, ',1002,', '员工', 'ROUTINE', 'FACTORY', 720001, '出厂水常规四项', @snapshot_factory, 'ONCE', 'ACTIVE', '临时补充采样', 0, 1001, '系统管理员', NOW() - INTERVAL 5 DAY, 1001, '系统管理员', NOW()),
(750005, '富池镇末梢水巡检计划', 740005, '富池镇末梢点', '富池镇居民供水末梢点', '30.112600', '115.312900', CURDATE() - INTERVAL 1 DAY, CURDATE() + INTERVAL 30 DAY, 1002, ',1002,', '员工', 'ROUTINE', 'TERMINAL', 720002, '管网末梢四项', @snapshot_terminal, 'WEEKLY', 'ACTIVE', '工作台采样计划演示数据', 0, 1001, '系统管理员', NOW() - INTERVAL 4 DAY, 1001, '系统管理员', NOW()),
(750006, '王英水库原水巡检计划', 740006, '王英水库取水口', '王英水库原水取水口', '29.885900', '114.982400', CURDATE() - INTERVAL 1 DAY, CURDATE() + INTERVAL 30 DAY, 1002, ',1002,', '员工', 'ROUTINE', 'RAW', 720003, '原水风险四项', @snapshot_raw, 'MONTHLY', 'ACTIVE', '工作台采样计划演示数据', 0, 1001, '系统管理员', NOW() - INTERVAL 3 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_sampling_task (
    id, task_no, sample_no, plan_id, point_id, point_name, sampling_time, sampler_id, sampler_ids,
    sampler_name, sample_type, sample_register_status, sample_id, detection_items, detection_type_id,
    detection_type_name, detection_config_snapshot, task_status, started_time, onsite_metrics,
    weather, temperature, sample_total_volume, sample_bottle_count, photo_urls, abandon_reason,
    finished_time, remark, address, latitude, longitude, deleted, created_by, created_name,
    created_time, updated_by, updated_name, updated_time
)
VALUES
(760001, 'TASK-20260609-001', NULL, 750001, 740001, '城东水厂出水口', NOW() + INTERVAL 2 HOUR, 1002, ',1002,', '员工', 'FACTORY', 'UNREGISTERED', NULL, 'pH,浊度,余氯,色度', 720001, '出厂水常规四项', @snapshot_factory, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '待执行采样任务', '阳新县城东水厂出水口', '30.215600', '115.220100', 0, 1001, '系统管理员', NOW() - INTERVAL 2 DAY, 1001, '系统管理员', NOW()),
(760002, 'TASK-20260609-002', NULL, 750002, 740003, '兴国大道末梢点', NOW() + INTERVAL 3 HOUR, 1002, ',1002,', '员工', 'TERMINAL', 'UNREGISTERED', NULL, 'pH,浊度,余氯,肉眼可见物', 720002, '管网末梢四项', @snapshot_terminal, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '待执行采样任务', '兴国大道末梢供水点', '30.217700', '115.205800', 0, 1001, '系统管理员', NOW() - INTERVAL 2 DAY, 1001, '系统管理员', NOW()),
(760003, 'TASK-20260609-003', NULL, 750003, 740004, '莲花湖取水口', NOW() - INTERVAL 1 HOUR, 1002, ',1002,', '员工', 'RAW', 'UNREGISTERED', NULL, 'pH,浊度,氨氮,耗氧量', 720003, '原水风险四项', @snapshot_raw, 'IN_PROGRESS', NOW() - INTERVAL 1 HOUR, '已到达点位，正在采样。', '晴', '26℃', NULL, NULL, NULL, NULL, NULL, '执行中采样任务', '莲花湖原水取水口', '30.190300', '115.155200', 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(760004, 'TASK-20260609-004', 'YX202606090001', 750004, 740002, '城西水厂出水口', NOW() - INTERVAL 5 HOUR, 1002, ',1002,', '员工', 'FACTORY', 'UNREGISTERED', NULL, 'pH,浊度,余氯,色度', 720001, '出厂水常规四项', @snapshot_factory, 'COMPLETED', NOW() - INTERVAL 6 HOUR, '现场外观正常。', '多云', '25℃', '500mL', '2', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/task-004.jpg', NULL, NOW() - INTERVAL 5 HOUR, '已采样，等待样品登录', '阳新县城西水厂出水口', '30.226800', '115.181500', 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(760005, 'TASK-20260609-005', 'YX202606090002', 750001, 740001, '城东水厂出水口', NOW() - INTERVAL 1 DAY, 1002, ',1002,', '员工', 'FACTORY', 'REGISTERED', 770001, 'pH,浊度,余氯,色度', 720001, '出厂水常规四项', @snapshot_factory, 'COMPLETED', NOW() - INTERVAL 1 DAY - INTERVAL 1 HOUR, '现场外观正常。', '晴', '24℃', '500mL', '2', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/task-005.jpg', NULL, NOW() - INTERVAL 1 DAY, '样品已登录，待检测录入', '阳新县城东水厂出水口', '30.215600', '115.220100', 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(760006, 'TASK-20260609-006', 'YX202606090003', 750002, 740003, '兴国大道末梢点', NOW() - INTERVAL 2 DAY, 1002, ',1002,', '员工', 'TERMINAL', 'REGISTERED', 770002, 'pH,浊度,余氯,肉眼可见物', 720002, '管网末梢四项', @snapshot_terminal, 'COMPLETED', NOW() - INTERVAL 2 DAY - INTERVAL 1 HOUR, '末梢水压正常。', '阴', '23℃', '500mL', '2', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/task-006.jpg', NULL, NOW() - INTERVAL 2 DAY, '样品已提交审核', '兴国大道末梢供水点', '30.217700', '115.205800', 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(760007, 'TASK-20260609-007', 'YX202606090004', 750003, 740004, '莲花湖取水口', NOW() - INTERVAL 3 DAY, 1002, ',1002,', '员工', 'RAW', 'REGISTERED', 770003, 'pH,浊度,氨氮,耗氧量', 720003, '原水风险四项', @snapshot_raw, 'COMPLETED', NOW() - INTERVAL 3 DAY - INTERVAL 1 HOUR, '水体轻微浑浊。', '小雨', '22℃', '1000mL', '3', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/task-007.jpg', NULL, NOW() - INTERVAL 3 DAY, '异常结果待审核', '莲花湖原水取水口', '30.190300', '115.155200', 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(760008, 'TASK-20260609-008', 'YX202606090005', 750004, 740002, '城西水厂出水口', NOW() - INTERVAL 4 DAY, 1002, ',1002,', '员工', 'FACTORY', 'REGISTERED', 770004, 'pH,浊度,余氯,色度', 720001, '出厂水常规四项', @snapshot_factory, 'COMPLETED', NOW() - INTERVAL 4 DAY - INTERVAL 1 HOUR, '现场外观正常。', '晴', '24℃', '500mL', '2', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/task-008.jpg', NULL, NOW() - INTERVAL 4 DAY, '审核通过，报告待发布', '阳新县城西水厂出水口', '30.226800', '115.181500', 0, 1002, '员工', NOW() - INTERVAL 4 DAY, 1002, '员工', NOW()),
(760009, 'TASK-20260609-009', 'YX202606090006', 750005, 740005, '富池镇末梢点', NOW() - INTERVAL 5 DAY, 1002, ',1002,', '员工', 'TERMINAL', 'REGISTERED', 770005, 'pH,浊度,余氯,肉眼可见物', 720002, '管网末梢四项', @snapshot_terminal, 'COMPLETED', NOW() - INTERVAL 5 DAY - INTERVAL 1 HOUR, '现场外观正常。', '晴', '25℃', '500mL', '2', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/task-009.jpg', NULL, NOW() - INTERVAL 5 DAY, '报告已生成', '富池镇居民供水末梢点', '30.112600', '115.312900', 0, 1002, '员工', NOW() - INTERVAL 5 DAY, 1002, '员工', NOW()),
(760010, 'TASK-20260609-010', 'YX202606090007', 750006, 740006, '王英水库取水口', NOW() - INTERVAL 6 DAY, 1002, ',1002,', '员工', 'RAW', 'REGISTERED', 770006, 'pH,浊度,氨氮,耗氧量', 720003, '原水风险四项', @snapshot_raw, 'COMPLETED', NOW() - INTERVAL 6 DAY - INTERVAL 1 HOUR, '水体外观正常。', '晴', '24℃', '1000mL', '3', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/sampling/task-010.jpg', NULL, NOW() - INTERVAL 6 DAY, '报告已发布', '王英水库原水取水口', '29.885900', '114.982400', 0, 1002, '员工', NOW() - INTERVAL 6 DAY, 1002, '员工', NOW());

INSERT INTO lab_sample_no_sequence(sequence_date, current_value)
VALUES (DATE_FORMAT(CURDATE(), '%Y%m%d'), 20);

-- 4. 样品台账、检测主流程与子流程。
SET @reviewFlowId = COALESCE((SELECT id FROM lab_flow_config WHERE deleted = 0 AND flow_type = 'REVIEW' AND status = 1 ORDER BY default_flag DESC, id LIMIT 1), 9601);
SET @reviewFlowName = COALESCE((SELECT flow_name FROM lab_flow_config WHERE id = @reviewFlowId LIMIT 1), '默认审核流程');
SET @reviewNodeId = COALESCE((SELECT id FROM lab_flow_node WHERE deleted = 0 AND flow_id = @reviewFlowId ORDER BY node_order LIMIT 1), 9611);
SET @reviewNodeName = COALESCE((SELECT node_name FROM lab_flow_node WHERE id = @reviewNodeId LIMIT 1), '主任审核');
SET @reviewNodeOrder = COALESCE((SELECT node_order FROM lab_flow_node WHERE id = @reviewNodeId LIMIT 1), 1);
SET @reviewRequiredFlag = COALESCE((SELECT required_flag FROM lab_flow_node WHERE id = @reviewNodeId LIMIT 1), 1);

INSERT INTO lab_sample (
    id, sample_no, task_id, point_id, point_name, sample_type, sample_source_method,
    detection_items, detection_type_id, detection_type_name, detection_config_snapshot,
    review_flow_id, review_flow_name, sampling_time, sample_total_volume, sample_bottle_count,
    sampler_id, sampler_name, weather, storage_condition, sample_status, result_summary,
    remark, trace_log, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(770001, 'YX202606090002', 760005, 740001, '城东水厂出水口', 'FACTORY', 'SAMPLING', 'pH,浊度,余氯,色度', 720001, '出厂水常规四项', @snapshot_factory, @reviewFlowId, @reviewFlowName, NOW() - INTERVAL 1 DAY, '500mL', '2', 1002, '员工', '晴', '冷藏', 'LOGGED', NULL, '待检测录入样品', '样品登录后自动生成检测流程。', 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(770002, 'YX202606090003', 760006, 740003, '兴国大道末梢点', 'TERMINAL', 'SAMPLING', 'pH,浊度,余氯,肉眼可见物', 720002, '管网末梢四项', @snapshot_terminal, @reviewFlowId, @reviewFlowName, NOW() - INTERVAL 2 DAY, '500mL', '2', 1002, '员工', '阴', '冷藏', 'REVIEWING', '检测结果已提交，等待审核。', '待审核样品', '检测已提交。', 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(770003, 'YX202606090004', 760007, 740004, '莲花湖取水口', 'RAW', 'SAMPLING', 'pH,浊度,氨氮,耗氧量', 720003, '原水风险四项', @snapshot_raw, @reviewFlowId, @reviewFlowName, NOW() - INTERVAL 3 DAY, '1000mL', '3', 1002, '员工', '小雨', '冷藏', 'REVIEWING', '氨氮偏高，待审核确认。', '异常结果待审核样品', '检测已提交。', 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(770004, 'YX202606090005', 760008, 740002, '城西水厂出水口', 'FACTORY', 'SAMPLING', 'pH,浊度,余氯,色度', 720001, '出厂水常规四项', @snapshot_factory, @reviewFlowId, @reviewFlowName, NOW() - INTERVAL 4 DAY, '500mL', '2', 1002, '员工', '晴', '冷藏', 'COMPLETED', '检测结果正常，审核通过。', '报告草稿待发布样品', '审核通过并生成报告草稿。', 0, 1002, '员工', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(770005, 'YX202606090006', 760009, 740005, '富池镇末梢点', 'TERMINAL', 'SAMPLING', 'pH,浊度,余氯,肉眼可见物', 720002, '管网末梢四项', @snapshot_terminal, @reviewFlowId, @reviewFlowName, NOW() - INTERVAL 5 DAY, '500mL', '2', 1002, '员工', '晴', '冷藏', 'COMPLETED', '检测结果正常，报告已生成。', '报告待发布样品', '审核通过并生成报告。', 0, 1002, '员工', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(770006, 'YX202606090007', 760010, 740006, '王英水库取水口', 'RAW', 'SAMPLING', 'pH,浊度,氨氮,耗氧量', 720003, '原水风险四项', @snapshot_raw, @reviewFlowId, @reviewFlowName, NOW() - INTERVAL 6 DAY, '1000mL', '3', 1002, '员工', '晴', '冷藏', 'COMPLETED', '检测结果正常，报告已发布。', '已发布报告样品', '全流程完成。', 0, 1002, '员工', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW());

INSERT INTO lab_detection_record (
    id, sample_id, sample_no, detection_type_id, detection_type_name, detection_time,
    detector_id, detector_name, detection_result, abnormal_remark, remark, detection_status,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(780001, 770001, 'YX202606090002', 720001, '出厂水常规四项', NOW() - INTERVAL 22 HOUR, 1002, '员工', NULL, NULL, '待录入检测结果。', 'WAIT_DETECT', 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(780002, 770002, 'YX202606090003', 720002, '管网末梢四项', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR, 1002, '员工', 'NORMAL', NULL, '检测结果正常，等待审核。', 'SUBMITTED', 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(780003, 770003, 'YX202606090004', 720003, '原水风险四项', NOW() - INTERVAL 3 DAY + INTERVAL 3 HOUR, 1002, '员工', 'ABNORMAL', '氨氮接近控制线，请审核确认。', '异常结果待审核。', 'SUBMITTED', 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(780004, 770004, 'YX202606090005', 720001, '出厂水常规四项', NOW() - INTERVAL 4 DAY + INTERVAL 3 HOUR, 1002, '员工', 'NORMAL', NULL, '审核通过，报告草稿待发布。', 'APPROVED', 0, 1002, '员工', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(780005, 770005, 'YX202606090006', 720002, '管网末梢四项', NOW() - INTERVAL 5 DAY + INTERVAL 3 HOUR, 1002, '员工', 'NORMAL', NULL, '审核通过，报告已生成。', 'APPROVED', 0, 1002, '员工', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(780006, 770006, 'YX202606090007', 720003, '原水风险四项', NOW() - INTERVAL 6 DAY + INTERVAL 3 HOUR, 1002, '员工', 'NORMAL', NULL, '审核通过，报告已发布。', 'APPROVED', 0, 1002, '员工', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW());

INSERT INTO lab_detection_item (
    id, record_id, parameter_id, parameter_name, standard_min, standard_max, result_value,
    unit, reference_standard, method_id, method_name, detector_id, detector_name, item_status,
    exceed_flag, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(790001, 780001, 700001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', 710001, '玻璃电极法', 1002, '员工', 'WAIT_DETECT', 0, 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(790002, 780001, 700002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1002, '员工', 'WAIT_DETECT', 0, 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(790003, 780001, 700003, '余氯', 0.05, 2.00, NULL, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1002, '员工', 'WAIT_DETECT', 0, 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(790004, 780001, 700005, '色度', 0.00, 15.00, NULL, '度', 'GB 5749-2022', 710005, '铂钴标准比色法', 1002, '员工', 'WAIT_DETECT', 0, 0, 1002, '员工', NOW() - INTERVAL 1 DAY, 1002, '员工', NOW()),
(790005, 780002, 700001, 'pH', 6.50, 8.50, 7.18, '', 'GB 5749-2022', 710001, '玻璃电极法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(790006, 780002, 700002, '浊度', 0.00, 1.00, 0.38, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(790007, 780002, 700003, '余氯', 0.05, 2.00, 0.36, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(790008, 780002, 700006, '肉眼可见物', 0.00, 0.00, 0.00, '项', 'GB 5749-2022', 710006, '目视观察法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(790009, 780003, 700001, 'pH', 6.50, 8.50, 7.62, '', 'GB 5749-2022', 710001, '玻璃电极法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(790010, 780003, 700002, '浊度', 0.00, 1.00, 0.92, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(790011, 780003, 700004, '氨氮', 0.00, 0.50, 0.48, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(790012, 780003, 700007, '耗氧量', 0.00, 3.00, 2.65, 'mg/L', 'GB 5749-2022', 710007, '酸性高锰酸钾滴定法', 1002, '员工', 'SUBMITTED', 0, 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(790013, 780004, 700001, 'pH', 6.50, 8.50, 7.24, '', 'GB 5749-2022', 710001, '玻璃电极法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(790014, 780004, 700002, '浊度', 0.00, 1.00, 0.33, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(790015, 780004, 700003, '余氯', 0.05, 2.00, 0.42, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(790016, 780004, 700005, '色度', 0.00, 15.00, 5.00, '度', 'GB 5749-2022', 710005, '铂钴标准比色法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(790017, 780005, 700001, 'pH', 6.50, 8.50, 7.20, '', 'GB 5749-2022', 710001, '玻璃电极法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(790018, 780005, 700002, '浊度', 0.00, 1.00, 0.41, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(790019, 780005, 700003, '余氯', 0.05, 2.00, 0.31, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(790020, 780005, 700006, '肉眼可见物', 0.00, 0.00, 0.00, '项', 'GB 5749-2022', 710006, '目视观察法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(790021, 780006, 700001, 'pH', 6.50, 8.50, 7.54, '', 'GB 5749-2022', 710001, '玻璃电极法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW()),
(790022, 780006, 700002, '浊度', 0.00, 1.00, 0.70, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW()),
(790023, 780006, 700004, '氨氮', 0.00, 0.50, 0.18, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW()),
(790024, 780006, 700007, '耗氧量', 0.00, 3.00, 1.68, 'mg/L', 'GB 5749-2022', 710007, '酸性高锰酸钾滴定法', 1002, '员工', 'APPROVED', 0, 0, 1002, '员工', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW());

-- 5. 审核与报告。
INSERT INTO lab_review_record (
    id, detection_record_id, sample_id, sample_no, flow_id, flow_node_id, flow_node_name,
    flow_node_order, required_flag, reviewer_id, reviewer_name, review_time, review_result,
    reject_reason, review_remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(800001, 780002, 770002, 'YX202606090003', @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1003, '主任', NULL, NULL, NULL, '待审核演示记录。', 0, 1002, '员工', NOW() - INTERVAL 2 DAY, 1002, '员工', NOW()),
(800002, 780003, 770003, 'YX202606090004', @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1003, '主任', NULL, NULL, NULL, '异常结果待审核确认。', 0, 1002, '员工', NOW() - INTERVAL 3 DAY, 1002, '员工', NOW()),
(800003, 780004, 770004, 'YX202606090005', @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1003, '主任', NOW() - INTERVAL 4 DAY + INTERVAL 5 HOUR, 'APPROVED', NULL, '审核通过。', 0, 1003, '主任', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(800004, 780005, 770005, 'YX202606090006', @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1003, '主任', NOW() - INTERVAL 5 DAY + INTERVAL 5 HOUR, 'APPROVED', NULL, '审核通过。', 0, 1003, '主任', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(800005, 780006, 770006, 'YX202606090007', @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1003, '主任', NOW() - INTERVAL 6 DAY + INTERVAL 5 HOUR, 'APPROVED', NULL, '审核通过。', 0, 1003, '主任', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW());

INSERT INTO lab_report (
    id, report_name, report_type, report_category, generated_time, sample_id, sample_no,
    detection_record_id, report_status, published_time, published_by, published_by_name,
    file_path, content_snapshot, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(810001, 'YX202606090005 检测报告草稿', 'DAILY', 'DETECTION_REPORT', NOW() - INTERVAL 4 DAY + INTERVAL 6 HOUR, 770004, 'YX202606090005', 780004, 'DRAFT', NULL, NULL, NULL, NULL, '样品编号：YX202606090005\n结论：检测结果正常，待发布。', 0, 1003, '主任', NOW() - INTERVAL 4 DAY, 1003, '主任', NOW()),
(810002, 'YX202606090006 检测报告', 'DAILY', 'DETECTION_REPORT', NOW() - INTERVAL 5 DAY + INTERVAL 6 HOUR, 770005, 'YX202606090006', 780005, 'GENERATED', NULL, NULL, NULL, 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/report/YX202606090006.pdf', '样品编号：YX202606090006\n结论：检测结果正常，待发布。', 0, 1003, '主任', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW()),
(810003, 'YX202606090007 检测报告', 'DAILY', 'DETECTION_REPORT', NOW() - INTERVAL 6 DAY + INTERVAL 6 HOUR, 770006, 'YX202606090007', 780006, 'PUBLISHED', NOW() - INTERVAL 6 DAY + INTERVAL 7 HOUR, 1003, '主任', 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/report/YX202606090007.pdf', '样品编号：YX202606090007\n结论：检测结果正常，已发布。', 0, 1003, '主任', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW());

INSERT INTO lab_report_push_record (
    id, report_id, sample_id, sample_no, recipient_user_id, recipient_name, recipient_phone,
    push_channel, push_status, push_message, push_time, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(820001, 810003, 770006, 'YX202606090007', 1003, '主任', '13800000002', 'UNIFIED', 'SUCCESS', '报告已推送。', NOW() - INTERVAL 6 DAY + INTERVAL 7 HOUR, 0, 1003, '主任', NOW() - INTERVAL 6 DAY, 1003, '主任', NOW()),
(820002, 810002, 770005, 'YX202606090006', 1003, '主任', '13800000002', 'UNIFIED', 'PENDING', '等待发布后推送。', NULL, 0, 1003, '主任', NOW() - INTERVAL 5 DAY, 1003, '主任', NOW());

-- 6. 资产与文档业务台账。
INSERT INTO lab_instrument (
    id, instrument_name, instrument_model, manufacturer, purchase_date, service_life_years,
    calibration_cycle, owner_name, instrument_status, storage_location, certificate_url, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(830001, 'pH 计', 'SevenCompact', 'Mettler Toledo', CURDATE() - INTERVAL 500 DAY, 5, '6个月', '员工', 'NORMAL', '理化室 A03', NULL, '用于 pH 检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(830002, '浊度仪', '2100Q', 'HACH', CURDATE() - INTERVAL 620 DAY, 6, '6个月', '员工', 'NORMAL', '理化室 A02', NULL, '用于浊度检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(830003, '紫外可见分光光度计', 'UV-2600i', 'Shimadzu', CURDATE() - INTERVAL 900 DAY, 8, '12个月', '员工', 'NORMAL', '理化室 A01', NULL, '用于余氯、氨氮检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(830004, '数字滴定器', 'Titrette', 'BRAND', CURDATE() - INTERVAL 360 DAY, 5, '12个月', '员工', 'CALIBRATING', '理化室 A04', NULL, '用于耗氧量滴定。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_detection_method_instrument_model_binding (
    id, method_id, method_name, instrument_model, manufacturer, instrument_count, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(831001, 710001, '玻璃电极法', 'SevenCompact', 'Mettler Toledo', 1, 'pH 方法设备绑定。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831002, 710002, '散射光浊度法', '2100Q', 'HACH', 1, '浊度方法设备绑定。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831003, 710003, 'DPD 分光光度法', 'UV-2600i', 'Shimadzu', 1, '余氯方法设备绑定。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831004, 710004, '纳氏试剂分光光度法', 'UV-2600i', 'Shimadzu', 1, '氨氮方法设备绑定。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(831005, 710007, '酸性高锰酸钾滴定法', 'Titrette', 'BRAND', 1, '耗氧量方法设备绑定。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_instrument_maintenance (
    id, instrument_id, instrument_name, maintenance_time, maintenance_reason, maintainer_name,
    maintenance_company, maintenance_result, maintenance_cost, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(840001, 830004, '数字滴定器', NOW() - INTERVAL 2 DAY, '例行校准', '张工', '内部校准', '校准中', 0.00, '设备台账演示记录。', 0, 1001, '系统管理员', NOW() - INTERVAL 2 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_document (
    id, document_name, document_category, file_type, file_size, file_url, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(850001, '生活饮用水检测作业指导书', '操作规程', 'pdf', 286720, 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/docs/drinking-water-sop.pdf', '业务文档演示数据。', 0, 1001, '系统管理员', NOW() - INTERVAL 10 DAY, 1001, '系统管理员', NOW()),
(850002, '采样编号管理制度', '管理制度', 'docx', 143360, 'https://yangxin.yunhexx.com:8443/api/storage/file?path=demo/docs/sample-no-management.docx', '业务文档演示数据。', 0, 1001, '系统管理员', NOW() - INTERVAL 9 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_document_share (
    id, document_id, user_id, username, real_name, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(860001, 850001, 1002, 'staff', '员工', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860002, 850002, 1002, 'staff', '员工', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- 7. 工作台可见性校验：以下 6 项都应大于 0。
SELECT '工作台-采样计划' AS check_item, COUNT(*) AS total
FROM lab_sampling_plan
WHERE deleted = 0 AND plan_status = 'ACTIVE'
UNION ALL
SELECT '工作台-采样任务', COUNT(*)
FROM lab_sampling_task
WHERE deleted = 0 AND task_status IN ('PENDING', 'IN_PROGRESS')
UNION ALL
SELECT '工作台-样品登录', COUNT(*)
FROM lab_sampling_task
WHERE deleted = 0 AND task_status = 'COMPLETED' AND sample_register_status = 'UNREGISTERED' AND sample_id IS NULL
UNION ALL
SELECT '工作台-检测录入', COUNT(*)
FROM lab_detection_item
WHERE deleted = 0 AND item_status = 'WAIT_DETECT'
UNION ALL
SELECT '工作台-结果审核', COUNT(*)
FROM lab_detection_record
WHERE deleted = 0 AND detection_status = 'SUBMITTED'
UNION ALL
SELECT '工作台-报告处理', COUNT(*)
FROM lab_report
WHERE deleted = 0 AND report_status IN ('DRAFT', 'GENERATED');

-- 业务总量核对。
SELECT 'lab_monitoring_point' AS table_name, COUNT(*) AS total FROM lab_monitoring_point
UNION ALL SELECT 'lab_sampling_plan', COUNT(*) FROM lab_sampling_plan
UNION ALL SELECT 'lab_sampling_task', COUNT(*) FROM lab_sampling_task
UNION ALL SELECT 'lab_sample', COUNT(*) FROM lab_sample
UNION ALL SELECT 'lab_detection_record', COUNT(*) FROM lab_detection_record
UNION ALL SELECT 'lab_detection_item', COUNT(*) FROM lab_detection_item
UNION ALL SELECT 'lab_review_record', COUNT(*) FROM lab_review_record
UNION ALL SELECT 'lab_report', COUNT(*) FROM lab_report
UNION ALL SELECT 'lab_instrument', COUNT(*) FROM lab_instrument
UNION ALL SELECT 'lab_document', COUNT(*) FROM lab_document;
