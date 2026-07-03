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
    avatar_url VARCHAR(500),
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

DROP TABLE IF EXISTS lab_flow_node;
DROP TABLE IF EXISTS lab_flow_config;
CREATE TABLE lab_flow_config (
    id BIGINT PRIMARY KEY,
    flow_name VARCHAR(128) NOT NULL,
    flow_type VARCHAR(32) NOT NULL COMMENT 'REVIEW瀹℃牳娴佺▼锛孭UBLISH鍙戝竷娴佺▼',
    scope_name VARCHAR(128) NOT NULL,
    default_flag TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_lab_flow_config_name (flow_name),
    KEY idx_lab_flow_config_type_status (flow_type, status),
    KEY idx_lab_flow_config_default (flow_type, default_flag)
);

CREATE TABLE lab_flow_node (
    id BIGINT PRIMARY KEY,
    flow_id BIGINT NOT NULL,
    node_order INT NOT NULL,
    node_name VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(64),
    assignee_id BIGINT,
    assignee_name VARCHAR(64),
    required_flag TINYINT DEFAULT 1,
    reject_mode VARCHAR(32) DEFAULT 'PREVIOUS' COMMENT 'PREVIOUS閫€鍥炰笂涓€姝ワ紝DETECTION閫€鍥炴娴嬶紝TERMINATE娴佺▼缁堟',
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_flow_node_flow_id (flow_id),
    KEY idx_lab_flow_node_order (flow_id, node_order),
    KEY idx_lab_flow_node_role_code (role_code),
    KEY idx_lab_flow_node_assignee_id (assignee_id)
);

DROP TABLE IF EXISTS lab_monitoring_point;
CREATE TABLE lab_monitoring_point (
    id BIGINT PRIMARY KEY,
    point_name VARCHAR(128) NOT NULL,
    address VARCHAR(255),
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
    org_id BIGINT,
    point_id BIGINT,
    point_name VARCHAR(128) NOT NULL,
    address VARCHAR(255),
    latitude VARCHAR(32),
    longitude VARCHAR(32),
    start_time DATETIME,
    end_time DATETIME,
    sampler_id BIGINT,
    sampler_ids VARCHAR(255),
    sampler_name VARCHAR(64),
    sampling_type VARCHAR(32),
    sample_type VARCHAR(32),
    detection_type_id BIGINT,
    detection_type_name VARCHAR(128),
    detection_config_snapshot TEXT,
    cycle_type VARCHAR(32),
    plan_status VARCHAR(32),
    remark VARCHAR(500),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_sampling_plan_org_id (org_id)
);

DROP TABLE IF EXISTS lab_sampling_task;
CREATE TABLE lab_sampling_task (
    id BIGINT PRIMARY KEY,
    task_no VARCHAR(64) NOT NULL,
    sample_no VARCHAR(64),
    plan_id BIGINT,
    org_id BIGINT,
    point_id BIGINT,
    point_name VARCHAR(128) NOT NULL,
    sampling_time DATETIME,
    sampler_id BIGINT,
    sampler_ids VARCHAR(255),
    sampler_name VARCHAR(64),
    sample_type VARCHAR(32),
    sample_register_status VARCHAR(32),
    sample_id BIGINT,
    detection_items VARCHAR(1000),
    detection_type_id BIGINT,
    detection_type_name VARCHAR(128),
    detection_config_snapshot TEXT,
    task_status VARCHAR(32),
    started_time DATETIME,
    onsite_metrics TEXT,
    weather VARCHAR(64),
    temperature VARCHAR(32),
    sample_total_volume VARCHAR(64),
    sample_bottle_count VARCHAR(32),
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
    KEY idx_lab_sampling_task_org_id (org_id),
    KEY idx_lab_sampling_task_no (task_no),
    KEY idx_lab_sampling_task_sample_no (sample_no)
);

DROP TABLE IF EXISTS lab_sample;
CREATE TABLE lab_sample (
    id BIGINT PRIMARY KEY,
    sample_no VARCHAR(64) NOT NULL,
    task_id BIGINT,
    org_id BIGINT,
    point_id BIGINT,
    point_name VARCHAR(128) NOT NULL,
    sample_type VARCHAR(32),
    sample_source_method VARCHAR(32) DEFAULT 'SAMPLING',
    detection_items VARCHAR(1000),
    detection_type_id BIGINT,
    detection_type_name VARCHAR(128),
    detection_config_snapshot TEXT,
    review_flow_id BIGINT,
    review_flow_name VARCHAR(128),
    sampling_time DATETIME,
    sample_total_volume VARCHAR(64),
    sample_bottle_count VARCHAR(32),
    sampler_id BIGINT,
    sampler_ids VARCHAR(255),
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
    KEY idx_lab_sample_org_id (org_id),
    KEY idx_lab_sample_no (sample_no),
    KEY idx_lab_sample_review_flow_id (review_flow_id)
);

DROP TABLE IF EXISTS lab_sample_no_sequence;
CREATE TABLE lab_sample_no_sequence (
    sequence_date VARCHAR(8) PRIMARY KEY,
    current_value BIGINT NOT NULL DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

DROP TABLE IF EXISTS lab_detection_type;
CREATE TABLE lab_detection_type (
    id BIGINT PRIMARY KEY,
    type_name VARCHAR(64) NOT NULL,
    group_id BIGINT,
    group_name VARCHAR(64),
    detector_id BIGINT,
    detector_name VARCHAR(64),
    sample_type VARCHAR(32),
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

DROP TABLE IF EXISTS lab_detection_method_instrument_model_binding;
CREATE TABLE lab_detection_method_instrument_model_binding (
    id BIGINT PRIMARY KEY,
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
    org_id BIGINT,
    detection_type_id BIGINT,
    detection_type_name VARCHAR(64),
    detection_time DATETIME,
    detector_id BIGINT,
    detector_name VARCHAR(64),
    detection_result VARCHAR(32),
    abnormal_remark VARCHAR(500),
    remark VARCHAR(500),
    detection_status VARCHAR(32),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_detection_record_org_id (org_id)
);

DROP TABLE IF EXISTS lab_detection_item;
CREATE TABLE lab_detection_item (
    id BIGINT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    org_id BIGINT,
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
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_detection_item_org_id (org_id)
);

DROP TABLE IF EXISTS lab_detection_assignment_memory;
CREATE TABLE lab_detection_assignment_memory (
    id BIGINT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    parameter_id BIGINT NOT NULL,
    parameter_name VARCHAR(64),
    detector_id BIGINT NOT NULL,
    detector_name VARCHAR(64),
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_detection_assignment_memory_org_param (org_id, parameter_id),
    KEY idx_detection_assignment_memory_detector (detector_id)
);

DROP TABLE IF EXISTS lab_review_record;
CREATE TABLE lab_review_record (
    id BIGINT PRIMARY KEY,
    detection_record_id BIGINT NOT NULL,
    sample_id BIGINT,
    sample_no VARCHAR(64),
    org_id BIGINT,
    flow_id BIGINT,
    flow_node_id BIGINT,
    flow_node_name VARCHAR(128),
    flow_node_order INT,
    required_flag TINYINT,
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
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_review_record_org_id (org_id)
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
    report_category VARCHAR(32) DEFAULT 'DETECTION_REPORT',
    generated_time DATETIME,
    sample_id BIGINT,
    sample_no VARCHAR(64),
    org_id BIGINT,
    detection_record_id BIGINT,
    report_status VARCHAR(32),
    published_time DATETIME,
    published_by BIGINT,
    published_by_name VARCHAR(64),
    file_path VARCHAR(500),
    content_snapshot TEXT,
    deleted TINYINT DEFAULT 0,
    created_by BIGINT,
    created_name VARCHAR(64),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_name VARCHAR(64),
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_lab_report_org_id (org_id),
    KEY idx_lab_report_category_status (report_category, report_status),
    KEY idx_lab_report_sample_no (sample_no)
);

DROP TABLE IF EXISTS lab_report_push_record;
CREATE TABLE lab_report_push_record (
    id BIGINT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    sample_id BIGINT,
    sample_no VARCHAR(64),
    org_id BIGINT,
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
    KEY idx_lab_report_push_org_id (org_id),
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
VALUES (901, 'ADMIN', '系统管理员', '全系统', 1, '系统全部权限', 0, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (902, 'DIRECTOR', '主任', '全系统', 1, '主任拥有与系统管理员一致的全系统权限', 0, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (903, 'STAFF', '员工', '业务执行', 1, '员工拥有采样员与检测员组合权限', 0, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (904, 'REVIEWER_OLD', '审核员（停用）', '审核闭环', 0, '已迁移至主任角色', 1, 'system', 'system');

INSERT INTO lab_role (id, role_code, role_name, role_scope, status, remark, deleted, created_name, updated_name)
VALUES (905, 'REPORTER_OLD', '报告员（停用）', '报告闭环', 0, '已迁移至主任角色', 1, 'system', 'system');

INSERT INTO lab_org (id, org_code, org_name, parent_id, parent_name, org_type, status, remark, deleted, created_name, updated_name)
VALUES (801, 'YX-LAB', '闃虫柊瀹為獙瀹?, NULL, NULL, '涓績瀹為獙瀹?, 1, '绯荤粺榛樿椤剁骇鏈烘瀯', 0, 'system', 'system');

INSERT INTO lab_org (id, org_code, org_name, parent_id, parent_name, org_type, status, remark, deleted, created_name, updated_name)
VALUES (802, 'YX-SAMPLE', '閲囨牱缁?, 801, '闃虫柊瀹為獙瀹?, '涓氬姟缁?, 1, '璐熻矗閲囨牱浠诲姟涓庢牱鍝佺櫥褰?, 0, 'system', 'system');

INSERT INTO lab_org (id, org_code, org_name, parent_id, parent_name, org_type, status, remark, deleted, created_name, updated_name)
VALUES (803, 'YX-DETECT', '妫€娴嬪鏍哥粍', 801, '闃虫柊瀹為獙瀹?, '涓氬姟缁?, 1, '璐熻矗妫€娴嬨€佸鏍镐笌鎶ュ憡鍙戝竷', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (851, 'instrument_status', '璁惧鐘舵€佸瓧鍏?, '浠櫒绠＄悊', '闂茬疆\n浣跨敤涓璡n缁翠繚涓璡n鍋滅敤', 1, '鐢ㄤ簬浠櫒璁惧鐘舵€佸睍绀轰笌绛涢€?, 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (852, 'point_status', '鐐逛綅鐘舵€佸瓧鍏?, '鐩戞祴鐐逛綅', '鍚敤\n鍋滅敤\n缁存姢涓?, 1, '鐢ㄤ簬鐩戞祴鐐逛綅鐘舵€佺鐞?, 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (853, 'plan_status', '璁″垝鐘舵€佸瓧鍏?, '閲囨牱璁″垝', '鑽夌\n寰呬笅鍙慭n鎵ц涓璡n宸叉殏鍋淺n宸插畬鎴?, 1, '鐢ㄤ簬閲囨牱璁″垝鐢熷懡鍛ㄦ湡鎺у埗', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (854, 'task_status', '浠诲姟鐘舵€佸瓧鍏?, '閲囨牱浠诲姟', '寰呮墽琛孿n鎵ц涓璡n宸插畬鎴怽n宸插簾寮?, 1, '鐢ㄤ簬閲囨牱浠诲姟娴佽浆鎺у埗', 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (855, 'sample_status', '鏍峰搧鐘舵€佸瓧鍏?, '鏍峰搧绠＄悊', '寰呯櫥褰昞n宸茬櫥褰昞n妫€娴嬩腑\n宸插畬鎴怽n宸查€€鍥?, 1, '鐢ㄤ簬鏍峰搧娴佽浆鐘舵€佹帶鍒?, 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (856, 'detection_status', '妫€娴嬬姸鎬佸瓧鍏?, '妫€娴嬬鐞?, '寰呮娴媆n妫€娴嬩腑\n寰呭鏍竆n宸查€€鍥瀄n宸插畬鎴?, 1, '鐢ㄤ簬妫€娴嬫祦绋嬬姸鎬佹帶鍒?, 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (857, 'report_status', '鎶ュ憡鐘舵€佸瓧鍏?, '鎶ュ憡绠＄悊', '寰呯敓鎴怽n寰呭彂甯僜n宸插彂甯僜n宸叉挙鍥?, 1, '鐢ㄤ簬鎶ュ憡姝ｅ紡浜х墿鐘舵€佺鐞?, 0, 'system', 'system');

INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (858, 'cycle_type', '鍛ㄦ湡绫诲瀷瀛楀吀', '鍩虹閰嶇疆', '姣忔棩\n姣忓懆\n姣忔湀\n姣忓搴?, 1, '鐢ㄤ簬鍛ㄦ湡璁″垝涓庤嚜鍔ㄤ换鍔￠厤缃?, 0, 'system', 'system');


INSERT INTO lab_dict (id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name)
VALUES (860, 'water_plant', '鎵€灞炴按鍘?, '閲囨牱绠＄悊', 'EAST=鍩庝笢姘村巶\nWEST=鍩庤タ姘村巶', 1, '鐢ㄤ簬鐩戞祴鐐瑰彴璐︽墍灞炴按鍘備笅鎷夐厤缃?, 0, 'system', 'system');

INSERT INTO lab_flow_config (id, flow_name, flow_type, scope_name, default_flag, status, remark, deleted, created_name, updated_name)
VALUES (9601, '甯歌涓夌骇瀹℃牳', 'REVIEW', '鍏ㄩ儴鏍峰搧', 1, 1, '鏍峰搧妫€娴嬪畬鎴愬悗杩涘叆鍒濆銆佸瀹°€佺粓瀹°€?, 0, 'system', 'system');

INSERT INTO lab_flow_config (id, flow_name, flow_type, scope_name, default_flag, status, remark, deleted, created_name, updated_name)
VALUES (9602, '鎶ュ憡鍙戝竷瀹℃壒', 'PUBLISH', '鍏ㄩ儴鎶ュ憡', 1, 1, '鎶ュ憡鐢熸垚鍚庡厛澶嶆牳锛屽啀纭鍙戝竷銆?, 0, 'system', 'system');

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, role_code, assignee_id, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
VALUES (9611, 9601, 1, '初审', '主任', 'DIRECTOR', NULL, NULL, 1, 'DETECTION', 0, 'system', 'system');

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, role_code, assignee_id, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
VALUES (9612, 9601, 2, '复审', '主任', 'DIRECTOR', NULL, NULL, 1, 'PREVIOUS', 0, 'system', 'system');

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, role_code, assignee_id, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
VALUES (9613, 9601, 3, '终审', '主任', 'DIRECTOR', NULL, NULL, 1, 'PREVIOUS', 0, 'system', 'system');

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, role_code, assignee_id, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
VALUES (9621, 9602, 1, '报告复核', '主任', 'DIRECTOR', NULL, NULL, 1, 'PREVIOUS', 0, 'system', 'system');

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, role_code, assignee_id, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
VALUES (9622, 9602, 2, '发布确认', '主任', 'DIRECTOR', NULL, NULL, 1, 'TERMINATE', 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1001, 'admin', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '系统管理员', 'ADMIN', '13800000000', 1, 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1002, 'staff', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '员工', 'STAFF', '13800000001', 1, 0, 'system', 'system');

INSERT INTO lab_user (id, username, password, real_name, role_code, phone, status, deleted, created_name, updated_name)
VALUES (1003, 'director', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '主任', 'DIRECTOR', '13800000002', 1, 0, 'system', 'system');

UPDATE lab_user SET real_name = '系统管理员', org_id = 801, org_name = '阳新实验室' WHERE id = 1001;
UPDATE lab_user SET real_name = '员工', org_id = 802, org_name = '采样组' WHERE id = 1002;
UPDATE lab_user SET real_name = '主任', org_id = 803, org_name = '检测审核组' WHERE id = 1003;

INSERT INTO lab_monitoring_point (id, point_name, longitude, latitude, region_name, service_population, frequency_type, owner_id, owner_name, contact_phone, point_type, point_status, created_name, updated_name)
VALUES (2001, '鍩庝笢姘村巶鍑哄巶姘?, '115.2121', '30.2211', '闃虫柊鍘垮煄涓滅墖鍖?, 36000, 'DAILY', 1002, '閲囨牱鍛?, '13800000001', 'FACTORY', 'ENABLED', 'system', 'system');

INSERT INTO lab_detection_parameter (id, parameter_name, standard_min, standard_max, unit, exceed_rule, reference_standard, enabled, created_name, updated_name)
VALUES
(3001, 'pH', 6.50, 8.50, '', 'OUT_OF_RANGE', 'GB 5749-2022', 1, 'system', 'system'),
(3002, '娴婂害', 0.00, 1.00, 'NTU', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, 'system', 'system'),
(3003, '浣欐隘', 0.05, 2.00, 'mg/L', 'OUT_OF_RANGE', 'GB 5749-2022', 1, 'system', 'system'),
(3004, '姘ㄦ爱', 0.00, 0.50, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, 'system', 'system');

INSERT INTO lab_detection_type (id, type_name, parameter_ids, parameter_names, enabled, created_name, updated_name)
VALUES (3101, '鏃ユ涔濋」', '3001,3002,3003,3004', 'pH,娴婂害,浣欐隘,姘ㄦ爱', 1, 'system', 'system');

INSERT INTO lab_detection_step (id, type_id, type_name, step_name, step_order, step_description, reagent_requirement, operation_requirement, created_name, updated_name)
VALUES
(3201, 3101, '鏃ユ涔濋」', '鏍峰搧棰勫鐞?, 1, '纭鏍峰搧缂栧彿涓庝繚瀛樻潯浠讹紝杞绘憞娣峰寑鍚庨潤缃€?, '鏃?, '浣╂埓闃叉姢瑁呭锛屾牳瀵规牱鍝佹爣绛俱€?, 'system', 'system'),
(3202, 3101, '鏃ユ涔濋」', '鍙傛暟妫€娴?, 2, '鎸夋娴嬪弬鏁伴€愰」瀹屾垚浠櫒鎴栦汉宸ユ娴嬨€?, '姣旇壊璇曞墏銆佹爣鍑嗘恫', '鎸夌収鏍囧噯鎿嶄綔姝ラ璁板綍鍘熷鍊笺€?, 'system', 'system'),
(3203, 3101, '鏃ユ涔濋」', '缁撴灉澶嶆牳', 3, '妫€娴嬪畬鎴愬悗鏍稿寮傚父鍊煎苟鎻愪氦銆?, '鏃?, '瓒呮爣椤归渶琛ュ厖澶囨敞銆?, 'system', 'system');

INSERT INTO lab_report_template (id, report_type, template_name, default_template, template_content, created_name, updated_name)
VALUES (4001, 'DAILY', '鏃ユ榛樿妯℃澘', 1, '鏍峰搧缂栧彿锛?{sampleNo}\n鐐逛綅鍚嶇О锛?{pointName}\n妫€娴嬬被鍨嬶細${detectionType}\n妫€娴嬬粨鏋滐細${detectionResult}\n缁撹锛氳缁撳悎鐜板満澶嶆牳銆?, 'system', 'system');
