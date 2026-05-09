ALTER TABLE lab_sampling_task
    ADD COLUMN task_no VARCHAR(64) NULL AFTER id,
    ADD COLUMN seal_no VARCHAR(64) NULL AFTER sample_type,
    ADD COLUMN sample_register_status VARCHAR(32) NULL AFTER seal_no,
    ADD COLUMN sample_id BIGINT NULL AFTER sample_register_status;

CREATE INDEX idx_lab_sampling_task_no ON lab_sampling_task (task_no);
CREATE INDEX idx_lab_sampling_task_seal_no ON lab_sampling_task (seal_no);
