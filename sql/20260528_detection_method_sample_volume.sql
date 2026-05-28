ALTER TABLE lab_detection_method
    ADD COLUMN sample_volume VARCHAR(64) NULL COMMENT '取样体积' AFTER standard_code;
