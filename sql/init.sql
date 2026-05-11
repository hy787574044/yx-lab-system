CREATE DATABASE IF NOT EXISTS yx_lab DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE yx_lab;

DROP TABLE IF EXISTS lab_role;
CREATE TABLE lab_role (
    id BIGINT PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    role_scope VARCHAR(64),
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lab_role_code (role_code),
    UNIQUE KEY uk_lab_role_name (role_name)
);

DROP TABLE IF EXISTS lab_org;
CREATE TABLE lab_org (
    id BIGINT PRIMARY KEY,
    org_code VARCHAR(32) NOT NULL,
    org_name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    parent_name VARCHAR(64),
    org_type VARCHAR(64),
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lab_org_code (org_code),
    UNIQUE KEY uk_lab_org_name (org_name)
);

DROP TABLE IF EXISTS lab_dict;
CREATE TABLE lab_dict (
    id BIGINT PRIMARY KEY,
    dict_code VARCHAR(64) NOT NULL,
    dict_name VARCHAR(64) NOT NULL,
    module_name VARCHAR(64) NOT NULL,
    item_text TEXT,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lab_dict_code (dict_code)
);

DROP TABLE IF EXISTS lab_user;
CREATE TABLE lab_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    org_id BIGINT,
    org_name VARCHAR(64),
    role_code VARCHAR(32) NOT NULL,
    phone VARCHAR(32),
    status TINYINT DEFAULT 1,
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lab_user_username (username)
);

DROP TABLE IF EXISTS lab_login_log;
CREATE TABLE lab_login_log (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64) NOT NULL,
    real_name VARCHAR(64),
    role_code VARCHAR(32),
    login_channel VARCHAR(32),
    login_status VARCHAR(32),
    login_time DATETIME,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_login_log_user_id (user_id),
    KEY idx_lab_login_log_login_time (login_time)
);

DROP TABLE IF EXISTS lab_monitoring_point;
CREATE TABLE lab_monitoring_point (
    id BIGINT PRIMARY KEY,
    point_name VARCHAR(128) NOT NULL,
    longitude VARCHAR(32),
    latitude VARCHAR(32),
    region_name VARCHAR(128),
    service_population INT,
    frequency_type VARCHAR(32),
    owner_id BIGINT,
    owner_name VARCHAR(64),
    contact_phone VARCHAR(32),
    point_type VARCHAR(32),
    point_status VARCHAR(32),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_sampling_plan;
CREATE TABLE lab_sampling_plan (
    id BIGINT PRIMARY KEY,
    plan_name VARCHAR(128) NOT NULL,
    point_id BIGINT NOT NULL,
    point_name VARCHAR(128) NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    sampler_id BIGINT,
    sampler_name VARCHAR(64),
    sampling_type VARCHAR(32),
    sample_type VARCHAR(32),
    cycle_type VARCHAR(32),
    plan_status VARCHAR(32),
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_sampling_task;
CREATE TABLE lab_sampling_task (
    id BIGINT PRIMARY KEY,
    task_no VARCHAR(64) NOT NULL,
    plan_id BIGINT,
    point_id BIGINT NOT NULL,
    point_name VARCHAR(128) NOT NULL,
    sampling_time DATETIME,
    sampler_id BIGINT,
    sampler_name VARCHAR(64),
    sample_type VARCHAR(32),
    seal_no VARCHAR(64),
    sample_register_status VARCHAR(32),
    sample_id BIGINT,
    detection_items VARCHAR(1000),
    task_status VARCHAR(32),
    started_time DATETIME,
    onsite_metrics TEXT,
    photo_urls TEXT,
    abandon_reason VARCHAR(500),
    finished_time DATETIME,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_sampling_task_no (task_no),
    KEY idx_lab_sampling_task_seal_no (seal_no)
);

DROP TABLE IF EXISTS lab_sample;
CREATE TABLE lab_sample (
    id BIGINT PRIMARY KEY,
    sample_no VARCHAR(64) NOT NULL,
    seal_no VARCHAR(64),
    task_id BIGINT,
    point_id BIGINT NOT NULL,
    point_name VARCHAR(128) NOT NULL,
    sample_type VARCHAR(32),
    detection_items VARCHAR(1000),
    sampling_time DATETIME,
    seal_time DATETIME,
    sampler_id BIGINT,
    sampler_name VARCHAR(64),
    weather VARCHAR(32),
    storage_condition VARCHAR(64),
    sample_status VARCHAR(32),
    result_summary VARCHAR(255),
    remark VARCHAR(500),
    trace_log TEXT,
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_sample_no (sample_no),
    KEY idx_lab_sample_seal_no (seal_no)
);

DROP TABLE IF EXISTS lab_detection_type;
CREATE TABLE lab_detection_type (
    id BIGINT PRIMARY KEY,
    type_name VARCHAR(64) NOT NULL,
    group_id BIGINT,
    group_name VARCHAR(64),
    detector_id BIGINT,
    detector_name VARCHAR(64),
    parameter_ids VARCHAR(1000),
    parameter_names VARCHAR(1000),
    parameter_method_bindings TEXT,
    enabled TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_detection_parameter;
CREATE TABLE lab_detection_parameter (
    id BIGINT PRIMARY KEY,
    parameter_name VARCHAR(64) NOT NULL,
    standard_min DECIMAL(10,2),
    standard_max DECIMAL(10,2),
    unit VARCHAR(32),
    exceed_rule VARCHAR(32),
    reference_standard VARCHAR(128),
    enabled TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_detection_step;
CREATE TABLE lab_detection_step (
    id BIGINT PRIMARY KEY,
    type_id BIGINT,
    type_name VARCHAR(64),
    step_name VARCHAR(128) NOT NULL,
    step_order INT,
    step_description TEXT,
    reagent_requirement VARCHAR(500),
    operation_requirement VARCHAR(500),
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_detection_record;
CREATE TABLE lab_detection_record (
    id BIGINT PRIMARY KEY,
    sample_id BIGINT NOT NULL,
    sample_no VARCHAR(64) NOT NULL,
    seal_no VARCHAR(64),
    detection_type_id BIGINT,
    detection_type_name VARCHAR(64),
    detection_time DATETIME,
    detector_id BIGINT,
    detector_name VARCHAR(64),
    detection_result VARCHAR(32),
    abnormal_remark VARCHAR(500),
    detection_status VARCHAR(32),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_detection_item;
CREATE TABLE lab_detection_item (
    id BIGINT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    parameter_id BIGINT,
    parameter_name VARCHAR(64),
    standard_min DECIMAL(10,2),
    standard_max DECIMAL(10,2),
    result_value DECIMAL(10,2),
    unit VARCHAR(32),
    reference_standard VARCHAR(255),
    method_id BIGINT,
    method_name VARCHAR(128),
    detector_id BIGINT,
    detector_name VARCHAR(64),
    item_status VARCHAR(32),
    exceed_flag TINYINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_review_record;
CREATE TABLE lab_review_record (
    id BIGINT PRIMARY KEY,
    detection_record_id BIGINT NOT NULL,
    sample_id BIGINT,
    sample_no VARCHAR(64),
    seal_no VARCHAR(64),
    reviewer_id BIGINT,
    reviewer_name VARCHAR(64),
    review_time DATETIME,
    review_result VARCHAR(32),
    reject_reason VARCHAR(500),
    review_remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_report_template;
CREATE TABLE lab_report_template (
    id BIGINT PRIMARY KEY,
    report_type VARCHAR(32),
    template_name VARCHAR(128),
    default_template TINYINT DEFAULT 0,
    template_content TEXT,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_report;
CREATE TABLE lab_report (
    id BIGINT PRIMARY KEY,
    report_name VARCHAR(128) NOT NULL,
    report_type VARCHAR(32),
    generated_time DATETIME,
    sample_id BIGINT,
    sample_no VARCHAR(64),
    seal_no VARCHAR(64),
    detection_record_id BIGINT,
    report_status VARCHAR(32),
    published_time DATETIME,
    published_by BIGINT,
    published_by_name VARCHAR(64),
    push_status VARCHAR(32),
    last_push_time DATETIME,
    last_push_message VARCHAR(500),
    file_path VARCHAR(500),
    content_snapshot TEXT,
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_report_push_record;
CREATE TABLE lab_report_push_record (
    id BIGINT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    sample_id BIGINT,
    sample_no VARCHAR(64),
    seal_no VARCHAR(64),
    recipient_user_id BIGINT,
    recipient_name VARCHAR(64),
    recipient_phone VARCHAR(32),
    push_channel VARCHAR(32),
    push_status VARCHAR(32),
    push_message VARCHAR(500),
    push_time DATETIME,
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_report_push_report_id (report_id),
    KEY idx_lab_report_push_sample_id (sample_id)
);

DROP TABLE IF EXISTS lab_instrument;
CREATE TABLE lab_instrument (
    id BIGINT PRIMARY KEY,
    instrument_name VARCHAR(128) NOT NULL,
    instrument_model VARCHAR(128),
    manufacturer VARCHAR(128),
    purchase_date DATE,
    service_life_years INT,
    calibration_cycle VARCHAR(64),
    owner_name VARCHAR(64),
    instrument_status VARCHAR(32),
    storage_location VARCHAR(128),
    certificate_url VARCHAR(500),
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_instrument_maintenance;
CREATE TABLE lab_instrument_maintenance (
    id BIGINT PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    instrument_name VARCHAR(128),
    maintenance_time DATETIME,
    maintenance_reason VARCHAR(500),
    maintainer_name VARCHAR(64),
    maintenance_company VARCHAR(128),
    maintenance_result VARCHAR(255),
    maintenance_cost DECIMAL(10,2),
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_document;
CREATE TABLE lab_document (
    id BIGINT PRIMARY KEY,
    document_name VARCHAR(128) NOT NULL,
    document_category VARCHAR(64),
    file_type VARCHAR(32),
    file_size BIGINT,
    file_url VARCHAR(500),
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_document_share;
CREATE TABLE lab_document_share (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(64),
    real_name VARCHAR(64),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lab_document_share_doc_user (document_id, user_id),
    KEY idx_lab_document_share_document_id (document_id),
    KEY idx_lab_document_share_user_id (user_id)
);

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (901, 'ADMIN', '系统管理员', '全系统', 1, '负责系统配置、账号维护与基础资料管理', 0, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (902, 'SAMPLER', '采样员', '采样闭环', 1, '负责采样任务执行、样品登录与现场填报', 0, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (903, 'DETECTOR', '检测员', '检测闭环', 1, '负责检测分析、结果录入与重检提交', 0, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (904, 'REVIEWER', '审核员', '审核闭环', 1, '负责审核通过、驳回与重检门禁控制', 0, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (905, 'REPORTER', '报告员', '报告闭环', 1, '负责正式报告生成、发布与推送', 0, 'system', 'system');

INSERT INTO lab_org (id, org_code, org_name, parent_id, parent_name, org_type, status, remark, deleted, created_name, updated_name)
VALUES (801, 'YX-LAB', '阳新实验室', NULL, NULL, '中心实验室', 1, '系统默认顶级机构', 0, 'system', 'system');

INSERT INTO lab_org (id, org_code, org_name, parent_id, parent_name, org_type, status, remark, deleted, created_name, updated_name)
VALUES (802, 'YX-SAMPLE', '采样组', 801, '阳新实验室', '业务组', 1, '负责采样任务与样品登录', 0, 'system', 'system');

INSERT INTO lab_org (id, org_code, org_name, parent_id, parent_name, org_type, status, remark, deleted, created_name, updated_name)
VALUES (803, 'YX-DETECT', '检测审核组', 801, '阳新实验室', '业务组', 1, '负责检测、审核与报告发布', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (851, 'instrument_status', '设备状态字典', '仪器管理', '闲置\n使用中\n维保中\n停用', 1, '用于仪器设备状态展示与筛选', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (852, 'point_status', '点位状态字典', '监测点位', '启用\n停用\n维护中', 1, '用于监测点位状态管理', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (853, 'plan_status', '计划状态字典', '采样计划', '草稿\n待下发\n执行中\n已暂停\n已完成', 1, '用于采样计划生命周期控制', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (854, 'task_status', '任务状态字典', '采样任务', '待执行\n执行中\n已完成\n已废弃', 1, '用于采样任务流转控制', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (855, 'sample_status', '样品状态字典', '样品管理', '待登录\n已登录\n检测中\n已完成\n已退回', 1, '用于样品流转状态控制', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (856, 'detection_status', '检测状态字典', '检测管理', '待检测\n检测中\n待复核\n已退回\n已完成', 1, '用于检测流程状态控制', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (857, 'report_status', '报告状态字典', '报告管理', '待生成\n待发布\n已发布\n已撤回', 1, '用于报告正式产物状态管理', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (858, 'cycle_type', '周期类型字典', '基础配置', '每日\n每周\n每月\n每季度', 1, '用于周期计划与自动任务配置', 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1001, 'admin', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '系统管理员', 'ADMIN', '13800000000', 1, 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1002, 'sampler', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '采样员', 'SAMPLER', '13800000001', 1, 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1003, 'reviewer', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '审核员', 'REVIEWER', '13800000002', 1, 0, 'system', 'system');

UPDATE lab_user SET real_name = '系统管理员', org_id = 801, org_name = '阳新实验室' WHERE id = 1001;
UPDATE lab_user SET real_name = '采样员', org_id = 802, org_name = '采样组' WHERE id = 1002;
UPDATE lab_user SET real_name = '审核员', org_id = 803, org_name = '检测审核组' WHERE id = 1003;

INSERT INTO lab_monitoring_point (id, point_name, longitude, latitude, region_name, service_population, frequency_type, owner_id, owner_name, contact_phone, point_type, point_status, created_name, updated_name)
VALUES (2001, '城东水厂出厂水', '115.2121', '30.2211', '阳新县城东片区', 36000, 'DAILY', 1002, '采样员', '13800000001', 'FACTORY', 'ENABLED', 'system', 'system');

INSERT INTO lab_detection_parameter (id, parameter_name, standard_min, standard_max, unit, exceed_rule, reference_standard, enabled, created_name, updated_name)
VALUES
(3001, 'pH', 6.50, 8.50, '', 'OUT_OF_RANGE', 'GB 5749-2022', 1, 'system', 'system'),
(3002, '浊度', 0.00, 1.00, 'NTU', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, 'system', 'system'),
(3003, '余氯', 0.05, 2.00, 'mg/L', 'OUT_OF_RANGE', 'GB 5749-2022', 1, 'system', 'system'),
(3004, '氨氮', 0.00, 0.50, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, 'system', 'system');

INSERT INTO lab_detection_type (id, type_name, parameter_ids, parameter_names, enabled, created_name, updated_name)
VALUES (3101, '日检九项', '3001,3002,3003,3004', 'pH,浊度,余氯,氨氮', 1, 'system', 'system');

INSERT INTO lab_detection_step (id, type_id, type_name, step_name, step_order, step_description, reagent_requirement, operation_requirement, created_name, updated_name)
VALUES
(3201, 3101, '日检九项', '样品预处理', 1, '确认样品编号与保存条件，轻摇混匀后静置。', '无', '佩戴防护装备，核对样品标签。', 'system', 'system'),
(3202, 3101, '日检九项', '参数检测', 2, '按检测参数逐项完成仪器或人工检测。', '比色试剂、标准液', '按照标准操作步骤记录原始值。', 'system', 'system'),
(3203, 3101, '日检九项', '结果复核', 3, '检测完成后核对异常值并提交。', '无', '超标项需补充备注。', 'system', 'system');

INSERT INTO lab_report_template (id, report_type, template_name, default_template, template_content, created_name, updated_name)
VALUES (4001, 'DAILY', '日检默认模板', 1, '样品编号：${sampleNo}\n点位名称：${pointName}\n检测类型：${detectionType}\n检测结果：${detectionResult}\n结论：请结合现场复核。', 'system', 'system');
