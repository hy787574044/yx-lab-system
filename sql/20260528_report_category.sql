ALTER TABLE lab_report
    ADD COLUMN report_category VARCHAR(32) NOT NULL DEFAULT 'DETECTION_REPORT' AFTER report_type;

UPDATE lab_report
SET report_category = 'DETECTION_REPORT'
WHERE report_category IS NULL
   OR report_category = '';

ALTER TABLE lab_report
    ADD KEY idx_lab_report_category_status (report_category, report_status),
    ADD KEY idx_lab_report_sample_no (sample_no);
