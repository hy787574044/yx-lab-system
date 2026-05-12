-- 样品类型补充：新增“水源水”。

UPDATE lab_dict
SET item_text = CONCAT(
        'FACTORY=出厂水', CHAR(10),
        'RAW=原水', CHAR(10),
        'TERMINAL=管网末梢', CHAR(10),
        'SOURCE_WATER=水源水'
    ),
    updated_time = NOW()
WHERE dict_code = 'sample_type';
