CREATE TABLE IF NOT EXISTS lab_detection_method_instrument_model_binding (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',
    method_id BIGINT NOT NULL COMMENT '检测方法ID',
    method_name VARCHAR(128) NOT NULL COMMENT '检测方法名称',
    instrument_model VARCHAR(128) NOT NULL COMMENT '设备型号',
    manufacturer VARCHAR(128) NOT NULL DEFAULT '' COMMENT '生产厂家',
    instrument_count INT NULL DEFAULT 0 COMMENT '当前型号在库设备数量',
    remark VARCHAR(500) NULL COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    created_by BIGINT NULL COMMENT '创建人ID',
    created_name VARCHAR(64) NULL COMMENT '创建人名称',
    created_time DATETIME NULL COMMENT '创建时间',
    updated_by BIGINT NULL COMMENT '更新人ID',
    updated_name VARCHAR(64) NULL COMMENT '更新人名称',
    updated_time DATETIME NULL COMMENT '更新时间',
    KEY idx_method_id (method_id),
    KEY idx_instrument_model (instrument_model),
    KEY idx_model_manufacturer (instrument_model, manufacturer)
) COMMENT='检测方法与设备型号绑定关系';
