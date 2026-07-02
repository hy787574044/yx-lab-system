CREATE TABLE IF NOT EXISTS lab_detection_assignment_memory (
    id BIGINT PRIMARY KEY,
    org_id BIGINT NOT NULL COMMENT '所属机构ID',
    parameter_id BIGINT NOT NULL COMMENT '检测参数ID',
    parameter_name VARCHAR(64) NULL COMMENT '检测参数名称',
    detector_id BIGINT NOT NULL COMMENT '检测人员ID',
    detector_name VARCHAR(64) NULL COMMENT '检测人员名称',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记，0未删除，1已删除',
    created_by BIGINT NULL COMMENT '创建人ID',
    created_name VARCHAR(64) NULL COMMENT '创建人名称',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NULL COMMENT '更新人ID',
    updated_name VARCHAR(64) NULL COMMENT '更新人名称',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_detection_assignment_memory_org_param (org_id, parameter_id),
    KEY idx_detection_assignment_memory_detector (detector_id)
) COMMENT = '检测参数人员分配记忆表';
