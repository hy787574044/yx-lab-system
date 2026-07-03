ALTER TABLE lab_sample
    ADD COLUMN sampler_ids VARCHAR(255) NULL COMMENT '采样员ID集合' AFTER sampler_id;

UPDATE lab_sample
SET sampler_ids = CONCAT(',', sampler_id, ',')
WHERE sampler_id IS NOT NULL
  AND (sampler_ids IS NULL OR sampler_ids = '')
  AND deleted = 0;
