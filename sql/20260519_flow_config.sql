CREATE TABLE IF NOT EXISTS lab_flow_config (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    flow_name VARCHAR(128) NOT NULL COMMENT '流程名称',
    flow_type VARCHAR(32) NOT NULL COMMENT '流程类型：REVIEW审核流程，PUBLISH发布流程',
    scope_name VARCHAR(128) NOT NULL COMMENT '适用范围',
    default_flag TINYINT DEFAULT 0 COMMENT '是否默认流程：1是，0否',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用，0停用',
    remark VARCHAR(500) COMMENT '备注',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    created_by BIGINT COMMENT '创建人ID',
    created_name VARCHAR(64) COMMENT '创建人名称',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人ID',
    updated_name VARCHAR(64) COMMENT '更新人名称',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_lab_flow_config_name (flow_name),
    KEY idx_lab_flow_config_type_status (flow_type, status),
    KEY idx_lab_flow_config_default (flow_type, default_flag)
) COMMENT='流程配置表';

CREATE TABLE IF NOT EXISTS lab_flow_node (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    flow_id BIGINT NOT NULL COMMENT '流程配置ID',
    node_order INT NOT NULL COMMENT '节点顺序',
    node_name VARCHAR(64) NOT NULL COMMENT '节点名称',
    role_name VARCHAR(64) NOT NULL COMMENT '审批角色',
    assignee_name VARCHAR(64) COMMENT '指定人员',
    required_flag TINYINT DEFAULT 1 COMMENT '是否必审：1是，0否',
    reject_mode VARCHAR(32) DEFAULT 'PREVIOUS' COMMENT '驳回方式：PREVIOUS退回上一步，DETECTION退回检测，TERMINATE流程终止',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    created_by BIGINT COMMENT '创建人ID',
    created_name VARCHAR(64) COMMENT '创建人名称',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人ID',
    updated_name VARCHAR(64) COMMENT '更新人名称',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_lab_flow_node_flow_id (flow_id),
    KEY idx_lab_flow_node_order (flow_id, node_order)
) COMMENT='流程节点配置表';

INSERT INTO lab_flow_config (id, flow_name, flow_type, scope_name, default_flag, status, remark, deleted, created_name, updated_name)
SELECT 9601, '常规三级审核', 'REVIEW', '全部样品', 1, 1, '样品检测完成后进入初审、复审、终审。', 0, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM lab_flow_config WHERE id = 9601);

INSERT INTO lab_flow_config (id, flow_name, flow_type, scope_name, default_flag, status, remark, deleted, created_name, updated_name)
SELECT 9602, '报告发布审批', 'PUBLISH', '全部报告', 1, 1, '报告生成后先复核，再确认发布。', 0, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM lab_flow_config WHERE id = 9602);

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
SELECT 9611, 9601, 1, '初审', '审核员', NULL, 1, 'DETECTION', 0, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM lab_flow_node WHERE id = 9611);

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
SELECT 9612, 9601, 2, '复审', '审核员', NULL, 1, 'PREVIOUS', 0, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM lab_flow_node WHERE id = 9612);

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
SELECT 9613, 9601, 3, '终审', '审核员', NULL, 1, 'PREVIOUS', 0, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM lab_flow_node WHERE id = 9613);

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
SELECT 9621, 9602, 1, '报告复核', '报告员', NULL, 1, 'PREVIOUS', 0, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM lab_flow_node WHERE id = 9621);

INSERT INTO lab_flow_node (id, flow_id, node_order, node_name, role_name, assignee_name, required_flag, reject_mode, deleted, created_name, updated_name)
SELECT 9622, 9602, 2, '发布确认', '报告员', NULL, 1, 'TERMINATE', 0, 'system', 'system'
WHERE NOT EXISTS (SELECT 1 FROM lab_flow_node WHERE id = 9622);
