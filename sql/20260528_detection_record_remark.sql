ALTER TABLE lab_detection_record
    ADD COLUMN remark VARCHAR(500) NULL COMMENT '备注' AFTER abnormal_remark;
