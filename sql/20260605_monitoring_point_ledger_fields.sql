INSERT INTO lab_dict (
    id, dict_code, dict_name, module_name, item_text, status, remark, deleted, created_name, updated_name
)
SELECT 860, 'water_plant', '所属水厂', '采样管理', 'EAST=城东水厂\nWEST=城西水厂', 1,
       '用于监测点台账所属水厂下拉配置', 0, 'system', 'system'
WHERE NOT EXISTS (
    SELECT 1 FROM lab_dict WHERE dict_code = 'water_plant'
);

UPDATE lab_dict
SET dict_name = '所属水厂',
    module_name = '采样管理',
    item_text = CASE
        WHEN item_text IS NULL OR item_text = '' THEN 'EAST=城东水厂\nWEST=城西水厂'
        ELSE item_text
    END,
    status = 1,
    remark = '用于监测点台账所属水厂下拉配置'
WHERE dict_code = 'water_plant';

ALTER TABLE lab_monitoring_point
    MODIFY COLUMN region_name VARCHAR(128) NULL COMMENT '所属水厂',
    DROP COLUMN service_population,
    DROP COLUMN frequency_type,
    DROP COLUMN owner_id,
    DROP COLUMN owner_name,
    DROP COLUMN contact_phone;
