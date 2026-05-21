ALTER TABLE lab_review_record
    ADD COLUMN flow_id BIGINT NULL COMMENT '审核流程ID' AFTER seal_no,
    ADD COLUMN flow_node_id BIGINT NULL COMMENT '审核流程节点ID' AFTER flow_id,
    ADD COLUMN flow_node_name VARCHAR(128) NULL COMMENT '审核流程节点名称' AFTER flow_node_id,
    ADD COLUMN flow_node_order INT NULL COMMENT '审核流程节点顺序' AFTER flow_node_name,
    ADD COLUMN required_flag TINYINT NULL COMMENT '是否必审，1是，0否' AFTER flow_node_order,
    ADD KEY idx_lab_review_record_flow_node (detection_record_id, flow_node_id),
    ADD KEY idx_lab_review_record_flow_id (flow_id);
