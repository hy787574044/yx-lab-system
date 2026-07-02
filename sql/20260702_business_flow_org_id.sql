ALTER TABLE lab_sampling_plan
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER plan_name;

CREATE INDEX idx_lab_sampling_plan_org_id ON lab_sampling_plan (org_id);

ALTER TABLE lab_sampling_task
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER plan_id;

CREATE INDEX idx_lab_sampling_task_org_id ON lab_sampling_task (org_id);

ALTER TABLE lab_sample
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER task_id;

CREATE INDEX idx_lab_sample_org_id ON lab_sample (org_id);

ALTER TABLE lab_detection_record
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER sample_no;

CREATE INDEX idx_lab_detection_record_org_id ON lab_detection_record (org_id);

ALTER TABLE lab_detection_item
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER record_id;

CREATE INDEX idx_lab_detection_item_org_id ON lab_detection_item (org_id);

ALTER TABLE lab_review_record
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER sample_no;

CREATE INDEX idx_lab_review_record_org_id ON lab_review_record (org_id);

ALTER TABLE lab_report
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER sample_no;

CREATE INDEX idx_lab_report_org_id ON lab_report (org_id);

ALTER TABLE lab_report_push_record
    ADD COLUMN org_id BIGINT NULL COMMENT '所属机构ID' AFTER sample_no;

CREATE INDEX idx_lab_report_push_org_id ON lab_report_push_record (org_id);

UPDATE lab_sampling_plan plan
LEFT JOIN lab_monitoring_point point ON plan.point_id = point.id
SET plan.org_id = point.org_id
WHERE plan.org_id IS NULL
  AND point.org_id IS NOT NULL;

UPDATE lab_sampling_task task
JOIN lab_sampling_plan plan ON task.plan_id = plan.id
SET task.org_id = plan.org_id
WHERE task.org_id IS NULL
  AND plan.org_id IS NOT NULL;

UPDATE lab_sample sample
JOIN lab_sampling_task task ON sample.task_id = task.id
SET sample.org_id = task.org_id
WHERE sample.org_id IS NULL
  AND task.org_id IS NOT NULL;

UPDATE lab_detection_record record
JOIN lab_sample sample ON record.sample_id = sample.id
SET record.org_id = sample.org_id
WHERE record.org_id IS NULL
  AND sample.org_id IS NOT NULL;

UPDATE lab_detection_item item
JOIN lab_detection_record record ON item.record_id = record.id
SET item.org_id = record.org_id
WHERE item.org_id IS NULL
  AND record.org_id IS NOT NULL;

UPDATE lab_review_record review
JOIN lab_detection_record record ON review.detection_record_id = record.id
SET review.org_id = record.org_id
WHERE review.org_id IS NULL
  AND record.org_id IS NOT NULL;

UPDATE lab_report report
JOIN lab_detection_record record ON report.detection_record_id = record.id
SET report.org_id = record.org_id
WHERE report.org_id IS NULL
  AND record.org_id IS NOT NULL;

UPDATE lab_report_push_record push_record
JOIN lab_report report ON push_record.report_id = report.id
SET push_record.org_id = report.org_id
WHERE push_record.org_id IS NULL
  AND report.org_id IS NOT NULL;
