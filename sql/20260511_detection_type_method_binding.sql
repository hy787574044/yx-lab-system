ALTER TABLE lab_detection_type
    ADD COLUMN parameter_method_bindings TEXT NULL COMMENT '检测套餐内参数-方法绑定关系 JSON'
    AFTER parameter_names;
