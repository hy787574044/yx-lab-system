ALTER TABLE lab_detection_type
    ADD COLUMN group_id BIGINT NULL COMMENT '检测项目组主键' AFTER type_name,
    ADD COLUMN group_name VARCHAR(64) NULL COMMENT '检测项目组名称' AFTER group_id,
    ADD COLUMN detector_id BIGINT NULL COMMENT '绑定检测员主键' AFTER group_name,
    ADD COLUMN detector_name VARCHAR(64) NULL COMMENT '绑定检测员名称' AFTER detector_id;
