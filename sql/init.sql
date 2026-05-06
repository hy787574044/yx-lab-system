CREATE DATABASE IF NOT EXISTS yx_lab DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE yx_lab;

DROP TABLE IF EXISTS lab_user;
CREATE TABLE lab_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
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
    plan_id BIGINT,
    point_id BIGINT NOT NULL,
    point_name VARCHAR(128) NOT NULL,
    sampling_time DATETIME,
    sampler_id BIGINT,
    sampler_name VARCHAR(64),
    sample_type VARCHAR(32),
    detection_items VARCHAR(1000),
    task_status VARCHAR(32),
    onsite_metrics TEXT,
    photo_urls TEXT,
    finished_time DATETIME,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_sample;
CREATE TABLE lab_sample (
    id BIGINT PRIMARY KEY,
    sample_no VARCHAR(64) NOT NULL,
    task_id BIGINT,
    point_id BIGINT NOT NULL,
    point_name VARCHAR(128) NOT NULL,
    sample_type VARCHAR(32),
    detection_items VARCHAR(1000),
    sampling_time DATETIME,
    sampler_id BIGINT,
    sampler_name VARCHAR(64),
    weather VARCHAR(32),
    storage_condition VARCHAR(64),
    sample_status VARCHAR(32),
    result_summary VARCHAR(255),
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_sample_no (sample_no)
);

DROP TABLE IF EXISTS lab_detection_type;
CREATE TABLE lab_detection_type (
    id BIGINT PRIMARY KEY,
    type_name VARCHAR(64) NOT NULL,
    parameter_ids VARCHAR(1000),
    parameter_names VARCHAR(1000),
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
    detection_record_id BIGINT,
    report_status VARCHAR(32),
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

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1001, 'admin', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '系统管理员', 'ADMIN', '13800000000', 1, 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1002, 'sampler', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '采样员', 'SAMPLER', '13800000001', 1, 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1003, 'reviewer', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '审核员', 'REVIEWER', '13800000002', 1, 0, 'system', 'system');

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
