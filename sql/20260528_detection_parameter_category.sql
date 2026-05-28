-- Add required category classification for detection parameters.
ALTER TABLE lab_detection_parameter
    ADD COLUMN parameter_category VARCHAR(32) NOT NULL DEFAULT '实验室测定' COMMENT '参数类别：原位检测/现场测定/实验室测定'
    AFTER parameter_name;

UPDATE lab_detection_parameter
SET parameter_category = '原位检测'
WHERE deleted = 0
  AND parameter_name IN ('pH');

UPDATE lab_detection_parameter
SET parameter_category = '现场测定'
WHERE deleted = 0
  AND parameter_name IN ('浊度', '余氯', '色度', '臭和味', '肉眼可见物');

UPDATE lab_detection_parameter
SET parameter_category = '实验室测定'
WHERE deleted = 0
  AND (parameter_category IS NULL OR parameter_category = '');

INSERT INTO lab_dict (
    id, dict_code, dict_name, module_name, item_text, status, remark, deleted,
    created_name, updated_name
)
SELECT
    2026052801,
    'detection_parameter_category',
    '参数类别',
    '检测管理',
    'IN_SITU=原位检测\nFIELD=现场测定\nLABORATORY=实验室测定',
    1,
    '检测参数基础台账参数类别',
    0,
    'system',
    'system'
WHERE NOT EXISTS (
    SELECT 1 FROM lab_dict WHERE dict_code = 'detection_parameter_category'
);
