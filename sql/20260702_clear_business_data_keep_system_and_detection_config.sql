-- Clear business/runtime data only.
-- Keep all system-management data and all detection parameter/method related configuration.
--
-- KEEP:
--   System management:
--     lab_role, lab_org, lab_dict, lab_user, lab_login_log,
--     lab_flow_config, lab_flow_node
--   Detection configuration:
--     lab_detection_project_group, lab_detection_parameter, lab_detection_method,
--     lab_detection_method_instrument_model_binding, lab_detection_type,
--     lab_detection_step, lab_detection_assignment_memory
--   Report configuration:
--     lab_report_template
--
-- CLEAR:
--   lab_report_push_record, lab_report, lab_review_record,
--   lab_detection_item, lab_detection_record,
--   lab_sample, lab_sampling_task, lab_sampling_plan, lab_monitoring_point,
--   lab_sample_no_sequence,
--   lab_instrument_maintenance, lab_instrument,
--   lab_document_share, lab_document
--
-- Recommended backup before execution:
--   mysqldump -h <host> -u<user> -p <database> > yx_lab_backup_before_clear.sql

SET FOREIGN_KEY_CHECKS = 0;

-- Report, push, and review runtime data.
TRUNCATE TABLE lab_report_push_record;
TRUNCATE TABLE lab_report;
TRUNCATE TABLE lab_review_record;

-- Detection workflow runtime data.
TRUNCATE TABLE lab_detection_item;
TRUNCATE TABLE lab_detection_record;

-- Sampling, sample login, and sample number runtime data.
TRUNCATE TABLE lab_sample;
TRUNCATE TABLE lab_sampling_task;
TRUNCATE TABLE lab_sampling_plan;
TRUNCATE TABLE lab_monitoring_point;
TRUNCATE TABLE lab_sample_no_sequence;

-- Asset/document business ledgers.
TRUNCATE TABLE lab_instrument_maintenance;
TRUNCATE TABLE lab_instrument;
TRUNCATE TABLE lab_document_share;
TRUNCATE TABLE lab_document;

SET FOREIGN_KEY_CHECKS = 1;

-- Cleared table verification. All totals below should be 0.
SELECT 'lab_report_push_record' AS table_name, COUNT(*) AS total FROM lab_report_push_record
UNION ALL SELECT 'lab_report', COUNT(*) FROM lab_report
UNION ALL SELECT 'lab_review_record', COUNT(*) FROM lab_review_record
UNION ALL SELECT 'lab_detection_item', COUNT(*) FROM lab_detection_item
UNION ALL SELECT 'lab_detection_record', COUNT(*) FROM lab_detection_record
UNION ALL SELECT 'lab_sample', COUNT(*) FROM lab_sample
UNION ALL SELECT 'lab_sampling_task', COUNT(*) FROM lab_sampling_task
UNION ALL SELECT 'lab_sampling_plan', COUNT(*) FROM lab_sampling_plan
UNION ALL SELECT 'lab_monitoring_point', COUNT(*) FROM lab_monitoring_point
UNION ALL SELECT 'lab_sample_no_sequence', COUNT(*) FROM lab_sample_no_sequence
UNION ALL SELECT 'lab_instrument_maintenance', COUNT(*) FROM lab_instrument_maintenance
UNION ALL SELECT 'lab_instrument', COUNT(*) FROM lab_instrument
UNION ALL SELECT 'lab_document_share', COUNT(*) FROM lab_document_share
UNION ALL SELECT 'lab_document', COUNT(*) FROM lab_document;

-- Preserved table verification. These tables are intentionally not cleaned.
SELECT 'lab_role' AS table_name, COUNT(*) AS total FROM lab_role
UNION ALL SELECT 'lab_org', COUNT(*) FROM lab_org
UNION ALL SELECT 'lab_dict', COUNT(*) FROM lab_dict
UNION ALL SELECT 'lab_user', COUNT(*) FROM lab_user
UNION ALL SELECT 'lab_login_log', COUNT(*) FROM lab_login_log
UNION ALL SELECT 'lab_flow_config', COUNT(*) FROM lab_flow_config
UNION ALL SELECT 'lab_flow_node', COUNT(*) FROM lab_flow_node
UNION ALL SELECT 'lab_detection_project_group', COUNT(*) FROM lab_detection_project_group
UNION ALL SELECT 'lab_detection_parameter', COUNT(*) FROM lab_detection_parameter
UNION ALL SELECT 'lab_detection_method', COUNT(*) FROM lab_detection_method
UNION ALL SELECT 'lab_detection_method_instrument_model_binding', COUNT(*) FROM lab_detection_method_instrument_model_binding
UNION ALL SELECT 'lab_detection_type', COUNT(*) FROM lab_detection_type
UNION ALL SELECT 'lab_detection_step', COUNT(*) FROM lab_detection_step
UNION ALL SELECT 'lab_detection_assignment_memory', COUNT(*) FROM lab_detection_assignment_memory
UNION ALL SELECT 'lab_report_template', COUNT(*) FROM lab_report_template;
