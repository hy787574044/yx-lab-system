ALTER TABLE lab_sampling_plan
    ADD COLUMN IF NOT EXISTS sampling_basis VARCHAR(1000) NULL COMMENT '采样依据' AFTER detection_config_snapshot;
