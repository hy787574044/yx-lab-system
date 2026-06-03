-- Replace legacy visible organization wording with "阳新" in existing data.
-- The public domain yangxin.yunhexx.com is intentionally not changed here.

SET @old_word = CONCAT('云', '河');
SET @new_word = '阳新';

UPDATE lab_org
SET org_name = REPLACE(org_name, @old_word, @new_word)
WHERE org_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_org
SET parent_name = REPLACE(parent_name, @old_word, @new_word)
WHERE parent_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_org
SET remark = REPLACE(remark, @old_word, @new_word)
WHERE remark LIKE CONCAT('%', @old_word, '%');

UPDATE lab_user
SET org_name = REPLACE(org_name, @old_word, @new_word)
WHERE org_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_monitoring_point
SET point_name = REPLACE(point_name, @old_word, @new_word)
WHERE point_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_monitoring_point
SET region_name = REPLACE(region_name, @old_word, @new_word)
WHERE region_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_plan
SET point_name = REPLACE(point_name, @old_word, @new_word)
WHERE point_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_plan
SET detection_config_snapshot = REPLACE(detection_config_snapshot, @old_word, @new_word)
WHERE detection_config_snapshot LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_plan
SET sampling_basis = REPLACE(sampling_basis, @old_word, @new_word)
WHERE sampling_basis LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_plan
SET remark = REPLACE(remark, @old_word, @new_word)
WHERE remark LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_task
SET point_name = REPLACE(point_name, @old_word, @new_word)
WHERE point_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_task
SET detection_config_snapshot = REPLACE(detection_config_snapshot, @old_word, @new_word)
WHERE detection_config_snapshot LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_task
SET sampling_basis = REPLACE(sampling_basis, @old_word, @new_word)
WHERE sampling_basis LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_task
SET onsite_metrics = REPLACE(onsite_metrics, @old_word, @new_word)
WHERE onsite_metrics LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sampling_task
SET remark = REPLACE(remark, @old_word, @new_word)
WHERE remark LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sample
SET point_name = REPLACE(point_name, @old_word, @new_word)
WHERE point_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sample
SET detection_config_snapshot = REPLACE(detection_config_snapshot, @old_word, @new_word)
WHERE detection_config_snapshot LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sample
SET remark = REPLACE(remark, @old_word, @new_word)
WHERE remark LIKE CONCAT('%', @old_word, '%');

UPDATE lab_sample
SET trace_log = REPLACE(trace_log, @old_word, @new_word)
WHERE trace_log LIKE CONCAT('%', @old_word, '%');

UPDATE lab_report_template
SET template_name = REPLACE(template_name, @old_word, @new_word)
WHERE template_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_report_template
SET template_content = REPLACE(template_content, @old_word, @new_word)
WHERE template_content LIKE CONCAT('%', @old_word, '%');

UPDATE lab_report_template
SET remark = REPLACE(remark, @old_word, @new_word)
WHERE remark LIKE CONCAT('%', @old_word, '%');

UPDATE lab_report
SET report_name = REPLACE(report_name, @old_word, @new_word)
WHERE report_name LIKE CONCAT('%', @old_word, '%');

UPDATE lab_report
SET content_snapshot = REPLACE(content_snapshot, @old_word, @new_word)
WHERE content_snapshot LIKE CONCAT('%', @old_word, '%');

UPDATE lab_dict
SET item_text = REPLACE(item_text, @old_word, @new_word)
WHERE item_text LIKE CONCAT('%', @old_word, '%');

UPDATE lab_dict
SET remark = REPLACE(remark, @old_word, @new_word)
WHERE remark LIKE CONCAT('%', @old_word, '%');
