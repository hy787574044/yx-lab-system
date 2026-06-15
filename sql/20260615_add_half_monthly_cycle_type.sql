-- 采样计划周期类型增加“半月”。
-- 用于已初始化数据库补齐 cycle_type 字典项；如果已包含 HALF_MONTHLY 则不重复追加。

UPDATE lab_dict
SET item_text = CONCAT(TRIM(TRAILING '\n' FROM COALESCE(item_text, '')), '\nHALF_MONTHLY=半月'),
    updated_time = NOW()
WHERE dict_code = 'cycle_type'
  AND deleted = 0
  AND COALESCE(item_text, '') NOT LIKE '%HALF_MONTHLY=%';
