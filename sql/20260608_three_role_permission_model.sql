-- 角色模型调整为三类：系统管理员、主任、员工。
-- 系统管理员与主任拥有全系统权限；员工拥有采样员+检测员权限。

INSERT INTO lab_role (
    id, role_code, role_name, role_scope, status, remark, deleted,
    created_by, created_name, updated_by, updated_name
)
VALUES
    (901, 'ADMIN', '系统管理员', '全系统', 1, '系统全部权限', 0, 0, 'system', 0, 'system'),
    (902, 'DIRECTOR', '主任', '全系统', 1, '主任拥有与系统管理员一致的全系统权限', 0, 0, 'system', 0, 'system'),
    (903, 'STAFF', '员工', '业务执行', 1, '员工拥有采样员与检测员组合权限', 0, 0, 'system', 0, 'system')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name),
    role_scope = VALUES(role_scope),
    status = VALUES(status),
    remark = VALUES(remark),
    deleted = VALUES(deleted),
    updated_by = VALUES(updated_by),
    updated_name = VALUES(updated_name);

UPDATE lab_user
SET role_code = 'STAFF'
WHERE role_code IN ('SAMPLER', 'DETECTOR');

UPDATE lab_user
SET role_code = 'DIRECTOR'
WHERE role_code IN ('REVIEWER', 'REPORTER');

UPDATE lab_login_log
SET role_code = 'STAFF'
WHERE role_code IN ('SAMPLER', 'DETECTOR');

UPDATE lab_login_log
SET role_code = 'DIRECTOR'
WHERE role_code IN ('REVIEWER', 'REPORTER');

UPDATE lab_flow_node
SET role_code = 'DIRECTOR',
    role_name = '主任'
WHERE role_code IN ('REVIEWER', 'REPORTER');

UPDATE lab_role
SET role_code = CONCAT(role_code, '_OLD'),
    role_name = CONCAT(role_name, '（停用）'),
    status = 0,
    deleted = 1,
    remark = '已迁移至三角色模型'
WHERE role_code IN ('SAMPLER', 'DETECTOR', 'REVIEWER', 'REPORTER');
