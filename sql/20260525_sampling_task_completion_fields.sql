ALTER TABLE lab_sampling_task
    ADD COLUMN weather VARCHAR(64) NULL COMMENT '现场天气情况' AFTER onsite_metrics,
    ADD COLUMN temperature VARCHAR(32) NULL COMMENT '现场温度' AFTER weather;
