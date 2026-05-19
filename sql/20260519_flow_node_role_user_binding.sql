ALTER TABLE lab_flow_node
    ADD COLUMN role_code VARCHAR(64) NULL COMMENT '审批角色编码' AFTER role_name,
    ADD COLUMN assignee_id BIGINT NULL COMMENT '指定人员ID' AFTER role_code,
    ADD KEY idx_lab_flow_node_role_code (role_code),
    ADD KEY idx_lab_flow_node_assignee_id (assignee_id);

UPDATE lab_flow_node
SET role_code = 'REVIEWER',
    role_name = '审核员'
WHERE flow_id = 9601
  AND role_code IS NULL;

UPDATE lab_flow_node
SET role_code = 'REPORTER',
    role_name = '报告员'
WHERE flow_id = 9602
  AND role_code IS NULL;
