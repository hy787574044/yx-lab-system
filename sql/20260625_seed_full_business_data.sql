-- ============================================
-- 完整业务流程测试数据
-- 包含：监测点位、采样计划、采样任务、样品、检测、审查、报告
-- 每个节点至少10条，覆盖各种状态
-- ============================================

USE yx_lab;

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 监测点位（12条，覆盖各种类型和状态）
-- ============================================
DELETE FROM lab_monitoring_point;

INSERT INTO lab_monitoring_point (id, point_name, address, longitude, latitude, org_id, service_population, frequency_type, owner_id, owner_name, contact_phone, point_type, point_status, created_by, created_name, updated_by, updated_name) VALUES
(2001, '城东水厂出厂水', '阳新县城东片区水厂', '115.2121', '30.2211', 802, 36000, 'DAILY', 1002, '员工', '13800000001', 'FACTORY', 'ENABLED', 1001, 'system', 1001, 'system'),
(2002, '城西水厂出厂水', '阳新县城西片区水厂', '115.1985', '30.2156', 802, 28000, 'DAILY', 1002, '员工', '13800000001', 'FACTORY', 'ENABLED', 1001, 'system', 1001, 'system'),
(2003, '城南水厂出厂水', '阳新县城南片区水厂', '115.2234', '30.2089', 802, 25000, 'DAILY', 1002, '员工', '13800000001', 'FACTORY', 'ENABLED', 1001, 'system', 1001, 'system'),
(2004, '城北水厂出厂水', '阳新县城北片区水厂', '115.2067', '30.2345', 803, 22000, 'WEEKLY', 1002, '员工', '13800000001', 'FACTORY', 'ENABLED', 1001, 'system', 1001, 'system'),
(2005, '城东水源地', '阳新县城东取水口', '115.2156', '30.2198', 802, 0, 'DAILY', 1002, '员工', '13800000001', 'SOURCE_WATER', 'ENABLED', 1001, 'system', 1001, 'system'),
(2006, '城西水源地', '阳新县城西取水口', '115.1923', '30.2178', 802, 0, 'DAILY', 1002, '员工', '13800000001', 'SOURCE_WATER', 'ENABLED', 1001, 'system', 1001, 'system'),
(2007, '县政府末梢水', '阳新县政府大楼', '115.2189', '30.2256', 802, 5000, 'WEEKLY', 1002, '员工', '13800000001', 'TERMINAL', 'ENABLED', 1001, 'system', 1001, 'system'),
(2008, '人民医院末梢水', '阳新县人民医院', '115.2145', '30.2234', 802, 3000, 'WEEKLY', 1002, '员工', '13800000001', 'TERMINAL', 'ENABLED', 1001, 'system', 1001, 'system'),
(2009, '实验小学末梢水', '阳新县实验小学', '115.2098', '30.2189', 803, 2000, 'MONTHLY', 1002, '员工', '13800000001', 'TERMINAL', 'ENABLED', 1001, 'system', 1001, 'system'),
(2010, '工业园末梢水', '阳新县工业园', '115.2267', '30.2123', 803, 8000, 'WEEKLY', 1002, '员工', '13800000001', 'TERMINAL', 'ENABLED', 1001, 'system', 1001, 'system'),
(2011, '火车站末梢水', '阳新县火车站', '115.2312', '30.2156', 803, 4000, 'MONTHLY', 1002, '员工', '13800000001', 'TERMINAL', 'DISABLED', 1001, 'system', 1001, 'system'),
(2012, '待建水厂', '规划中', '115.2400', '30.2200', 803, 0, 'MONTHLY', 1002, '员工', '13800000001', 'FACTORY', 'DISABLED', 1001, 'system', 1001, 'system');

-- ============================================
-- 2. 检测套餐（3个）
-- ============================================
DELETE FROM lab_detection_type;

INSERT INTO lab_detection_type (id, type_name, group_id, group_name, detector_id, detector_name, parameter_ids, parameter_names, parameter_method_bindings, enabled, created_by, created_name, updated_by, updated_name) VALUES
(3101, '日检九项', NULL, NULL, 1002, '员工', '3001,3002,3003,3004', 'pH,浊度,余氯,氨氮', NULL, 1, 1001, 'system', 1001, 'system'),
(3102, '月检全项', NULL, NULL, 1002, '员工', '3001,3002,3003,3004', 'pH,浊度,余氯,氨氮', NULL, 1, 1001, 'system', 1001, 'system'),
(3103, '水源水检测', NULL, NULL, 1002, '员工', '3001,3002,3004', 'pH,浊度,氨氮', NULL, 1, 1001, 'system', 1001, 'system');

-- ============================================
-- 3. 采样计划（12条，覆盖各种状态）
-- ============================================
DELETE FROM lab_sampling_plan;

INSERT INTO lab_sampling_plan (id, plan_name, point_id, point_name, address, latitude, longitude, start_time, end_time, sampler_id, sampler_ids, sampler_name, sampling_type, detection_type_id, detection_type_name, detection_config_snapshot, cycle_type, plan_status, remark, deleted, created_by, created_name, updated_by, updated_name) VALUES
(4001, '城东水厂日检计划', 2001, '城东水厂出厂水', '阳新县城东片区水厂', '30.2211', '115.2121', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3101, '日检九项', NULL, 'DAILY', 'ACTIVE', '每日采样检测', 0, 1001, 'system', 1001, 'system'),
(4002, '城西水厂日检计划', 2002, '城西水厂出厂水', '阳新县城西片区水厂', '30.2156', '115.1985', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3101, '日检九项', NULL, 'DAILY', 'ACTIVE', '每日采样检测', 0, 1001, 'system', 1001, 'system'),
(4003, '城南水厂日检计划', 2003, '城南水厂出厂水', '阳新县城南片区水厂', '30.2089', '115.2234', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3101, '日检九项', NULL, 'DAILY', 'ACTIVE', '每日采样检测', 0, 1001, 'system', 1001, 'system'),
(4004, '城北水厂周检计划', 2004, '城北水厂出厂水', '阳新县城北片区水厂', '30.2345', '115.2067', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3102, '月检全项', NULL, 'WEEKLY', 'ACTIVE', '每周采样检测', 0, 1001, 'system', 1001, 'system'),
(4005, '城东水源地日检计划', 2005, '城东水源地', '阳新县城东取水口', '30.2198', '115.2156', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3103, '水源水检测', NULL, 'DAILY', 'ACTIVE', '每日水源检测', 0, 1001, 'system', 1001, 'system'),
(4006, '城西水源地日检计划', 2006, '城西水源地', '阳新县城西取水口', '30.2178', '115.1923', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3103, '水源水检测', NULL, 'DAILY', 'ACTIVE', '每日水源检测', 0, 1001, 'system', 1001, 'system'),
(4007, '县政府末梢水周检', 2007, '县政府末梢水', '阳新县政府大楼', '30.2256', '115.2189', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3101, '日检九项', NULL, 'WEEKLY', 'ACTIVE', '每周末梢水检测', 0, 1001, 'system', 1001, 'system'),
(4008, '人民医院末梢水周检', 2008, '人民医院末梢水', '阳新县人民医院', '30.2234', '115.2145', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3101, '日检九项', NULL, 'WEEKLY', 'ACTIVE', '每周末梢水检测', 0, 1001, 'system', 1001, 'system'),
(4009, '实验小学月检计划', 2009, '实验小学末梢水', '阳新县实验小学', '30.2189', '115.2098', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3102, '月检全项', NULL, 'MONTHLY', 'ACTIVE', '每月检测', 0, 1001, 'system', 1001, 'system'),
(4010, '工业园周检计划', 2010, '工业园末梢水', '阳新县工业园', '30.2123', '115.2267', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3101, '日检九项', NULL, 'WEEKLY', 'ACTIVE', '每周检测', 0, 1001, 'system', 1001, 'system'),
(4011, '暂停的计划', 2001, '城东水厂出厂水', '阳新县城东片区水厂', '30.2211', '115.2121', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1002, '1002', '员工', 'ROUTINE', 3101, '日检九项', NULL, 'DAILY', 'PAUSED', '已暂停的计划', 0, 1001, 'system', 1001, 'system'),
(4012, '待派发的计划', 2002, '城西水厂出厂水', '阳新县城西片区水厂', '30.2156', '115.1985', '2026-07-01 00:00:00', '2026-12-31 23:59:59', NULL, NULL, NULL, 'ROUTINE', 3101, '日检九项', NULL, 'DAILY', 'UNPUBLISHED', '待派发', 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 4. 采样任务（15条，覆盖各种状态）
-- ============================================
DELETE FROM lab_sampling_task;

INSERT INTO lab_sampling_task (id, task_no, sample_no, plan_id, point_id, point_name, sampling_time, sampler_id, sampler_ids, sampler_name, sample_register_status, sample_id, detection_items, detection_type_id, detection_type_name, detection_config_snapshot, task_status, started_time, onsite_metrics, weather, temperature, sample_total_volume, sample_bottle_count, photo_urls, abandon_reason, finished_time, remark, deleted, created_by, created_name, updated_by, updated_name) VALUES
-- 已完成且已登记的任务（对应已登录的样品）
(5001, 'TASK202606250001', 'YX202606250001', 4001, 2001, '城东水厂出厂水', '2026-06-25 08:00:00', 1002, '1002', '员工', 'REGISTERED', 6001, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'COMPLETED', '2026-06-25 07:30:00', NULL, '晴', '28', '5L', '2', NULL, NULL, '2026-06-25 09:00:00', '正常采样', 0, 1001, 'system', 1001, 'system'),
(5002, 'TASK202606250002', 'YX202606250002', 4002, 2002, '城西水厂出厂水', '2026-06-25 08:30:00', 1002, '1002', '员工', 'REGISTERED', 6002, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'COMPLETED', '2026-06-25 08:00:00', NULL, '晴', '27', '5L', '2', NULL, NULL, '2026-06-25 09:30:00', '正常采样', 0, 1001, 'system', 1001, 'system'),
(5003, 'TASK202606250003', 'YX202606250003', 4003, 2003, '城南水厂出厂水', '2026-06-25 09:00:00', 1002, '1002', '员工', 'REGISTERED', 6003, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'COMPLETED', '2026-06-25 08:30:00', NULL, '多云', '26', '5L', '2', NULL, NULL, '2026-06-25 10:00:00', '正常采样', 0, 1001, 'system', 1001, 'system'),
(5004, 'TASK202606250004', 'YX202606250004', 4005, 2005, '城东水源地', '2026-06-25 07:00:00', 1002, '1002', '员工', 'REGISTERED', 6004, 'pH,浊度,氨氮', 3103, '水源水检测', NULL, 'COMPLETED', '2026-06-25 06:30:00', NULL, '晴', '25', '10L', '3', NULL, NULL, '2026-06-25 08:00:00', '水源采样', 0, 1001, 'system', 1001, 'system'),
-- 待执行的任务
(5005, 'TASK202606260001', NULL, 4001, 2001, '城东水厂出厂水', '2026-06-26 08:00:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '明日采样', 0, 1001, 'system', 1001, 'system'),
(5006, 'TASK202606260002', NULL, 4002, 2002, '城西水厂出厂水', '2026-06-26 08:30:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '明日采样', 0, 1001, 'system', 1001, 'system'),
(5007, 'TASK202606260003', NULL, 4003, 2003, '城南水厂出厂水', '2026-06-26 09:00:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '明日采样', 0, 1001, 'system', 1001, 'system'),
(5008, 'TASK202606260004', NULL, 4007, 2007, '县政府末梢水', '2026-06-26 10:00:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '明日采样', 0, 1001, 'system', 1001, 'system'),
-- 执行中的任务
(5009, 'TASK202606250005', NULL, 4004, 2004, '城北水厂出厂水', '2026-06-25 10:00:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,余氯,氨氮', 3102, '月检全项', NULL, 'IN_PROGRESS', '2026-06-25 09:30:00', NULL, '晴', '29', NULL, NULL, NULL, NULL, NULL, '正在采样中', 0, 1001, 'system', 1001, 'system'),
(5010, 'TASK202606250006', NULL, 4006, 2006, '城西水源地', '2026-06-25 07:30:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,氨氮', 3103, '水源水检测', NULL, 'IN_PROGRESS', '2026-06-25 07:00:00', NULL, '晴', '24', NULL, NULL, NULL, NULL, NULL, '正在采样中', 0, 1001, 'system', 1001, 'system'),
-- 已废弃的任务
(5011, 'TASK202606240001', NULL, 4001, 2001, '城东水厂出厂水', '2026-06-24 08:00:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'ABANDONED', '2026-06-24 07:30:00', NULL, '暴雨', '22', NULL, NULL, NULL, '暴雨天气无法采样', NULL, '天气原因废弃', 0, 1001, 'system', 1001, 'system'),
(5012, 'TASK202606240002', NULL, 4002, 2002, '城西水厂出厂水', '2026-06-24 08:30:00', 1002, '1002', '员工', NULL, NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'ABANDONED', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '设备故障', NULL, '设备故障废弃', 0, 1001, 'system', 1001, 'system'),
-- 已完成但未登记的任务（等待样品登录）
(5013, 'TASK202606250007', 'YX202606250005', 4008, 2008, '人民医院末梢水', '2026-06-25 10:30:00', 1002, '1002', '员工', 'UNREGISTERED', NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'COMPLETED', '2026-06-25 10:00:00', NULL, '多云', '27', '5L', '2', NULL, NULL, '2026-06-25 11:00:00', '已完成待登记', 0, 1001, 'system', 1001, 'system'),
(5014, 'TASK202606250008', 'YX202606250006', 4009, 2009, '实验小学末梢水', '2026-06-25 11:00:00', 1002, '1002', '员工', 'UNREGISTERED', NULL, 'pH,浊度,余氯,氨氮', 3102, '月检全项', NULL, 'COMPLETED', '2026-06-25 10:30:00', NULL, '晴', '28', '5L', '2', NULL, NULL, '2026-06-25 12:00:00', '已完成待登记', 0, 1001, 'system', 1001, 'system'),
(5015, 'TASK202606250009', 'YX202606250007', 4010, 2010, '工业园末梢水', '2026-06-25 11:30:00', 1002, '1002', '员工', 'UNREGISTERED', NULL, 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 'COMPLETED', '2026-06-25 11:00:00', NULL, '晴', '29', '5L', '2', NULL, NULL, '2026-06-25 12:30:00', '已完成待登记', 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 5. 样品（12条，覆盖各种状态）
-- ============================================
DELETE FROM lab_sample;

INSERT INTO lab_sample (id, sample_no, task_id, point_id, point_name, sample_source_method, detection_items, detection_type_id, detection_type_name, detection_config_snapshot, review_flow_id, review_flow_name, sampling_time, sample_total_volume, sample_bottle_count, sampler_id, sampler_name, weather, storage_condition, sample_status, result_summary, remark, trace_log, deleted, created_by, created_name, updated_by, updated_name) VALUES
-- 已登录的样品（等待检测）
(6001, 'YX202606250001', 5001, 2001, '城东水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-25 08:00:00', '5L', '2', 1002, '员工', '晴', '冷藏', 'LOGGED', NULL, '正常登录', '2026-06-25 09:00:00 样品登录', 0, 1001, 'system', 1001, 'system'),
(6002, 'YX202606250002', 5002, 2002, '城西水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-25 08:30:00', '5L', '2', 1002, '员工', '晴', '冷藏', 'LOGGED', NULL, '正常登录', '2026-06-25 09:30:00 样品登录', 0, 1001, 'system', 1001, 'system'),
(6003, 'YX202606250003', 5003, 2003, '城南水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-25 09:00:00', '5L', '2', 1002, '员工', '多云', '冷藏', 'LOGGED', NULL, '正常登录', '2026-06-25 10:00:00 样品登录', 0, 1001, 'system', 1001, 'system'),
(6004, 'YX202606250004', 5004, 2005, '城东水源地', 'SAMPLING', 'pH,浊度,氨氮', 3103, '水源水检测', NULL, 9601, '常规三级审核', '2026-06-25 07:00:00', '10L', '3', 1002, '员工', '晴', '冷藏', 'LOGGED', NULL, '水源采样', '2026-06-25 08:00:00 样品登录', 0, 1001, 'system', 1001, 'system'),
-- 审核中的样品（检测已提交）
(6005, 'YX202606240001', NULL, 2001, '城东水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-24 08:00:00', '5L', '2', 1002, '员工', '晴', '冷藏', 'REVIEWING', '正常', '检测已提交', '2026-06-24 09:00:00 样品登录\n2026-06-24 14:00:00 检测结果已提交', 0, 1001, 'system', 1001, 'system'),
(6006, 'YX202606240002', NULL, 2002, '城西水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-24 08:30:00', '5L', '2', 1002, '员工', '晴', '冷藏', 'REVIEWING', '正常', '检测已提交', '2026-06-24 09:30:00 样品登录\n2026-06-24 15:00:00 检测结果已提交', 0, 1001, 'system', 1001, 'system'),
(6007, 'YX202606240003', NULL, 2003, '城南水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-24 09:00:00', '5L', '2', 1002, '员工', '多云', '冷藏', 'REVIEWING', '异常', '浊度超标', '2026-06-24 10:00:00 样品登录\n2026-06-24 16:00:00 检测结果已提交', 0, 1001, 'system', 1001, 'system'),
-- 待重检的样品（审核驳回）
(6008, 'YX202606230001', NULL, 2001, '城东水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-23 08:00:00', '5L', '2', 1002, '员工', '晴', '冷藏', 'RETEST', '异常', '初审驳回-浊度超标', '2026-06-23 09:00:00 样品登录\n2026-06-23 14:00:00 检测结果已提交\n2026-06-23 16:00:00 审核驳回：浊度超标需重检', 0, 1001, 'system', 1001, 'system'),
(6009, 'YX202606230002', NULL, 2005, '城东水源地', 'SAMPLING', 'pH,浊度,氨氮', 3103, '水源水检测', NULL, 9601, '常规三级审核', '2026-06-23 07:00:00', '10L', '3', 1002, '员工', '晴', '冷藏', 'RETEST', '异常', '复审驳回-氨氮偏高', '2026-06-23 08:00:00 样品登录\n2026-06-23 12:00:00 检测结果已提交\n2026-06-23 17:00:00 审核驳回：氨氮偏高需重检', 0, 1001, 'system', 1001, 'system'),
-- 已完成的样品（审核通过）
(6010, 'YX202606220001', NULL, 2001, '城东水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-22 08:00:00', '5L', '2', 1002, '员工', '晴', '冷藏', 'COMPLETED', '正常', '审核通过', '2026-06-22 09:00:00 样品登录\n2026-06-22 14:00:00 检测结果已提交\n2026-06-22 16:00:00 初审通过\n2026-06-22 17:00:00 复审通过\n2026-06-22 18:00:00 终审通过', 0, 1001, 'system', 1001, 'system'),
(6011, 'YX202606220002', NULL, 2002, '城西水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-22 08:30:00', '5L', '2', 1002, '员工', '晴', '冷藏', 'COMPLETED', '正常', '审核通过', '2026-06-22 09:30:00 样品登录\n2026-06-22 15:00:00 检测结果已提交\n2026-06-22 16:30:00 初审通过\n2026-06-22 17:30:00 复审通过\n2026-06-22 18:30:00 终审通过', 0, 1001, 'system', 1001, 'system'),
(6012, 'YX202606220003', NULL, 2003, '城南水厂出厂水', 'SAMPLING', 'pH,浊度,余氯,氨氮', 3101, '日检九项', NULL, 9601, '常规三级审核', '2026-06-22 09:00:00', '5L', '2', 1002, '员工', '多云', '冷藏', 'COMPLETED', '正常', '审核通过', '2026-06-22 10:00:00 样品登录\n2026-06-22 16:00:00 检测结果已提交\n2026-06-22 17:00:00 初审通过\n2026-06-22 18:00:00 复审通过\n2026-06-22 19:00:00 终审通过', 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 6. 检测记录（12条，覆盖各种状态）
-- ============================================
DELETE FROM lab_detection_record;

INSERT INTO lab_detection_record (id, sample_id, sample_no, detection_type_id, detection_type_name, detection_time, detector_id, detector_name, detection_result, abnormal_remark, remark, detection_status, deleted, created_by, created_name, updated_by, updated_name) VALUES
-- 待分配的检测记录
(7001, 6001, 'YX202606250001', 3101, '日检九项', '2026-06-25 09:00:00', NULL, NULL, NULL, '待分配检测员', NULL, 'WAIT_ASSIGN', 0, 1001, 'system', 1001, 'system'),
(7002, 6002, 'YX202606250002', 3101, '日检九项', '2026-06-25 09:30:00', NULL, NULL, NULL, '待分配检测员', NULL, 'WAIT_ASSIGN', 0, 1001, 'system', 1001, 'system'),
-- 待检测的检测记录（已分配检测员）
(7003, 6003, 'YX202606250003', 3101, '日检九项', '2026-06-25 10:00:00', 1002, '员工', NULL, '已默认分配采样员检测', NULL, 'WAIT_DETECT', 0, 1001, 'system', 1001, 'system'),
(7004, 6004, 'YX202606250004', 3103, '水源水检测', '2026-06-25 08:00:00', 1002, '员工', NULL, '已默认分配采样员检测', NULL, 'WAIT_DETECT', 0, 1001, 'system', 1001, 'system'),
-- 待审核的检测记录（已提交结果）
(7005, 6005, 'YX202606240001', 3101, '日检九项', '2026-06-24 14:00:00', 1002, '员工', 'NORMAL', NULL, '检测完成', 'SUBMITTED', 0, 1001, 'system', 1001, 'system'),
(7006, 6006, 'YX202606240002', 3101, '日检九项', '2026-06-24 15:00:00', 1002, '员工', 'NORMAL', NULL, '检测完成', 'SUBMITTED', 0, 1001, 'system', 1001, 'system'),
(7007, 6007, 'YX202606240003', 3101, '日检九项', '2026-06-24 16:00:00', 1002, '员工', 'ABNORMAL', '浊度超标', '浊度1.2NTU超出标准', 'SUBMITTED', 0, 1001, 'system', 1001, 'system'),
-- 已通过的检测记录
(7008, 6010, 'YX202606220001', 3101, '日检九项', '2026-06-22 14:00:00', 1002, '员工', 'NORMAL', NULL, '检测完成', 'APPROVED', 0, 1001, 'system', 1001, 'system'),
(7009, 6011, 'YX202606220002', 3101, '日检九项', '2026-06-22 15:00:00', 1002, '员工', 'NORMAL', NULL, '检测完成', 'APPROVED', 0, 1001, 'system', 1001, 'system'),
(7010, 6012, 'YX202606220003', 3101, '日检九项', '2026-06-22 16:00:00', 1002, '员工', 'NORMAL', NULL, '检测完成', 'APPROVED', 0, 1001, 'system', 1001, 'system'),
-- 已驳回的检测记录
(7011, 6008, 'YX202606230001', 3101, '日检九项', '2026-06-23 14:00:00', 1002, '员工', 'ABNORMAL', '浊度超标', '浊度1.5NTU超出标准', 'REJECTED', 0, 1001, 'system', 1001, 'system'),
(7012, 6009, 'YX202606230002', 3103, '水源水检测', '2026-06-23 12:00:00', 1002, '员工', 'ABNORMAL', '氨氮偏高', '氨氮0.8mg/L超出标准', 'REJECTED', 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 7. 检测项（48条，每个检测记录4个参数）
-- ============================================
DELETE FROM lab_detection_item;

-- 待分配的检测项（7001, 7002）
INSERT INTO lab_detection_item (id, record_id, parameter_id, parameter_name, standard_min, standard_max, result_value, unit, reference_standard, method_id, method_name, detector_id, detector_name, item_status, exceed_flag, deleted, created_by, created_name, updated_by, updated_name) VALUES
(8001, 7001, 3001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),
(8002, 7001, 3002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),
(8003, 7001, 3003, '余氯', 0.05, 2.00, NULL, 'mg/L', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),
(8004, 7001, 3004, '氨氮', 0.00, 0.50, NULL, 'mg/L', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),
(8005, 7002, 3001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),
(8006, 7002, 3002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),
(8007, 7002, 3003, '余氯', 0.05, 2.00, NULL, 'mg/L', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),
(8008, 7002, 3004, '氨氮', 0.00, 0.50, NULL, 'mg/L', 'GB 5749-2022', NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1001, 'system', 1001, 'system'),

-- 待检测的检测项（7003, 7004）
(8009, 7003, 3001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'WAIT_DETECT', 0, 0, 1001, 'system', 1001, 'system'),
(8010, 7003, 3002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'WAIT_DETECT', 0, 0, 1001, 'system', 1001, 'system'),
(8011, 7003, 3003, '余氯', 0.05, 2.00, NULL, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'WAIT_DETECT', 0, 0, 1001, 'system', 1001, 'system'),
(8012, 7003, 3004, '氨氮', 0.00, 0.50, NULL, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'WAIT_DETECT', 0, 0, 1001, 'system', 1001, 'system'),
(8013, 7004, 3001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'WAIT_DETECT', 0, 0, 1001, 'system', 1001, 'system'),
(8014, 7004, 3002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'WAIT_DETECT', 0, 0, 1001, 'system', 1001, 'system'),
(8015, 7004, 3004, '氨氮', 0.00, 0.50, NULL, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'WAIT_DETECT', 0, 0, 1001, 'system', 1001, 'system'),

-- 待审核的检测项（7005, 7006, 7007）
(8016, 7005, 3001, 'pH', 6.50, 8.50, 7.20, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8017, 7005, 3002, '浊度', 0.00, 1.00, 0.50, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8018, 7005, 3003, '余氯', 0.05, 2.00, 0.80, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8019, 7005, 3004, '氨氮', 0.00, 0.50, 0.15, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8020, 7006, 3001, 'pH', 6.50, 8.50, 7.30, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8021, 7006, 3002, '浊度', 0.00, 1.00, 0.45, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8022, 7006, 3003, '余氯', 0.05, 2.00, 0.90, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8023, 7006, 3004, '氨氮', 0.00, 0.50, 0.20, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8024, 7007, 3001, 'pH', 6.50, 8.50, 7.10, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8025, 7007, 3002, '浊度', 0.00, 1.00, 1.20, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 1, 0, 1001, 'system', 1001, 'system'),
(8026, 7007, 3003, '余氯', 0.05, 2.00, 0.75, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),
(8027, 7007, 3004, '氨氮', 0.00, 0.50, 0.18, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'SUBMITTED', 0, 0, 1001, 'system', 1001, 'system'),

-- 已通过的检测项（7008, 7009, 7010）
(8028, 7008, 3001, 'pH', 6.50, 8.50, 7.25, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8029, 7008, 3002, '浊度', 0.00, 1.00, 0.35, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8030, 7008, 3003, '余氯', 0.05, 2.00, 0.85, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8031, 7008, 3004, '氨氮', 0.00, 0.50, 0.12, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8032, 7009, 3001, 'pH', 6.50, 8.50, 7.18, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8033, 7009, 3002, '浊度', 0.00, 1.00, 0.42, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8034, 7009, 3003, '余氯', 0.05, 2.00, 0.78, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8035, 7009, 3004, '氨氮', 0.00, 0.50, 0.22, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8036, 7010, 3001, 'pH', 6.50, 8.50, 7.32, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8037, 7010, 3002, '浊度', 0.00, 1.00, 0.38, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8038, 7010, 3003, '余氯', 0.05, 2.00, 0.92, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),
(8039, 7010, 3004, '氨氮', 0.00, 0.50, 0.16, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'APPROVED', 0, 0, 1001, 'system', 1001, 'system'),

-- 已驳回的检测项（7011, 7012）
(8040, 7011, 3001, 'pH', 6.50, 8.50, 7.15, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'REJECTED', 0, 0, 1001, 'system', 1001, 'system'),
(8041, 7011, 3002, '浊度', 0.00, 1.00, 1.50, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'REJECTED', 1, 0, 1001, 'system', 1001, 'system'),
(8042, 7011, 3003, '余氯', 0.05, 2.00, 0.65, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'REJECTED', 0, 0, 1001, 'system', 1001, 'system'),
(8043, 7011, 3004, '氨氮', 0.00, 0.50, 0.25, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'REJECTED', 0, 0, 1001, 'system', 1001, 'system'),
(8044, 7012, 3001, 'pH', 6.50, 8.50, 7.08, '', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'REJECTED', 0, 0, 1001, 'system', 1001, 'system'),
(8045, 7012, 3002, '浊度', 0.00, 1.00, 0.55, 'NTU', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'REJECTED', 0, 0, 1001, 'system', 1001, 'system'),
(8046, 7012, 3004, '氨氮', 0.00, 0.50, 0.80, 'mg/L', 'GB 5749-2022', NULL, NULL, 1002, '员工', 'REJECTED', 1, 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 8. 审查记录（15条，覆盖各审核节点）
-- ============================================
DELETE FROM lab_review_record;

INSERT INTO lab_review_record (id, detection_record_id, sample_id, sample_no, flow_id, flow_node_id, flow_node_name, flow_node_order, required_flag, reviewer_id, reviewer_name, review_time, review_result, reject_reason, review_remark, deleted, created_by, created_name, updated_by, updated_name) VALUES
-- 待审核的样品（7005, 7006, 7007）- 暂无审查记录
-- 已通过的样品（7008）- 初审、复审、终审全部通过
(9001, 7008, 6010, 'YX202606220001', 9601, 9611, '初审', 1, 1, 1003, '主任', '2026-06-22 16:00:00', 'APPROVED', NULL, '初审通过', 0, 1001, 'system', 1001, 'system'),
(9002, 7008, 6010, 'YX202606220001', 9601, 9612, '复审', 2, 1, 1003, '主任', '2026-06-22 17:00:00', 'APPROVED', NULL, '复审通过', 0, 1001, 'system', 1001, 'system'),
(9003, 7008, 6010, 'YX202606220001', 9601, 9613, '终审', 3, 1, 1003, '主任', '2026-06-22 18:00:00', 'APPROVED', NULL, '终审通过', 0, 1001, 'system', 1001, 'system'),

-- 已通过的样品（7009）- 初审、复审、终审全部通过
(9004, 7009, 6011, 'YX202606220002', 9601, 9611, '初审', 1, 1, 1003, '主任', '2026-06-22 16:30:00', 'APPROVED', NULL, '初审通过', 0, 1001, 'system', 1001, 'system'),
(9005, 7009, 6011, 'YX202606220002', 9601, 9612, '复审', 2, 1, 1003, '主任', '2026-06-22 17:30:00', 'APPROVED', NULL, '复审通过', 0, 1001, 'system', 1001, 'system'),
(9006, 7009, 6011, 'YX202606220002', 9601, 9613, '终审', 3, 1, 1003, '主任', '2026-06-22 18:30:00', 'APPROVED', NULL, '终审通过', 0, 1001, 'system', 1001, 'system'),

-- 已通过的样品（7010）- 初审、复审、终审全部通过
(9007, 7010, 6012, 'YX202606220003', 9601, 9611, '初审', 1, 1, 1003, '主任', '2026-06-22 17:00:00', 'APPROVED', NULL, '初审通过', 0, 1001, 'system', 1001, 'system'),
(9008, 7010, 6012, 'YX202606220003', 9601, 9612, '复审', 2, 1, 1003, '主任', '2026-06-22 18:00:00', 'APPROVED', NULL, '复审通过', 0, 1001, 'system', 1001, 'system'),
(9009, 7010, 6012, 'YX202606220003', 9601, 9613, '终审', 3, 1, 1003, '主任', '2026-06-22 19:00:00', 'APPROVED', NULL, '终审通过', 0, 1001, 'system', 1001, 'system'),

-- 已驳回的样品（7011）- 初审驳回
(9010, 7011, 6008, 'YX202606230001', 9601, 9611, '初审', 1, 1, 1003, '主任', '2026-06-23 16:00:00', 'REJECTED', '浊度超标', '浊度1.5NTU超出标准，需重检', 0, 1001, 'system', 1001, 'system'),

-- 已驳回的样品（7012）- 初审通过，复审驳回
(9011, 7012, 6009, 'YX202606230002', 9601, 9611, '初审', 1, 1, 1003, '主任', '2026-06-23 15:00:00', 'APPROVED', NULL, '初审通过', 0, 1001, 'system', 1001, 'system'),
(9012, 7012, 6009, 'YX202606230002', 9601, 9612, '复审', 2, 1, 1003, '主任', '2026-06-23 17:00:00', 'REJECTED', '氨氮偏高', '氨氮0.8mg/L超出标准，需重检', 0, 1001, 'system', 1001, 'system'),

-- 审核中的样品（7005, 7006）- 初审中
(9013, 7005, 6005, 'YX202606240001', 9601, 9611, '初审', 1, 1, NULL, NULL, NULL, NULL, NULL, '待初审', 0, 1001, 'system', 1001, 'system'),
(9014, 7006, 6006, 'YX202606240002', 9601, 9611, '初审', 1, 1, NULL, NULL, NULL, NULL, NULL, '待初审', 0, 1001, 'system', 1001, 'system'),
(9015, 7007, 6007, 'YX202606240003', 9601, 9611, '初审', 1, 1, NULL, NULL, NULL, NULL, NULL, '待初审', 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 9. 报告（12条，覆盖各种状态）
-- ============================================
DELETE FROM lab_report;

INSERT INTO lab_report (id, report_name, report_type, report_category, generated_time, sample_id, sample_no, detection_record_id, report_status, published_time, published_by, published_by_name, file_path, content_snapshot, deleted, created_by, created_name, updated_by, updated_name) VALUES
-- 已发布的报告
(10001, '城东水厂出厂水检测报告-20260622', 'DAILY', 'DETECTION_REPORT', '2026-06-22 19:00:00', 6010, 'YX202606220001', 7008, 'PUBLISHED', '2026-06-22 20:00:00', 1003, '主任', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10002, '城西水厂出厂水检测报告-20260622', 'DAILY', 'DETECTION_REPORT', '2026-06-22 19:30:00', 6011, 'YX202606220002', 7009, 'PUBLISHED', '2026-06-22 20:30:00', 1003, '主任', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10003, '城南水厂出厂水检测报告-20260622', 'DAILY', 'DETECTION_REPORT', '2026-06-22 20:00:00', 6012, 'YX202606220003', 7010, 'PUBLISHED', '2026-06-22 21:00:00', 1003, '主任', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
-- 全流程原始记录
(10004, '城东水厂原始记录-20260622', 'DAILY', 'RAW_RECORD', '2026-06-22 19:00:00', 6010, 'YX202606220001', 7008, 'PUBLISHED', '2026-06-22 20:00:00', 1003, '主任', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10005, '城西水厂原始记录-20260622', 'DAILY', 'RAW_RECORD', '2026-06-22 19:30:00', 6011, 'YX202606220002', 7009, 'PUBLISHED', '2026-06-22 20:30:00', 1003, '主任', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10006, '城南水厂原始记录-20260622', 'DAILY', 'RAW_RECORD', '2026-06-22 20:00:00', 6012, 'YX202606220003', 7010, 'PUBLISHED', '2026-06-22 21:00:00', 1003, '主任', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
-- 待发布的报告（已生成）
(10007, '城东水厂出厂水检测报告-20260624', 'DAILY', 'DETECTION_REPORT', '2026-06-24 18:00:00', 6005, 'YX202606240001', 7005, 'GENERATED', NULL, NULL, NULL, NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10008, '城西水厂出厂水检测报告-20260624', 'DAILY', 'DETECTION_REPORT', '2026-06-24 18:30:00', 6006, 'YX202606240002', 7006, 'GENERATED', NULL, NULL, NULL, NULL, NULL, 0, 1001, 'system', 1001, 'system'),
-- 草稿报告
(10009, '城东水厂周报-第26周', 'WEEKLY', 'DETECTION_REPORT', NULL, NULL, NULL, NULL, 'DRAFT', NULL, NULL, NULL, NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10010, '城西水厂周报-第26周', 'WEEKLY', 'DETECTION_REPORT', NULL, NULL, NULL, NULL, 'DRAFT', NULL, NULL, NULL, NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10011, '城南水厂周报-第26周', 'WEEKLY', 'DETECTION_REPORT', NULL, NULL, NULL, NULL, 'DRAFT', NULL, NULL, NULL, NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(10012, '阳新县水质月报-2026年6月', 'MONTHLY', 'DETECTION_REPORT', NULL, NULL, NULL, NULL, 'DRAFT', NULL, NULL, NULL, NULL, NULL, 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 10. 报告推送记录（6条）
-- ============================================
DELETE FROM lab_report_push_record;

INSERT INTO lab_report_push_record (id, report_id, sample_id, sample_no, recipient_user_id, recipient_name, recipient_phone, push_channel, push_status, push_message, push_time, deleted, created_by, created_name, updated_by, updated_name) VALUES
(11001, 10001, 6010, 'YX202606220001', 1002, '员工', '13800000001', 'SYSTEM', 'SUCCESS', '报告已发布，请查阅', '2026-06-22 20:00:00', 0, 1001, 'system', 1001, 'system'),
(11002, 10002, 6011, 'YX202606220002', 1002, '员工', '13800000001', 'SYSTEM', 'SUCCESS', '报告已发布，请查阅', '2026-06-22 20:30:00', 0, 1001, 'system', 1001, 'system'),
(11003, 10003, 6012, 'YX202606220003', 1002, '员工', '13800000001', 'SYSTEM', 'SUCCESS', '报告已发布，请查阅', '2026-06-22 21:00:00', 0, 1001, 'system', 1001, 'system'),
(11004, 10007, 6005, 'YX202606240001', 1002, '员工', '13800000001', 'SYSTEM', 'PENDING', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(11005, 10008, 6006, 'YX202606240002', 1002, '员工', '13800000001', 'SYSTEM', 'PENDING', NULL, NULL, 0, 1001, 'system', 1001, 'system'),
(11006, 10001, 6010, 'YX202606220001', 1003, '主任', '13800000002', 'SYSTEM', 'SUCCESS', '报告已发布，请查阅', '2026-06-22 20:00:00', 0, 1001, 'system', 1001, 'system');

-- ============================================
-- 11. 报告模板（2条）
-- ============================================
DELETE FROM lab_report_template;

INSERT INTO lab_report_template (id, report_type, template_name, default_template, template_content, remark, deleted, created_by, created_name, updated_by, updated_name) VALUES
(12001, 'DAILY', '日报模板', 1, '<html><body><h1>水质检测日报</h1></body></html>', '默认日报模板', 0, 1001, 'system', 1001, 'system'),
(12002, 'WEEKLY', '周报模板', 1, '<html><body><h1>水质检测周报</h1></body></html>', '默认周报模板', 0, 1001, 'system', 1001, 'system');

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 数据统计
-- ============================================
SELECT '监测点位' as 表名, COUNT(*) as 记录数 FROM lab_monitoring_point
UNION ALL
SELECT '采样计划', COUNT(*) FROM lab_sampling_plan
UNION ALL
SELECT '采样任务', COUNT(*) FROM lab_sampling_task
UNION ALL
SELECT '样品', COUNT(*) FROM lab_sample
UNION ALL
SELECT '检测记录', COUNT(*) FROM lab_detection_record
UNION ALL
SELECT '检测项', COUNT(*) FROM lab_detection_item
UNION ALL
SELECT '审查记录', COUNT(*) FROM lab_review_record
UNION ALL
SELECT '报告', COUNT(*) FROM lab_report
UNION ALL
SELECT '报告推送', COUNT(*) FROM lab_report_push_record
UNION ALL
SELECT '报告模板', COUNT(*) FROM lab_report_template;
