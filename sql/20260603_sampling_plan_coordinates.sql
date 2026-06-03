ALTER TABLE lab_sampling_plan
    MODIFY COLUMN point_id BIGINT NULL COMMENT '监测点位ID';

ALTER TABLE lab_sampling_task
    MODIFY COLUMN point_id BIGINT NULL COMMENT '监测点位ID';

ALTER TABLE lab_sampling_plan
    ADD COLUMN address VARCHAR(255) NULL COMMENT '点位地址/地图名称' AFTER point_name,
    ADD COLUMN latitude VARCHAR(32) NULL COMMENT '纬度' AFTER address,
    ADD COLUMN longitude VARCHAR(32) NULL COMMENT '经度' AFTER latitude;

UPDATE lab_sampling_plan plan
LEFT JOIN lab_monitoring_point point ON point.id = plan.point_id
SET plan.address = COALESCE(plan.address, point.address, point.point_name, plan.point_name),
    plan.latitude = COALESCE(plan.latitude, point.latitude),
    plan.longitude = COALESCE(plan.longitude, point.longitude)
WHERE plan.deleted = 0;

UPDATE lab_sampling_task task
LEFT JOIN lab_sampling_plan plan ON plan.id = task.plan_id
SET task.address = COALESCE(task.address, plan.address),
    task.latitude = COALESCE(task.latitude, plan.latitude),
    task.longitude = COALESCE(task.longitude, plan.longitude)
WHERE task.deleted = 0;
