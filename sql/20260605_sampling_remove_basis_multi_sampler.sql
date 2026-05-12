ALTER TABLE lab_sampling_plan
    ADD COLUMN sampler_ids VARCHAR(255) NULL COMMENT '采样员ID集合' AFTER sampler_id,
    DROP COLUMN sampling_basis;

ALTER TABLE lab_sampling_task
    ADD COLUMN sampler_ids VARCHAR(255) NULL COMMENT '采样员ID集合' AFTER sampler_id,
    DROP COLUMN sampling_basis;

UPDATE lab_sampling_plan
SET sampler_ids = CONCAT(',', sampler_id, ',')
WHERE sampler_id IS NOT NULL
  AND (sampler_ids IS NULL OR sampler_ids = '')
  AND deleted = 0;

UPDATE lab_sampling_task
SET sampler_ids = CONCAT(',', sampler_id, ',')
WHERE sampler_id IS NOT NULL
  AND (sampler_ids IS NULL OR sampler_ids = '')
  AND deleted = 0;
