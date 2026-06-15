-- 监测点位类型与样品类型保持一致。
-- 已有数据库执行后，新增/编辑监测点位的点位类型下拉为：水源水、滤前水、出厂水、末梢水。
UPDATE lab_dict
SET item_text = 'SOURCE_WATER=水源水
RAW=滤前水
FACTORY=出厂水
TERMINAL=末梢水',
    updated_time = NOW()
WHERE dict_code = 'point_type'
  AND deleted = 0;
