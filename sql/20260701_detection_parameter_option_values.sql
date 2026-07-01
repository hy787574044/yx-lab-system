-- 检测参数支持文本选项值（如 臭和味：无异臭、异味 / 有异臭、异味）
ALTER TABLE lab_detection_parameter ADD COLUMN option_values VARCHAR(1000) NULL COMMENT '文本选项值JSON数组，例如 ["无异臭、异味","有异臭、异味"]';
ALTER TABLE lab_detection_item ADD COLUMN option_values VARCHAR(1000) NULL COMMENT '检测项文本选项值快照';
