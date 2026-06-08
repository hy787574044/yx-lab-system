ALTER TABLE lab_detection_type
    ADD COLUMN sample_type VARCHAR(32) NULL COMMENT '绑定样品类型' AFTER detector_name;

CREATE INDEX idx_lab_detection_type_sample_type ON lab_detection_type (sample_type);
