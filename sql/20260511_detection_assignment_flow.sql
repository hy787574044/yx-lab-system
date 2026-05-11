ALTER TABLE lab_detection_item
    ADD COLUMN reference_standard VARCHAR(255) NULL COMMENT '参考范围/参考标准' AFTER unit,
    ADD COLUMN method_id BIGINT NULL COMMENT '检测方法ID' AFTER reference_standard,
    ADD COLUMN method_name VARCHAR(128) NULL COMMENT '检测方法名称' AFTER method_id,
    ADD COLUMN detector_id BIGINT NULL COMMENT '分配检测员ID' AFTER method_name,
    ADD COLUMN detector_name VARCHAR(64) NULL COMMENT '分配检测员名称' AFTER detector_id,
    ADD COLUMN item_status VARCHAR(32) NULL COMMENT '参数子流程状态' AFTER detector_name;
