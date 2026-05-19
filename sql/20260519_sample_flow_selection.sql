ALTER TABLE lab_sample
    ADD COLUMN review_flow_id BIGINT NULL COMMENT '样品登录时选择的审核流程ID' AFTER detection_config_snapshot,
    ADD COLUMN review_flow_name VARCHAR(128) NULL COMMENT '样品登录时选择的审核流程名称' AFTER review_flow_id,
    ADD COLUMN publish_flow_id BIGINT NULL COMMENT '样品登录时选择的发布流程ID' AFTER review_flow_name,
    ADD COLUMN publish_flow_name VARCHAR(128) NULL COMMENT '样品登录时选择的发布流程名称' AFTER publish_flow_id,
    ADD KEY idx_lab_sample_review_flow_id (review_flow_id),
    ADD KEY idx_lab_sample_publish_flow_id (publish_flow_id);
