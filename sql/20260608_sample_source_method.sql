ALTER TABLE lab_sample
    ADD COLUMN sample_source_method VARCHAR(32) NULL DEFAULT 'SAMPLING' COMMENT '样品来源：SAMPLING采样、DELIVERED送样、OUTSOURCED外采' AFTER sample_type;

UPDATE lab_sample
SET sample_source_method = COALESCE(NULLIF(sample_source_method, ''), 'SAMPLING')
WHERE deleted = 0;
