-- 清理业务数据，保留系统配置数据。
-- 适用场景：表结构多次调整后，清空运行过程和台账数据，保留系统基础配置后重新录入业务。
--
-- 保留：
--   lab_role、lab_org、lab_dict、lab_user
--   lab_flow_config、lab_flow_node
--   lab_detection_project_group、lab_detection_parameter、lab_detection_method、lab_detection_type、lab_detection_step
--   lab_report_template
--
-- 清理：
--   监测点位、采样计划、采样任务、样品、检测流程、审核、报告、方法设备绑定、设备、维修、文档、推送、登录日志。
--
-- 执行前建议先备份数据库：
--   mysqldump -h <host> -u<user> -p <database> > yx_lab_backup_before_clear.sql

SET FOREIGN_KEY_CHECKS = 0;

-- 报告与审核链路，先清子表再清主表。
TRUNCATE TABLE lab_report_push_record;
TRUNCATE TABLE lab_report;
TRUNCATE TABLE lab_review_record;

-- 检测流程链路。
TRUNCATE TABLE lab_detection_item;
TRUNCATE TABLE lab_detection_record;

-- 样品与采样链路。
TRUNCATE TABLE lab_sample;
TRUNCATE TABLE lab_sampling_task;
TRUNCATE TABLE lab_sampling_plan;
TRUNCATE TABLE lab_monitoring_point;

-- 设备、维修、文档台账。
TRUNCATE TABLE lab_detection_method_instrument_model_binding;
TRUNCATE TABLE lab_instrument_maintenance;
TRUNCATE TABLE lab_instrument;
TRUNCATE TABLE lab_document_share;
TRUNCATE TABLE lab_document;

-- 运行日志。
TRUNCATE TABLE lab_login_log;

SET FOREIGN_KEY_CHECKS = 1;

-- 清理后计数核对。
SELECT 'lab_report_push_record' AS table_name, COUNT(*) AS total FROM lab_report_push_record
UNION ALL SELECT 'lab_report', COUNT(*) FROM lab_report
UNION ALL SELECT 'lab_review_record', COUNT(*) FROM lab_review_record
UNION ALL SELECT 'lab_detection_item', COUNT(*) FROM lab_detection_item
UNION ALL SELECT 'lab_detection_record', COUNT(*) FROM lab_detection_record
UNION ALL SELECT 'lab_sample', COUNT(*) FROM lab_sample
UNION ALL SELECT 'lab_sampling_task', COUNT(*) FROM lab_sampling_task
UNION ALL SELECT 'lab_sampling_plan', COUNT(*) FROM lab_sampling_plan
UNION ALL SELECT 'lab_monitoring_point', COUNT(*) FROM lab_monitoring_point
UNION ALL SELECT 'lab_detection_method_instrument_model_binding', COUNT(*) FROM lab_detection_method_instrument_model_binding
UNION ALL SELECT 'lab_instrument_maintenance', COUNT(*) FROM lab_instrument_maintenance
UNION ALL SELECT 'lab_instrument', COUNT(*) FROM lab_instrument
UNION ALL SELECT 'lab_document_share', COUNT(*) FROM lab_document_share
UNION ALL SELECT 'lab_document', COUNT(*) FROM lab_document
UNION ALL SELECT 'lab_login_log', COUNT(*) FROM lab_login_log;
