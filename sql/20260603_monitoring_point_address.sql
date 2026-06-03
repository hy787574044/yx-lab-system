ALTER TABLE lab_monitoring_point
    ADD COLUMN address VARCHAR(255) NULL COMMENT '点位地址/地图名称' AFTER point_name;

UPDATE lab_monitoring_point
SET address = COALESCE(address, point_name)
WHERE deleted = 0;
