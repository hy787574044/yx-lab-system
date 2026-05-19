-- 演示数据重置与造数脚本
-- 用途：清理非系统管理业务数据，并生成可继续流转的全链路演示数据。
-- 注意：执行前请确认目标库为 yx_lab。

USE yx_lab;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 清理业务数据，保留角色、机构、字典、流程配置等系统基础配置。
DELETE FROM lab_report_push_record;
DELETE FROM lab_report;
DELETE FROM lab_review_record;
DELETE FROM lab_detection_item;
DELETE FROM lab_detection_record;
DELETE FROM lab_sample;
DELETE FROM lab_sampling_task;
DELETE FROM lab_sampling_plan;
DELETE FROM lab_monitoring_point;
DELETE FROM lab_instrument_maintenance;
DELETE FROM lab_instrument;
DELETE FROM lab_document_share;
DELETE FROM lab_document;

-- 2. 清理本脚本维护的演示检测配置，避免重复执行时唯一键冲突。
DELETE FROM lab_detection_step WHERE id BETWEEN 730000 AND 739999;
DELETE FROM lab_detection_type WHERE id BETWEEN 720000 AND 729999 OR type_name IN ('出厂水常规九项', '原水重点五项', '管网末梢四项', '应急复检套餐');
DELETE FROM lab_detection_method
WHERE id BETWEEN 710000 AND 719999
   OR method_code LIKE 'DEMO-%'
   OR method_name IN (
      '玻璃电极法',
      '散射光浊度法',
      'DPD 分光光度法',
      '纳氏试剂分光光度法',
      '铂钴标准比色法',
      '嗅味直接判断法',
      '目视观察法',
      '原子吸收分光光度法-铁',
      '原子吸收分光光度法-锰',
      '酸性高锰酸钾滴定法'
   );
DELETE FROM lab_detection_parameter WHERE id BETWEEN 700000 AND 709999 OR parameter_name IN ('pH', '浊度', '余氯', '氨氮', '色度', '臭和味', '肉眼可见物', '铁', '锰', '耗氧量');
DELETE FROM lab_detection_project_group WHERE id BETWEEN 705000 AND 705999 OR group_name IN ('常规理化组', '消毒指标组', '金属指标组', '感官指标组');
DELETE FROM lab_user WHERE id BETWEEN 880000 AND 880999 OR username LIKE 'demo\_%';

-- 3. 补充系统用户。密码沿用初始化脚本中的演示密码。
INSERT INTO lab_user (id, username, password, real_name, org_id, org_name, role_code, phone, avatar_url, status, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(1101, 'sampler01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '陈采样', 802, '采样组', 'SAMPLER', '13810001001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1102, 'sampler02', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '李采样', 802, '采样组', 'SAMPLER', '13810001002', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1201, 'detector01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '王检测', 803, '检测审核组', 'DETECTOR', '13810002001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1202, 'detector02', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '赵检测', 803, '检测审核组', 'DETECTOR', '13810002002', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1203, 'detector03', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '周检测', 803, '检测审核组', 'DETECTOR', '13810002003', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1301, 'reviewer01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '刘审核', 803, '检测审核组', 'REVIEWER', '13810003001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1302, 'reviewer02', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '何审核', 803, '检测审核组', 'REVIEWER', '13810003002', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(1401, 'reporter01', 'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7', '孙报告', 803, '检测审核组', 'REPORTER', '13810004001', NULL, 1, 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW())
ON DUPLICATE KEY UPDATE
real_name = VALUES(real_name),
org_id = VALUES(org_id),
org_name = VALUES(org_name),
role_code = VALUES(role_code),
phone = VALUES(phone),
status = VALUES(status),
deleted = 0,
updated_by = VALUES(updated_by),
updated_name = VALUES(updated_name),
updated_time = NOW();

-- 4. 检测项目组、参数、方法、套餐和步骤。
INSERT INTO lab_detection_project_group (id, group_name, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(705001, '常规理化组', 1, '覆盖 pH、浊度、耗氧量等日常理化指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705002, '消毒指标组', 1, '覆盖余氯、氨氮等消毒与氮素指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705003, '金属指标组', 1, '覆盖铁、锰等金属风险指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(705004, '感官指标组', 1, '覆盖色度、臭和味、肉眼可见物', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_detection_parameter (id, parameter_name, standard_min, standard_max, unit, exceed_rule, reference_standard, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(700001, 'pH', 6.50, 8.50, '', 'OUT_OF_RANGE', 'GB 5749-2022', 1, '生活饮用水酸碱度', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700002, '浊度', 0.00, 1.00, 'NTU', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '反映水体悬浮物情况', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700003, '余氯', 0.05, 2.00, 'mg/L', 'OUT_OF_RANGE', 'GB 5749-2022', 1, '消毒剂余量控制指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700004, '氨氮', 0.00, 0.50, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '氮素污染风险指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700005, '色度', 0.00, 15.00, '度', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官性状指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700006, '臭和味', 0.00, 0.00, '级', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官性状指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700007, '肉眼可见物', 0.00, 0.00, '项', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '感官性状指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700008, '铁', 0.00, 0.30, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '金属指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700009, '锰', 0.00, 0.10, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '金属指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(700010, '耗氧量', 0.00, 3.00, 'mg/L', 'GREATER_THAN_MAX', 'GB 5749-2022', 1, '有机污染综合指标', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_detection_method (id, method_name, method_code, parameter_id, parameter_name, standard_code, method_basis, apply_scope, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(710001, '玻璃电极法', 'DEMO-PH-01', 700001, 'pH', 'GB/T 5750.4', '校准 pH 计后直接测定，记录温度补偿后的稳定读数。', '出厂水、原水、管网水 pH 检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710002, '散射光浊度法', 'DEMO-TURB-01', 700002, '浊度', 'GB/T 5750.4', '摇匀样品后加入比色皿，使用浊度仪读取 NTU 值。', '生活饮用水浊度检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710003, 'DPD 分光光度法', 'DEMO-CL-01', 700003, '余氯', 'GB/T 5750.11', '加入 DPD 试剂显色，在规定波长下测定吸光度并换算浓度。', '出厂水和管网水余氯检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710004, '纳氏试剂分光光度法', 'DEMO-NH3N-01', 700004, '氨氮', 'HJ 535', '样品经预处理后加入纳氏试剂显色，使用分光光度计测定。', '原水和异常复检样品氨氮检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710005, '铂钴标准比色法', 'DEMO-COLOR-01', 700005, '色度', 'GB/T 5750.4', '与铂钴标准色列比对，读取最接近色度值。', '感官指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710006, '嗅味直接判断法', 'DEMO-ODOR-01', 700006, '臭和味', 'GB/T 5750.4', '按标准温度条件嗅辨并记录等级。', '感官指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710007, '目视观察法', 'DEMO-VISIBLE-01', 700007, '肉眼可见物', 'GB/T 5750.4', '取样后在自然光下目视观察是否存在可见悬浮物。', '感官指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710008, '原子吸收分光光度法-铁', 'DEMO-FE-01', 700008, '铁', 'GB/T 5750.6', '消解后使用原子吸收分光光度计测定铁含量。', '金属指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710009, '原子吸收分光光度法-锰', 'DEMO-MN-01', 700009, '锰', 'GB/T 5750.6', '消解后使用原子吸收分光光度计测定锰含量。', '金属指标检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(710010, '酸性高锰酸钾滴定法', 'DEMO-CODMN-01', 700010, '耗氧量', 'GB/T 5750.7', '酸性条件下高锰酸钾氧化，滴定计算耗氧量。', '有机污染综合检测', 1, '演示方法', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_detection_type (id, type_name, group_id, group_name, detector_id, detector_name, parameter_ids, parameter_names, parameter_method_bindings, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(720001, '出厂水常规九项', 705001, '常规理化组', 1201, '王检测', '700001,700002,700003,700004,700005,700006,700007,700008,700010', 'pH,浊度,余氯,氨氮,色度,臭和味,肉眼可见物,铁,耗氧量', '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700003,"methodIds":[710003]},{"parameterId":700004,"methodIds":[710004]},{"parameterId":700005,"methodIds":[710005]},{"parameterId":700006,"methodIds":[710006]},{"parameterId":700007,"methodIds":[710007]},{"parameterId":700008,"methodIds":[710008]},{"parameterId":700010,"methodIds":[710010]}]', 1, '出厂水日常检测套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720002, '原水重点五项', 705002, '消毒指标组', 1202, '赵检测', '700001,700002,700004,700009,700010', 'pH,浊度,氨氮,锰,耗氧量', '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700004,"methodIds":[710004]},{"parameterId":700009,"methodIds":[710009]},{"parameterId":700010,"methodIds":[710010]}]', 1, '原水重点风险指标套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720003, '管网末梢四项', 705004, '感官指标组', 1203, '周检测', '700001,700002,700003,700007', 'pH,浊度,余氯,肉眼可见物', '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700003,"methodIds":[710003]},{"parameterId":700007,"methodIds":[710007]}]', 1, '管网末梢常规巡检套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(720004, '应急复检套餐', 705003, '金属指标组', 1201, '王检测', '700002,700004,700008,700009,700010', '浊度,氨氮,铁,锰,耗氧量', '[{"parameterId":700002,"methodIds":[710002]},{"parameterId":700004,"methodIds":[710004]},{"parameterId":700008,"methodIds":[710008]},{"parameterId":700009,"methodIds":[710009]},{"parameterId":700010,"methodIds":[710010]}]', 1, '异常样品复检套餐', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

INSERT INTO lab_detection_step (id, type_id, type_name, step_name, step_order, step_description, reagent_requirement, operation_requirement, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(730001, 720001, '出厂水常规九项', '样品核验与预处理', 1, '核对封签、样品编号、保存条件并混匀样品。', '无', '确认样品外观无泄漏，必要时记录异常。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730002, 720001, '出厂水常规九项', '理化与感官检测', 2, '依次完成 pH、浊度、色度、臭和味等指标检测。', '标准缓冲液、比色试剂', '按检测步骤逐项记录原始值。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730003, 720002, '原水重点五项', '原水重点指标检测', 1, '完成原水 pH、浊度、氨氮、锰、耗氧量检测。', '纳氏试剂、标准液', '对异常指标进行复测确认。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730004, 720003, '管网末梢四项', '末梢快速检测', 1, '对末梢水 pH、浊度、余氯、肉眼可见物进行快速检测。', 'DPD 试剂', '余氯需现场快速检测并记录时间。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(730005, 720004, '应急复检套餐', '异常指标复检', 1, '对被驳回或异常样品执行重点复检。', '复检标准液', '复检结果需附异常说明。', '演示步骤', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

-- 5. 监测点位、采样计划和采样任务。
INSERT INTO lab_monitoring_point (id, point_name, longitude, latitude, region_name, service_population, frequency_type, owner_id, owner_name, contact_phone, point_type, point_status, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(740001, '城东水厂出厂水', '115.2121', '30.2211', '阳新县城东片区', 36000, 'DAILY', 1101, '陈采样', '13810001001', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 30 DAY, 1001, '系统管理员', NOW()),
(740002, '城西水厂出厂水', '115.1781', '30.2051', '阳新县城西片区', 42000, 'DAILY', 1102, '李采样', '13810001002', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 28 DAY, 1001, '系统管理员', NOW()),
(740003, '富河原水取水口', '115.0912', '30.1548', '富河流域', 68000, 'WEEKLY', 1101, '陈采样', '13810001001', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 25 DAY, 1001, '系统管理员', NOW()),
(740004, '兴国大道末梢点', '115.2266', '30.2199', '兴国大道', 12000, 'DAILY', 1102, '李采样', '13810001002', 'TERMINAL', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW()),
(740005, '莲花湖社区末梢点', '115.2442', '30.2366', '莲花湖社区', 9800, 'WEEKLY', 1101, '陈采样', '13810001001', 'TERMINAL', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 15 DAY, 1001, '系统管理员', NOW()),
(740006, '应急加密监测点', '115.2622', '30.2411', '城南片区', 6000, 'DAILY', 1102, '李采样', '13810001002', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 12 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_sampling_plan (id, plan_name, point_id, point_name, start_time, end_time, sampler_id, sampler_name, sampling_type, sample_type, cycle_type, plan_status, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(750001, '城东水厂每日出厂水计划', 740001, '城东水厂出厂水', CURDATE() - INTERVAL 20 DAY, CURDATE() + INTERVAL 40 DAY, 1101, '陈采样', 'ROUTINE', 'FACTORY', 'DAILY', 'ACTIVE', '每日早班采集出厂水。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(750002, '城西水厂每日出厂水计划', 740002, '城西水厂出厂水', CURDATE() - INTERVAL 18 DAY, CURDATE() + INTERVAL 35 DAY, 1102, '李采样', 'ROUTINE', 'FACTORY', 'DAILY', 'ACTIVE', '每日常规出厂水监测。', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW()),
(750003, '富河原水每周计划', 740003, '富河原水取水口', CURDATE() - INTERVAL 16 DAY, CURDATE() + INTERVAL 60 DAY, 1101, '陈采样', 'ROUTINE', 'RAW', 'WEEKLY', 'DISPATCHED', '每周一采集原水样品。', 0, 1001, '系统管理员', NOW() - INTERVAL 16 DAY, 1001, '系统管理员', NOW()),
(750004, '兴国大道末梢巡检计划', 740004, '兴国大道末梢点', CURDATE() - INTERVAL 14 DAY, CURDATE() + INTERVAL 45 DAY, 1102, '李采样', 'ROUTINE', 'TERMINAL', 'DAILY', 'ACTIVE', '管网末梢每日巡检。', 0, 1001, '系统管理员', NOW() - INTERVAL 14 DAY, 1001, '系统管理员', NOW()),
(750005, '应急加密监测计划', 740006, '应急加密监测点', CURDATE() - INTERVAL 5 DAY, CURDATE() + INTERVAL 10 DAY, 1102, '李采样', 'ROUTINE', 'RAW', 'ONCE', 'UNPUBLISHED', '异常天气后加密采样。', 0, 1001, '系统管理员', NOW() - INTERVAL 5 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_sampling_task (id, task_no, plan_id, point_id, point_name, sampling_time, sampler_id, sampler_name, sample_type, seal_no, sample_register_status, sample_id, detection_items, task_status, started_time, onsite_metrics, photo_urls, abandon_reason, finished_time, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(760001, 'TASK-20260501-001', 750001, 740001, '城东水厂出厂水', NOW() - INTERVAL 18 DAY, 1101, '陈采样', 'FACTORY', 'SEAL-DEMO-001', 'REGISTERED', 770001, '出厂水常规九项', 'COMPLETED', NOW() - INTERVAL 18 DAY + INTERVAL 1 HOUR, '水温 21.3℃；余氯现场读数 0.42mg/L', NULL, NULL, NOW() - INTERVAL 18 DAY + INTERVAL 2 HOUR, '样品已登录并完成审核。', 0, 1101, '陈采样', NOW() - INTERVAL 18 DAY, 1101, '陈采样', NOW() - INTERVAL 18 DAY + INTERVAL 2 HOUR),
(760002, 'TASK-20260502-001', 750002, 740002, '城西水厂出厂水', NOW() - INTERVAL 13 DAY, 1102, '李采样', 'FACTORY', 'SEAL-DEMO-002', 'REGISTERED', 770002, '出厂水常规九项', 'COMPLETED', NOW() - INTERVAL 13 DAY + INTERVAL 1 HOUR, '水温 20.8℃；余氯现场读数 0.35mg/L', NULL, NULL, NOW() - INTERVAL 13 DAY + INTERVAL 2 HOUR, '样品已发布报告。', 0, 1102, '李采样', NOW() - INTERVAL 13 DAY, 1102, '李采样', NOW() - INTERVAL 13 DAY + INTERVAL 2 HOUR),
(760003, 'TASK-20260505-001', 750003, 740003, '富河原水取水口', NOW() - INTERVAL 9 DAY, 1101, '陈采样', 'RAW', 'SEAL-DEMO-003', 'REGISTERED', 770003, '原水重点五项', 'COMPLETED', NOW() - INTERVAL 9 DAY + INTERVAL 1 HOUR, '水温 19.6℃；水位偏高', NULL, NULL, NOW() - INTERVAL 9 DAY + INTERVAL 2 HOUR, '样品待审核。', 0, 1101, '陈采样', NOW() - INTERVAL 9 DAY, 1101, '陈采样', NOW() - INTERVAL 9 DAY + INTERVAL 2 HOUR),
(760004, 'TASK-20260508-001', 750004, 740004, '兴国大道末梢点', NOW() - INTERVAL 6 DAY, 1102, '李采样', 'TERMINAL', 'SEAL-DEMO-004', 'REGISTERED', 770004, '管网末梢四项', 'COMPLETED', NOW() - INTERVAL 6 DAY + INTERVAL 1 HOUR, '水温 22.1℃；现场余氯偏低', NULL, NULL, NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR, '审核驳回，待重检。', 0, 1102, '李采样', NOW() - INTERVAL 6 DAY, 1102, '李采样', NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR),
(760005, 'TASK-20260511-001', 750001, 740001, '城东水厂出厂水', NOW() - INTERVAL 3 DAY, 1101, '陈采样', 'FACTORY', 'SEAL-DEMO-005', 'REGISTERED', 770005, '出厂水常规九项', 'COMPLETED', NOW() - INTERVAL 3 DAY + INTERVAL 1 HOUR, '水温 23.0℃；现场状态正常', NULL, NULL, NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, '已登录待分配。', 0, 1101, '陈采样', NOW() - INTERVAL 3 DAY, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
(760006, 'TASK-20260513-001', 750004, 740004, '兴国大道末梢点', NOW() - INTERVAL 2 DAY, 1102, '李采样', 'TERMINAL', 'SEAL-DEMO-006', 'REGISTERED', 770006, '管网末梢四项', 'COMPLETED', NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR, '水温 22.6℃；末梢压力正常', NULL, NULL, NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR, '已分配检测人员。', 0, 1102, '李采样', NOW() - INTERVAL 2 DAY, 1102, '李采样', NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR),
(760007, 'TASK-20260515-001', 750003, 740003, '富河原水取水口', NOW() - INTERVAL 1 DAY, 1101, '陈采样', 'RAW', 'SEAL-DEMO-007', 'UNREGISTERED', NULL, '原水重点五项', 'COMPLETED', NOW() - INTERVAL 1 DAY + INTERVAL 1 HOUR, '水温 18.9℃；浊度略高', NULL, NULL, NOW() - INTERVAL 1 DAY + INTERVAL 2 HOUR, '采样完成，待样品登录。', 0, 1101, '陈采样', NOW() - INTERVAL 1 DAY, 1101, '陈采样', NOW() - INTERVAL 1 DAY + INTERVAL 2 HOUR),
(760008, 'TASK-20260516-001', 750005, 740006, '应急加密监测点', NOW() + INTERVAL 1 DAY, 1102, '李采样', 'RAW', 'SEAL-DEMO-008', 'UNREGISTERED', NULL, '应急复检套餐', 'PENDING', NULL, NULL, NULL, NULL, NULL, '待执行采样任务，可继续开始采样。', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(760009, 'TASK-20260516-002', 750002, 740002, '城西水厂出厂水', NOW(), 1102, '李采样', 'FACTORY', 'SEAL-DEMO-009', 'UNREGISTERED', NULL, '出厂水常规九项', 'IN_PROGRESS', NOW() - INTERVAL 1 HOUR, '已到达现场，正在采集样品。', NULL, NULL, NULL, '执行中任务，可继续完成采样。', 0, 1102, '李采样', NOW(), 1102, '李采样', NOW());

-- 6. 样品、检测主流程、检测子流程、审核、报告。
INSERT INTO lab_sample (id, sample_no, seal_no, task_id, point_id, point_name, sample_type, detection_items, detection_type_id, detection_type_name, detection_config_snapshot, review_flow_id, review_flow_name, publish_flow_id, publish_flow_name, sampling_time, seal_time, sampler_id, sampler_name, weather, storage_condition, sample_status, result_summary, remark, trace_log, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(770001, 'SAMPLE-DEMO-001', 'SEAL-DEMO-001', 760001, 740001, '城东水厂出厂水', 'FACTORY', '出厂水常规九项', 720001, '出厂水常规九项', NULL, 9601, '常规三级审核', 9602, '报告发布审批', NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 18 DAY + INTERVAL 2 HOUR, 1101, '陈采样', '晴', '冷藏避光', 'COMPLETED', '正常', '演示：已完成并生成报告。', '样品登录：封签号=SEAL-DEMO-001，采样员=陈采样\n审查通过：结果=正常\n已生成报告', 0, 1101, '陈采样', NOW() - INTERVAL 18 DAY + INTERVAL 2 HOUR, 1301, '刘审核', NOW() - INTERVAL 17 DAY),
(770002, 'SAMPLE-DEMO-002', 'SEAL-DEMO-002', 760002, 740002, '城西水厂出厂水', 'FACTORY', '出厂水常规九项', 720001, '出厂水常规九项', NULL, 9601, '常规三级审核', 9602, '报告发布审批', NOW() - INTERVAL 13 DAY, NOW() - INTERVAL 13 DAY + INTERVAL 2 HOUR, 1102, '李采样', '多云', '常温送检', 'COMPLETED', '正常', '演示：已发布报告。', '样品登录：封签号=SEAL-DEMO-002，采样员=李采样\n审查通过：结果=正常\n已发布报告', 0, 1102, '李采样', NOW() - INTERVAL 13 DAY + INTERVAL 2 HOUR, 1401, '孙报告', NOW() - INTERVAL 12 DAY),
(770003, 'SAMPLE-DEMO-003', 'SEAL-DEMO-003', 760003, 740003, '富河原水取水口', 'RAW', '原水重点五项', 720002, '原水重点五项', NULL, 9601, '常规三级审核', 9602, '报告发布审批', NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 9 DAY + INTERVAL 2 HOUR, 1101, '陈采样', '小雨', '冷藏送检', 'REVIEWING', '异常', '演示：检测已提交，待审核。', '样品登录：封签号=SEAL-DEMO-003，采样员=陈采样\n检测提交：结果=异常', 0, 1101, '陈采样', NOW() - INTERVAL 9 DAY + INTERVAL 2 HOUR, 1202, '赵检测', NOW() - INTERVAL 8 DAY),
(770004, 'SAMPLE-DEMO-004', 'SEAL-DEMO-004', 760004, 740004, '兴国大道末梢点', 'TERMINAL', '管网末梢四项', 720003, '管网末梢四项', NULL, 9601, '常规三级审核', 9602, '报告发布审批', NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR, 1102, '李采样', '晴', '常温送检', 'RETEST', '余氯：低于控制要求', '演示：审核驳回，待重检。', '样品登录：封签号=SEAL-DEMO-004，采样员=李采样\n审查驳回：余氯偏低，要求复检', 0, 1102, '李采样', NOW() - INTERVAL 6 DAY + INTERVAL 2 HOUR, 1302, '何审核', NOW() - INTERVAL 5 DAY),
(770005, 'SAMPLE-DEMO-005', 'SEAL-DEMO-005', 760005, 740001, '城东水厂出厂水', 'FACTORY', '出厂水常规九项', 720001, '出厂水常规九项', NULL, 9601, '常规三级审核', 9602, '报告发布审批', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 1101, '陈采样', '晴', '冷藏避光', 'LOGGED', NULL, '演示：待分配检测人员。', '样品登录：封签号=SEAL-DEMO-005，采样员=陈采样', 0, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
(770006, 'SAMPLE-DEMO-006', 'SEAL-DEMO-006', 760006, 740004, '兴国大道末梢点', 'TERMINAL', '管网末梢四项', 720003, '管网末梢四项', NULL, 9601, '常规三级审核', 9602, '报告发布审批', NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR, 1102, '李采样', '阴', '常温送检', 'LOGGED', NULL, '演示：已分配，待检测。', '样品登录：封签号=SEAL-DEMO-006，采样员=李采样\n检测分样：已分配检测人员', 0, 1102, '李采样', NOW() - INTERVAL 2 DAY + INTERVAL 2 HOUR, 1203, '周检测', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR);

INSERT INTO lab_detection_record (id, sample_id, sample_no, seal_no, detection_type_id, detection_type_name, detection_time, detector_id, detector_name, detection_result, abnormal_remark, detection_status, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(780001, 770001, 'SAMPLE-DEMO-001', 'SEAL-DEMO-001', 720001, '出厂水常规九项', NOW() - INTERVAL 17 DAY, 1201, '王检测', 'NORMAL', '全部指标正常。', 'APPROVED', 0, 1201, '王检测', NOW() - INTERVAL 17 DAY, 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR),
(780002, 770002, 'SAMPLE-DEMO-002', 'SEAL-DEMO-002', 720001, '出厂水常规九项', NOW() - INTERVAL 12 DAY, 1201, '王检测', 'NORMAL', '全部指标正常。', 'APPROVED', 0, 1201, '王检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR),
(780003, 770003, 'SAMPLE-DEMO-003', 'SEAL-DEMO-003', 720002, '原水重点五项', NOW() - INTERVAL 8 DAY, 1202, '赵检测', 'ABNORMAL', '浊度与氨氮偏高，建议关注上游来水。', 'SUBMITTED', 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR),
(780004, 770004, 'SAMPLE-DEMO-004', 'SEAL-DEMO-004', 720003, '管网末梢四项', NOW() - INTERVAL 5 DAY, 1203, '周检测', NULL, '余氯结果需复检。', 'WAIT_DETECT', 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1302, '何审核', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR),
(780005, 770005, 'SAMPLE-DEMO-005', 'SEAL-DEMO-005', 720001, '出厂水常规九项', NULL, NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
(780006, 770006, 'SAMPLE-DEMO-006', 'SEAL-DEMO-006', 720003, '管网末梢四项', NULL, 1203, '周检测', NULL, NULL, 'WAIT_DETECT', 0, 1203, '周检测', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR, 1203, '周检测', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR);

-- 检测项：覆盖已通过、待审核、待检测、待分配、驳回复检等状态。
INSERT INTO lab_detection_item (id, record_id, parameter_id, parameter_name, standard_min, standard_max, result_value, unit, reference_standard, method_id, method_name, detector_id, detector_name, item_status, exceed_flag, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(790001, 780001, 700001, 'pH', 6.50, 8.50, 7.20, '', 'GB 5749-2022', 710001, '玻璃电极法', 1201, '王检测', 'APPROVED', 0, 0, 1201, '王检测', NOW() - INTERVAL 17 DAY, 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR),
(790002, 780001, 700002, '浊度', 0.00, 1.00, 0.38, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1201, '王检测', 'APPROVED', 0, 0, 1201, '王检测', NOW() - INTERVAL 17 DAY, 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR),
(790003, 780001, 700003, '余氯', 0.05, 2.00, 0.42, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1202, '赵检测', 'APPROVED', 0, 0, 1202, '赵检测', NOW() - INTERVAL 17 DAY, 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR),
(790004, 780001, 700004, '氨氮', 0.00, 0.50, 0.08, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1202, '赵检测', 'APPROVED', 0, 0, 1202, '赵检测', NOW() - INTERVAL 17 DAY, 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR),
(790005, 780002, 700001, 'pH', 6.50, 8.50, 7.08, '', 'GB 5749-2022', 710001, '玻璃电极法', 1201, '王检测', 'APPROVED', 0, 0, 1201, '王检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR),
(790006, 780002, 700002, '浊度', 0.00, 1.00, 0.46, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1201, '王检测', 'APPROVED', 0, 0, 1201, '王检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR),
(790007, 780002, 700003, '余氯', 0.05, 2.00, 0.35, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1202, '赵检测', 'APPROVED', 0, 0, 1202, '赵检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR),
(790008, 780002, 700010, '耗氧量', 0.00, 3.00, 1.20, 'mg/L', 'GB 5749-2022', 710010, '酸性高锰酸钾滴定法', 1203, '周检测', 'APPROVED', 0, 0, 1203, '周检测', NOW() - INTERVAL 12 DAY, 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR),
(790009, 780003, 700001, 'pH', 6.50, 8.50, 7.80, '', 'GB 5749-2022', 710001, '玻璃电极法', 1202, '赵检测', 'SUBMITTED', 0, 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR),
(790010, 780003, 700002, '浊度', 0.00, 1.00, 1.38, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1202, '赵检测', 'SUBMITTED', 1, 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR),
(790011, 780003, 700004, '氨氮', 0.00, 0.50, 0.72, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1202, '赵检测', 'SUBMITTED', 1, 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY + INTERVAL 2 HOUR),
(790012, 780004, 700001, 'pH', 6.50, 8.50, 7.10, '', 'GB 5749-2022', 710001, '玻璃电极法', 1203, '周检测', 'APPROVED', 0, 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1302, '何审核', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR),
(790013, 780004, 700003, '余氯', 0.05, 2.00, 0.03, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1203, '周检测', 'REJECTED', 1, 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1302, '何审核', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR),
(790014, 780005, 700001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', 710001, '玻璃电极法', NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
(790015, 780005, 700002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR, 1101, '陈采样', NOW() - INTERVAL 3 DAY + INTERVAL 2 HOUR),
(790016, 780006, 700001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', 710001, '玻璃电极法', 1203, '周检测', 'WAIT_DETECT', 0, 0, 1203, '周检测', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR, 1203, '周检测', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR),
(790017, 780006, 700003, '余氯', 0.05, 2.00, NULL, 'mg/L', 'GB 5749-2022', 710003, 'DPD 分光光度法', 1203, '周检测', 'WAIT_DETECT', 0, 0, 1203, '周检测', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR, 1203, '周检测', NOW() - INTERVAL 2 DAY + INTERVAL 3 HOUR);

INSERT INTO lab_review_record (id, detection_record_id, sample_id, sample_no, seal_no, reviewer_id, reviewer_name, review_time, review_result, reject_reason, review_remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(800001, 780001, 770001, 'SAMPLE-DEMO-001', 'SEAL-DEMO-001', 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR, 'APPROVED', NULL, '指标稳定，审核通过。', 0, 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR, 1301, '刘审核', NOW() - INTERVAL 17 DAY + INTERVAL 3 HOUR),
(800002, 780002, 770002, 'SAMPLE-DEMO-002', 'SEAL-DEMO-002', 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR, 'APPROVED', NULL, '指标稳定，审核通过。', 0, 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR, 1301, '刘审核', NOW() - INTERVAL 12 DAY + INTERVAL 3 HOUR),
(800003, 780004, 770004, 'SAMPLE-DEMO-004', 'SEAL-DEMO-004', 1302, '何审核', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR, 'REJECTED', '余氯低于控制要求，请重新检测。', '要求检测员复核余氯结果并补充现场说明。', 0, 1302, '何审核', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR, 1302, '何审核', NOW() - INTERVAL 5 DAY + INTERVAL 4 HOUR);

INSERT INTO lab_report (id, report_name, report_type, generated_time, sample_id, sample_no, seal_no, detection_record_id, report_status, published_time, published_by, published_by_name, push_status, last_push_time, last_push_message, file_path, content_snapshot, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(810001, 'SAMPLE-DEMO-001-检测报告', 'DAILY', NOW() - INTERVAL 16 DAY, 770001, 'SAMPLE-DEMO-001', 'SEAL-DEMO-001', 780001, 'GENERATED', NULL, NULL, NULL, 'PENDING', NULL, '报告已生成，待发布。', NULL, '样品编号：SAMPLE-DEMO-001\n点位名称：城东水厂出厂水\n检测类型：出厂水常规九项\n检测结果：正常\n结论：本次检测符合要求。', 0, 1301, '刘审核', NOW() - INTERVAL 16 DAY, 1301, '刘审核', NOW() - INTERVAL 16 DAY),
(810002, 'SAMPLE-DEMO-002-检测报告', 'DAILY', NOW() - INTERVAL 11 DAY, 770002, 'SAMPLE-DEMO-002', 'SEAL-DEMO-002', 780002, 'PUBLISHED', NOW() - INTERVAL 10 DAY, 1401, '孙报告', 'SUCCESS', NOW() - INTERVAL 10 DAY, '已推送至业务系统。', NULL, '样品编号：SAMPLE-DEMO-002\n点位名称：城西水厂出厂水\n检测类型：出厂水常规九项\n检测结果：正常\n结论：本次检测符合要求。', 0, 1301, '刘审核', NOW() - INTERVAL 11 DAY, 1401, '孙报告', NOW() - INTERVAL 10 DAY),
(810003, '应急复检报告草稿', 'DAILY', NOW() - INTERVAL 2 DAY, 770004, 'SAMPLE-DEMO-004', 'SEAL-DEMO-004', 780004, 'DRAFT', NULL, NULL, NULL, 'PENDING', NULL, '驳回样品复检后再发布。', NULL, '样品编号：SAMPLE-DEMO-004\n当前状态：待重检\n说明：草稿报告用于演示报告草稿状态。', 0, 1401, '孙报告', NOW() - INTERVAL 2 DAY, 1401, '孙报告', NOW() - INTERVAL 2 DAY);

INSERT INTO lab_report_push_record (id, report_id, sample_id, sample_no, seal_no, recipient_user_id, recipient_name, recipient_phone, push_channel, push_status, push_message, push_time, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(820001, 810002, 770002, 'SAMPLE-DEMO-002', 'SEAL-DEMO-002', 1401, '孙报告', '13810004001', 'UNIFIED', 'SUCCESS', '已推送至业务系统。', NOW() - INTERVAL 10 DAY, 0, 1401, '孙报告', NOW() - INTERVAL 10 DAY, 1401, '孙报告', NOW() - INTERVAL 10 DAY);

-- 7. 仪器、维修、文档资料。
INSERT INTO lab_instrument (id, instrument_name, instrument_model, manufacturer, purchase_date, service_life_years, calibration_cycle, owner_name, instrument_status, storage_location, certificate_url, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(830001, '紫外可见分光光度计', 'UV-2600i', '岛津', CURDATE() - INTERVAL 900 DAY, 8, '12个月', '王检测', 'NORMAL', '理化室 A-01', NULL, '用于氨氮、余氯等比色检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 60 DAY, 1001, '系统管理员', NOW()),
(830002, '浊度仪', '2100Q', 'HACH', CURDATE() - INTERVAL 600 DAY, 6, '6个月', '赵检测', 'NORMAL', '理化室 A-02', NULL, '现场与实验室浊度检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 55 DAY, 1001, '系统管理员', NOW()),
(830003, 'pH 计', 'SevenCompact', '梅特勒', CURDATE() - INTERVAL 500 DAY, 5, '6个月', '周检测', 'CALIBRATING', '理化室 A-03', NULL, '正在校准，演示校准状态。', 0, 1001, '系统管理员', NOW() - INTERVAL 50 DAY, 1001, '系统管理员', NOW()),
(830004, '原子吸收分光光度计', 'AA-7000', '岛津', CURDATE() - INTERVAL 1200 DAY, 10, '12个月', '王检测', 'MAINTENANCE', '金属室 B-01', NULL, '用于铁、锰检测，当前维修中。', 0, 1001, '系统管理员', NOW() - INTERVAL 48 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_instrument_maintenance (id, instrument_id, instrument_name, maintenance_time, maintenance_reason, maintainer_name, maintenance_company, maintenance_result, maintenance_cost, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(840001, 830004, '原子吸收分光光度计', NOW() - INTERVAL 7 DAY, '灯能量波动，检测重复性下降。', '工程师张工', '武汉精仪维保有限公司', '已更换空心阴极灯，等待复测。', 2800.00, '演示维修记录。', 0, 1001, '系统管理员', NOW() - INTERVAL 7 DAY, 1001, '系统管理员', NOW() - INTERVAL 7 DAY),
(840002, 830003, 'pH 计', NOW() - INTERVAL 2 DAY, '例行校准。', '周检测', '内部校准', '校准中，待复核。', 0.00, '演示校准记录。', 0, 1203, '周检测', NOW() - INTERVAL 2 DAY, 1203, '周检测', NOW() - INTERVAL 2 DAY);

INSERT INTO lab_document (id, document_name, document_category, file_type, file_size, file_url, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(850001, '生活饮用水检测作业指导书', '操作规程', 'pdf', 286720, 'demo/docs/drinking-water-sop.pdf', '演示制度文件。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW()),
(850002, '采样封签管理制度', '管理制度', 'docx', 143360, 'demo/docs/seal-management.docx', '演示制度文件。', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW()),
(850003, '仪器期间核查记录模板', '表单模板', 'xlsx', 102400, 'demo/docs/instrument-check-template.xlsx', '演示模板文件。', 0, 1001, '系统管理员', NOW() - INTERVAL 12 DAY, 1001, '系统管理员', NOW());

INSERT INTO lab_document_share (id, document_id, user_id, username, real_name, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
VALUES
(860001, 850001, 1201, 'detector01', '王检测', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860002, 850001, 1202, 'detector02', '赵检测', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860003, 850002, 1101, 'sampler01', '陈采样', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860004, 850002, 1102, 'sampler02', '李采样', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

-- 8. 批量演示数据：保证各业务列表、统计页面都有 100+ 可分页、可筛选的数据。
DROP TEMPORARY TABLE IF EXISTS tmp_demo_seq;
CREATE TEMPORARY TABLE tmp_demo_seq (n INT PRIMARY KEY);

INSERT INTO tmp_demo_seq (n)
SELECT seq
FROM (
    SELECT ones.i + tens.i * 10 + hundreds.i * 100 + 1 AS seq
    FROM (SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
    CROSS JOIN (SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
    CROSS JOIN (SELECT 0 AS i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
) numbers
WHERE seq <= 600;

INSERT INTO lab_user (id, username, password, real_name, org_id, org_name, role_code, phone, avatar_url, status, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    880000 + n,
    CASE MOD(n, 4)
        WHEN 0 THEN CONCAT('demo_sampler_', LPAD(n, 3, '0'))
        WHEN 1 THEN CONCAT('demo_detector_', LPAD(n, 3, '0'))
        WHEN 2 THEN CONCAT('demo_reviewer_', LPAD(n, 3, '0'))
        ELSE CONCAT('demo_reporter_', LPAD(n, 3, '0'))
    END,
    'e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7',
    CASE MOD(n, 4)
        WHEN 0 THEN CONCAT('演示采样员', LPAD(n, 3, '0'))
        WHEN 1 THEN CONCAT('演示检测员', LPAD(n, 3, '0'))
        WHEN 2 THEN CONCAT('演示审核员', LPAD(n, 3, '0'))
        ELSE CONCAT('演示报告员', LPAD(n, 3, '0'))
    END,
    CASE MOD(n, 4) WHEN 0 THEN 802 ELSE 803 END,
    CASE MOD(n, 4) WHEN 0 THEN '采样组' ELSE '检测审核组' END,
    CASE MOD(n, 4) WHEN 0 THEN 'SAMPLER' WHEN 1 THEN 'DETECTOR' WHEN 2 THEN 'REVIEWER' ELSE 'REPORTER' END,
    CONCAT('139', LPAD(n, 8, '0')),
    NULL,
    1,
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 120
ON DUPLICATE KEY UPDATE
real_name = VALUES(real_name),
org_id = VALUES(org_id),
org_name = VALUES(org_name),
role_code = VALUES(role_code),
phone = VALUES(phone),
status = VALUES(status),
deleted = 0,
updated_by = VALUES(updated_by),
updated_name = VALUES(updated_name),
updated_time = NOW();

INSERT INTO lab_detection_project_group (id, group_name, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    705100 + n,
    CONCAT('批量演示项目组-', LPAD(n, 3, '0')),
    1,
    '批量造数检测项目组。',
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 120;

INSERT INTO lab_detection_parameter (id, parameter_name, standard_min, standard_max, unit, exceed_rule, reference_standard, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    701000 + n,
    CONCAT('演示参数', LPAD(n, 3, '0')),
    0.00,
    10.00 + MOD(n, 15),
    CASE MOD(n, 4) WHEN 0 THEN 'mg/L' WHEN 1 THEN 'NTU' WHEN 2 THEN '度' ELSE '' END,
    CASE MOD(n, 3) WHEN 0 THEN 'OUT_OF_RANGE' ELSE 'GREATER_THAN_MAX' END,
    'GB 5749-2022',
    1,
    '批量造数检测参数',
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 120;

INSERT INTO lab_detection_method (id, method_name, method_code, parameter_id, parameter_name, standard_code, method_basis, apply_scope, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    711000 + n,
    CONCAT('演示检测方法-', LPAD(n, 3, '0')),
    CONCAT('DEMO-BATCH-', LPAD(n, 3, '0')),
    701000 + n,
    CONCAT('演示参数', LPAD(n, 3, '0')),
    CONCAT('DEMO/STD-', LPAD(n, 3, '0')),
    '按检测步骤完成样品预处理、仪器校准、读数记录与结果复核。',
    '用于批量演示数据的水质检测方法。',
    1,
    '批量造数检测方法',
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 120;

INSERT INTO lab_detection_type (id, type_name, group_id, group_name, detector_id, detector_name, parameter_ids, parameter_names, parameter_method_bindings, enabled, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    721000 + n,
    CONCAT('演示检测套餐-', LPAD(n, 3, '0')),
    CASE MOD(n, 4) WHEN 0 THEN 705001 WHEN 1 THEN 705002 WHEN 2 THEN 705003 ELSE 705004 END,
    CASE MOD(n, 4) WHEN 0 THEN '常规理化组' WHEN 1 THEN '消毒指标组' WHEN 2 THEN '金属指标组' ELSE '感官指标组' END,
    CASE MOD(n, 3) WHEN 0 THEN 1201 WHEN 1 THEN 1202 ELSE 1203 END,
    CASE MOD(n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END,
    '700001,700002,700003',
    'pH,浊度,余氯',
    '[{"parameterId":700001,"methodIds":[710001]},{"parameterId":700002,"methodIds":[710002]},{"parameterId":700003,"methodIds":[710003]}]',
    1,
    '批量造数检测套餐',
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 120;

INSERT INTO lab_detection_step (id, type_id, type_name, step_name, step_order, step_description, reagent_requirement, operation_requirement, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    731000 + n,
    721000 + n,
    CONCAT('演示检测套餐-', LPAD(n, 3, '0')),
    '批量演示检测步骤',
    1,
    '完成样品接收、预处理、检测读数、结果复核与记录归档。',
    '按检测参数准备标准溶液和质控样。',
    '检测人员按步骤录入原始值，异常值需备注说明。',
    '批量造数检测步骤',
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 120;

INSERT INTO lab_monitoring_point (id, point_name, longitude, latitude, region_name, service_population, frequency_type, owner_id, owner_name, contact_phone, point_type, point_status, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    900000 + n,
    CONCAT('批量演示监测点-', LPAD(n, 3, '0')),
    CONCAT('115.', LPAD(1000 + n, 4, '0')),
    CONCAT('30.', LPAD(2000 + n, 4, '0')),
    CONCAT('演示片区-', LPAD(MOD(n, 12) + 1, 2, '0')),
    5000 + n * 120,
    CASE MOD(n, 3) WHEN 0 THEN 'DAILY' WHEN 1 THEN 'WEEKLY' ELSE 'MONTHLY' END,
    CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END,
    CASE MOD(n, 2) WHEN 0 THEN '13810001001' ELSE '13810001002' END,
    CASE MOD(n, 3) WHEN 0 THEN 'FACTORY' WHEN 1 THEN 'RAW' ELSE 'TERMINAL' END,
    CASE MOD(n, 13) WHEN 0 THEN 'DISABLED' ELSE 'ENABLED' END,
    0,
    1001,
    '系统管理员',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY,
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 160;

INSERT INTO lab_sampling_plan (id, plan_name, point_id, point_name, start_time, end_time, sampler_id, sampler_name, sampling_type, sample_type, cycle_type, plan_status, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    910000 + n,
    CONCAT('批量演示采样计划-', LPAD(n, 3, '0')),
    900000 + MOD(n - 1, 160) + 1,
    CONCAT('批量演示监测点-', LPAD(MOD(n - 1, 160) + 1, 3, '0')),
    CURDATE() - INTERVAL (MOD(n, 60) + 10) DAY,
    CURDATE() + INTERVAL (MOD(n, 120) + 30) DAY,
    CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END,
    'ROUTINE',
    CASE MOD(n, 3) WHEN 0 THEN 'FACTORY' WHEN 1 THEN 'RAW' ELSE 'TERMINAL' END,
    CASE MOD(n, 4) WHEN 0 THEN 'DAILY' WHEN 1 THEN 'WEEKLY' WHEN 2 THEN 'MONTHLY' ELSE 'ONCE' END,
    CASE MOD(n, 5) WHEN 0 THEN 'ACTIVE' WHEN 1 THEN 'DISPATCHED' WHEN 2 THEN 'UNPUBLISHED' WHEN 3 THEN 'PAUSED' ELSE 'COMPLETED' END,
    '批量演示采样计划，可用于分页、筛选和统计验证。',
    0,
    1001,
    '系统管理员',
    NOW() - INTERVAL (MOD(n, 60) + 10) DAY,
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 240;

INSERT INTO lab_sampling_task (id, task_no, plan_id, point_id, point_name, sampling_time, sampler_id, sampler_name, sample_type, seal_no, sample_register_status, sample_id, detection_items, task_status, started_time, onsite_metrics, photo_urls, abandon_reason, finished_time, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    920000 + n,
    CONCAT('TASK-BATCH-', LPAD(n, 4, '0')),
    910000 + MOD(n - 1, 240) + 1,
    900000 + MOD(n - 1, 160) + 1,
    CONCAT('批量演示监测点-', LPAD(MOD(n - 1, 160) + 1, 3, '0')),
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY,
    CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END,
    CASE MOD(n, 3) WHEN 0 THEN 'FACTORY' WHEN 1 THEN 'RAW' ELSE 'TERMINAL' END,
    CONCAT('SEAL-BATCH-', LPAD(n, 4, '0')),
    'REGISTERED',
    930000 + n,
    CASE MOD(n, 4) WHEN 0 THEN '出厂水常规九项' WHEN 1 THEN '原水重点五项' WHEN 2 THEN '管网末梢四项' ELSE '应急复检套餐' END,
    'COMPLETED',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 1 HOUR,
    CONCAT('水温 ', 18 + MOD(n, 8), '.5℃；现场状态正常'),
    NULL,
    NULL,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 2 HOUR,
    '批量演示采样任务，已完成并生成样品。',
    0,
    CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY,
    CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 2 HOUR
FROM tmp_demo_seq;

INSERT INTO lab_sample (id, sample_no, seal_no, task_id, point_id, point_name, sample_type, detection_items, detection_type_id, detection_type_name, detection_config_snapshot, review_flow_id, review_flow_name, publish_flow_id, publish_flow_name, sampling_time, seal_time, sampler_id, sampler_name, weather, storage_condition, sample_status, result_summary, remark, trace_log, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    930000 + n,
    CONCAT('SAMPLE-BATCH-', LPAD(n, 4, '0')),
    CONCAT('SEAL-BATCH-', LPAD(n, 4, '0')),
    920000 + n,
    900000 + MOD(n - 1, 160) + 1,
    CONCAT('批量演示监测点-', LPAD(MOD(n - 1, 160) + 1, 3, '0')),
    CASE MOD(n, 3) WHEN 0 THEN 'FACTORY' WHEN 1 THEN 'RAW' ELSE 'TERMINAL' END,
    CASE MOD(n, 4) WHEN 0 THEN '出厂水常规九项' WHEN 1 THEN '原水重点五项' WHEN 2 THEN '管网末梢四项' ELSE '应急复检套餐' END,
    CASE MOD(n, 4) WHEN 0 THEN 720001 WHEN 1 THEN 720002 WHEN 2 THEN 720003 ELSE 720004 END,
    CASE MOD(n, 4) WHEN 0 THEN '出厂水常规九项' WHEN 1 THEN '原水重点五项' WHEN 2 THEN '管网末梢四项' ELSE '应急复检套餐' END,
    NULL,
    9601,
    '常规三级审核',
    9602,
    '报告发布审批',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 2 HOUR,
    CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END,
    CASE MOD(n, 4) WHEN 0 THEN '晴' WHEN 1 THEN '多云' WHEN 2 THEN '小雨' ELSE '阴' END,
    CASE MOD(n, 3) WHEN 0 THEN '冷藏避光' WHEN 1 THEN '常温送检' ELSE '冷藏送检' END,
    CASE MOD(n, 5) WHEN 0 THEN 'LOGGED' WHEN 1 THEN 'LOGGED' WHEN 2 THEN 'REVIEWING' WHEN 3 THEN 'RETEST' ELSE 'COMPLETED' END,
    CASE MOD(n, 5) WHEN 2 THEN '异常' WHEN 3 THEN '异常' WHEN 4 THEN '正常' ELSE NULL END,
    CASE MOD(n, 5)
        WHEN 0 THEN '批量演示：待分配检测人员。'
        WHEN 1 THEN '批量演示：已分配，待检测。'
        WHEN 2 THEN '批量演示：检测已提交，待审核。'
        WHEN 3 THEN '批量演示：审核驳回，待重检。'
        ELSE '批量演示：审核通过，可生成或发布报告。'
    END,
    CONCAT('样品登录：封签号=SEAL-BATCH-', LPAD(n, 4, '0'), '；批量演示链路数据。'),
    0,
    CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 2 HOUR,
    CASE MOD(n, 5) WHEN 4 THEN 1301 WHEN 3 THEN 1302 ELSE CASE MOD(n, 2) WHEN 0 THEN 1101 ELSE 1102 END END,
    CASE MOD(n, 5) WHEN 4 THEN '刘审核' WHEN 3 THEN '何审核' ELSE CASE MOD(n, 2) WHEN 0 THEN '陈采样' ELSE '李采样' END END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 3 HOUR
FROM tmp_demo_seq;

INSERT INTO lab_detection_record (id, sample_id, sample_no, seal_no, detection_type_id, detection_type_name, detection_time, detector_id, detector_name, detection_result, abnormal_remark, detection_status, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    940000 + n,
    930000 + n,
    CONCAT('SAMPLE-BATCH-', LPAD(n, 4, '0')),
    CONCAT('SEAL-BATCH-', LPAD(n, 4, '0')),
    CASE MOD(n, 4) WHEN 0 THEN 720001 WHEN 1 THEN 720002 WHEN 2 THEN 720003 ELSE 720004 END,
    CASE MOD(n, 4) WHEN 0 THEN '出厂水常规九项' WHEN 1 THEN '原水重点五项' WHEN 2 THEN '管网末梢四项' ELSE '应急复检套餐' END,
    CASE WHEN MOD(n, 5) IN (0, 1) THEN NULL ELSE NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 4 HOUR END,
    CASE WHEN MOD(n, 5) = 0 THEN NULL ELSE CASE MOD(n, 3) WHEN 0 THEN 1201 WHEN 1 THEN 1202 ELSE 1203 END END,
    CASE WHEN MOD(n, 5) = 0 THEN NULL ELSE CASE MOD(n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END END,
    CASE MOD(n, 5) WHEN 2 THEN 'ABNORMAL' WHEN 3 THEN 'ABNORMAL' WHEN 4 THEN 'NORMAL' ELSE NULL END,
    CASE MOD(n, 5) WHEN 2 THEN '浊度或氨氮存在轻微超限，待审核确认。' WHEN 3 THEN '审核驳回后等待复检。' WHEN 4 THEN '全部指标正常。' ELSE NULL END,
    CASE MOD(n, 5) WHEN 0 THEN 'WAIT_ASSIGN' WHEN 1 THEN 'WAIT_DETECT' WHEN 2 THEN 'SUBMITTED' WHEN 3 THEN 'REJECTED' ELSE 'APPROVED' END,
    0,
    CASE WHEN MOD(n, 5) = 0 THEN 1101 ELSE CASE MOD(n, 3) WHEN 0 THEN 1201 WHEN 1 THEN 1202 ELSE 1203 END END,
    CASE WHEN MOD(n, 5) = 0 THEN '陈采样' ELSE CASE MOD(n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 3 HOUR,
    CASE MOD(n, 5) WHEN 4 THEN 1301 WHEN 3 THEN 1302 ELSE CASE MOD(n, 3) WHEN 0 THEN 1201 WHEN 1 THEN 1202 ELSE 1203 END END,
    CASE MOD(n, 5) WHEN 4 THEN '刘审核' WHEN 3 THEN '何审核' ELSE CASE MOD(n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 5 HOUR
FROM tmp_demo_seq;

INSERT INTO lab_detection_item (id, record_id, parameter_id, parameter_name, standard_min, standard_max, result_value, unit, reference_standard, method_id, method_name, detector_id, detector_name, item_status, exceed_flag, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    950000 + s.n * 10 + p.n,
    940000 + s.n,
    CASE p.n WHEN 1 THEN 700001 WHEN 2 THEN 700002 ELSE 700003 END,
    CASE p.n WHEN 1 THEN 'pH' WHEN 2 THEN '浊度' ELSE '余氯' END,
    CASE p.n WHEN 1 THEN 6.50 ELSE 0.00 END,
    CASE p.n WHEN 1 THEN 8.50 WHEN 2 THEN 1.00 ELSE 2.00 END,
    CASE WHEN MOD(s.n, 5) IN (0, 1) THEN NULL ELSE CASE p.n WHEN 1 THEN 7.00 + MOD(s.n, 8) / 10 WHEN 2 THEN 0.30 + MOD(s.n, 12) / 10 ELSE 0.20 + MOD(s.n, 6) / 10 END END,
    CASE p.n WHEN 1 THEN '' WHEN 2 THEN 'NTU' ELSE 'mg/L' END,
    'GB 5749-2022',
    CASE p.n WHEN 1 THEN 710001 WHEN 2 THEN 710002 ELSE 710003 END,
    CASE p.n WHEN 1 THEN '玻璃电极法' WHEN 2 THEN '散射光浊度法' ELSE 'DPD 分光光度法' END,
    CASE WHEN MOD(s.n, 5) = 0 THEN NULL ELSE CASE MOD(s.n, 3) WHEN 0 THEN 1201 WHEN 1 THEN 1202 ELSE 1203 END END,
    CASE WHEN MOD(s.n, 5) = 0 THEN NULL ELSE CASE MOD(s.n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END END,
    CASE MOD(s.n, 5) WHEN 0 THEN 'WAIT_ASSIGN' WHEN 1 THEN 'WAIT_DETECT' WHEN 2 THEN 'SUBMITTED' WHEN 3 THEN 'REJECTED' ELSE 'APPROVED' END,
    CASE WHEN MOD(s.n, 5) IN (2, 3) AND p.n = 2 THEN 1 ELSE 0 END,
    0,
    CASE WHEN MOD(s.n, 5) = 0 THEN 1101 ELSE CASE MOD(s.n, 3) WHEN 0 THEN 1201 WHEN 1 THEN 1202 ELSE 1203 END END,
    CASE WHEN MOD(s.n, 5) = 0 THEN '陈采样' ELSE CASE MOD(s.n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END END,
    NOW() - INTERVAL (MOD(s.n, 90) + 1) DAY + INTERVAL 3 HOUR,
    CASE MOD(s.n, 5) WHEN 4 THEN 1301 WHEN 3 THEN 1302 ELSE CASE MOD(s.n, 3) WHEN 0 THEN 1201 WHEN 1 THEN 1202 ELSE 1203 END END,
    CASE MOD(s.n, 5) WHEN 4 THEN '刘审核' WHEN 3 THEN '何审核' ELSE CASE MOD(s.n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END END,
    NOW() - INTERVAL (MOD(s.n, 90) + 1) DAY + INTERVAL 5 HOUR
FROM tmp_demo_seq s
JOIN (
    SELECT 1 AS n
    UNION ALL SELECT 2
    UNION ALL SELECT 3
) p ON 1 = 1;

INSERT INTO lab_review_record (id, detection_record_id, sample_id, sample_no, seal_no, reviewer_id, reviewer_name, review_time, review_result, reject_reason, review_remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    960000 + n,
    940000 + n,
    930000 + n,
    CONCAT('SAMPLE-BATCH-', LPAD(n, 4, '0')),
    CONCAT('SEAL-BATCH-', LPAD(n, 4, '0')),
    CASE MOD(n, 2) WHEN 0 THEN 1301 ELSE 1302 END,
    CASE MOD(n, 2) WHEN 0 THEN '刘审核' ELSE '何审核' END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 6 HOUR,
    CASE MOD(n, 5) WHEN 3 THEN 'REJECTED' ELSE 'APPROVED' END,
    CASE MOD(n, 5) WHEN 3 THEN '批量演示：检测结果异常，要求复检。' ELSE NULL END,
    CASE MOD(n, 5) WHEN 3 THEN '已驳回至检测环节。' ELSE '指标稳定，审核通过。' END,
    0,
    CASE MOD(n, 2) WHEN 0 THEN 1301 ELSE 1302 END,
    CASE MOD(n, 2) WHEN 0 THEN '刘审核' ELSE '何审核' END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 6 HOUR,
    CASE MOD(n, 2) WHEN 0 THEN 1301 ELSE 1302 END,
    CASE MOD(n, 2) WHEN 0 THEN '刘审核' ELSE '何审核' END,
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 6 HOUR
FROM tmp_demo_seq
WHERE MOD(n, 5) IN (3, 4);

INSERT INTO lab_report (id, report_name, report_type, generated_time, sample_id, sample_no, seal_no, detection_record_id, report_status, published_time, published_by, published_by_name, push_status, last_push_time, last_push_message, file_path, content_snapshot, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    970000 + n,
    CONCAT('SAMPLE-BATCH-', LPAD(n, 4, '0'), '-检测报告'),
    'DAILY',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 7 HOUR,
    930000 + n,
    CONCAT('SAMPLE-BATCH-', LPAD(n, 4, '0')),
    CONCAT('SEAL-BATCH-', LPAD(n, 4, '0')),
    940000 + n,
    CASE MOD(n, 5) WHEN 3 THEN 'DRAFT' ELSE CASE MOD(n, 2) WHEN 0 THEN 'PUBLISHED' ELSE 'GENERATED' END END,
    CASE WHEN MOD(n, 5) = 4 AND MOD(n, 2) = 0 THEN NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 8 HOUR ELSE NULL END,
    CASE WHEN MOD(n, 5) = 4 AND MOD(n, 2) = 0 THEN 1401 ELSE NULL END,
    CASE WHEN MOD(n, 5) = 4 AND MOD(n, 2) = 0 THEN '孙报告' ELSE NULL END,
    CASE WHEN MOD(n, 5) = 4 AND MOD(n, 2) = 0 THEN 'SUCCESS' ELSE 'PENDING' END,
    CASE WHEN MOD(n, 5) = 4 AND MOD(n, 2) = 0 THEN NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 8 HOUR ELSE NULL END,
    CASE WHEN MOD(n, 5) = 4 AND MOD(n, 2) = 0 THEN '已推送至业务系统。' ELSE '报告已生成，待后续处理。' END,
    NULL,
    CONCAT('样品编号：SAMPLE-BATCH-', LPAD(n, 4, '0'), '\n检测类型：', CASE MOD(n, 4) WHEN 0 THEN '出厂水常规九项' WHEN 1 THEN '原水重点五项' WHEN 2 THEN '管网末梢四项' ELSE '应急复检套餐' END, '\n检测结果：', CASE MOD(n, 5) WHEN 3 THEN '待重检' ELSE '正常' END),
    0,
    1401,
    '孙报告',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 7 HOUR,
    1401,
    '孙报告',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 8 HOUR
FROM tmp_demo_seq
WHERE MOD(n, 5) IN (3, 4);

INSERT INTO lab_report_push_record (id, report_id, sample_id, sample_no, seal_no, recipient_user_id, recipient_name, recipient_phone, push_channel, push_status, push_message, push_time, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    980000 + n,
    970000 + n,
    930000 + n,
    CONCAT('SAMPLE-BATCH-', LPAD(n, 4, '0')),
    CONCAT('SEAL-BATCH-', LPAD(n, 4, '0')),
    1401,
    '孙报告',
    '13810004001',
    'UNIFIED',
    'SUCCESS',
    '批量演示报告推送成功。',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 8 HOUR,
    0,
    1401,
    '孙报告',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 8 HOUR,
    1401,
    '孙报告',
    NOW() - INTERVAL (MOD(n, 90) + 1) DAY + INTERVAL 8 HOUR
FROM tmp_demo_seq
WHERE MOD(n, 5) = 4 AND MOD(n, 2) = 0;

INSERT INTO lab_instrument (id, instrument_name, instrument_model, manufacturer, purchase_date, service_life_years, calibration_cycle, owner_name, instrument_status, storage_location, certificate_url, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    990000 + n,
    CONCAT('批量演示仪器-', LPAD(n, 3, '0')),
    CONCAT('MODEL-', LPAD(n, 3, '0')),
    CASE MOD(n, 4) WHEN 0 THEN '岛津' WHEN 1 THEN 'HACH' WHEN 2 THEN '梅特勒' ELSE '赛默飞' END,
    CURDATE() - INTERVAL (365 + MOD(n, 1200)) DAY,
    5 + MOD(n, 6),
    CASE MOD(n, 3) WHEN 0 THEN '6个月' WHEN 1 THEN '12个月' ELSE '24个月' END,
    CASE MOD(n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END,
    CASE MOD(n, 4) WHEN 0 THEN 'NORMAL' WHEN 1 THEN 'CALIBRATING' WHEN 2 THEN 'MAINTENANCE' ELSE 'DISABLED' END,
    CONCAT('演示实验室 ', CHAR(65 + MOD(n, 4)), '-', LPAD(MOD(n, 30) + 1, 2, '0')),
    NULL,
    '批量演示仪器数据。',
    0,
    1001,
    '系统管理员',
    NOW() - INTERVAL (MOD(n, 180) + 1) DAY,
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 140;

-- 修正已执行过旧脚本时残留的非法枚举值，避免页面直接显示英文码值。
UPDATE lab_monitoring_point
SET frequency_type = 'MONTHLY',
    updated_by = 1001,
    updated_name = '系统管理员',
    updated_time = NOW()
WHERE frequency_type NOT IN ('DAILY', 'WEEKLY', 'MONTHLY');

UPDATE lab_monitoring_point
SET point_status = 'DISABLED',
    updated_by = 1001,
    updated_name = '系统管理员',
    updated_time = NOW()
WHERE point_status NOT IN ('ENABLED', 'DISABLED');

UPDATE lab_instrument
SET instrument_status = 'DISABLED',
    updated_by = 1001,
    updated_name = '系统管理员',
    updated_time = NOW()
WHERE instrument_status NOT IN ('NORMAL', 'DISABLED', 'MAINTENANCE', 'CALIBRATING');

INSERT INTO lab_instrument_maintenance (id, instrument_id, instrument_name, maintenance_time, maintenance_reason, maintainer_name, maintenance_company, maintenance_result, maintenance_cost, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    991000 + n,
    990000 + n,
    CONCAT('批量演示仪器-', LPAD(n, 3, '0')),
    NOW() - INTERVAL (MOD(n, 120) + 1) DAY,
    CASE MOD(n, 3) WHEN 0 THEN '例行校准。' WHEN 1 THEN '检测重复性下降，申请维护。' ELSE '期间核查记录。' END,
    CASE MOD(n, 3) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' ELSE '周检测' END,
    CASE MOD(n, 2) WHEN 0 THEN '内部校准' ELSE '武汉精仪维保有限公司' END,
    CASE MOD(n, 3) WHEN 0 THEN '校准通过。' WHEN 1 THEN '已维护，待复核。' ELSE '核查结果正常。' END,
    100.00 + MOD(n, 20) * 35.00,
    '批量演示维修/校准记录。',
    0,
    1001,
    '系统管理员',
    NOW() - INTERVAL (MOD(n, 120) + 1) DAY,
    1001,
    '系统管理员',
    NOW() - INTERVAL (MOD(n, 120) + 1) DAY
FROM tmp_demo_seq
WHERE n <= 140;

INSERT INTO lab_document (id, document_name, document_category, file_type, file_size, file_url, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    992000 + n,
    CONCAT('批量演示资料-', LPAD(n, 3, '0')),
    CASE MOD(n, 4) WHEN 0 THEN '操作规程' WHEN 1 THEN '管理制度' WHEN 2 THEN '表单模板' ELSE '培训资料' END,
    CASE MOD(n, 4) WHEN 0 THEN 'pdf' WHEN 1 THEN 'docx' WHEN 2 THEN 'xlsx' ELSE 'pptx' END,
    102400 + n * 2048,
    CONCAT('demo/docs/batch-demo-', LPAD(n, 3, '0'), '.', CASE MOD(n, 4) WHEN 0 THEN 'pdf' WHEN 1 THEN 'docx' WHEN 2 THEN 'xlsx' ELSE 'pptx' END),
    '批量演示文档资料。',
    0,
    1001,
    '系统管理员',
    NOW() - INTERVAL (MOD(n, 180) + 1) DAY,
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 140;

INSERT INTO lab_document_share (id, document_id, user_id, username, real_name, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time)
SELECT
    993000 + n,
    992000 + n,
    CASE MOD(n, 4) WHEN 0 THEN 1201 WHEN 1 THEN 1202 WHEN 2 THEN 1101 ELSE 1102 END,
    CASE MOD(n, 4) WHEN 0 THEN 'detector01' WHEN 1 THEN 'detector02' WHEN 2 THEN 'sampler01' ELSE 'sampler02' END,
    CASE MOD(n, 4) WHEN 0 THEN '王检测' WHEN 1 THEN '赵检测' WHEN 2 THEN '陈采样' ELSE '李采样' END,
    0,
    1001,
    '系统管理员',
    NOW(),
    1001,
    '系统管理员',
    NOW()
FROM tmp_demo_seq
WHERE n <= 140;

SET FOREIGN_KEY_CHECKS = 1;

-- 9. 造数结果快速核对。
SELECT 'users' AS module_name, COUNT(*) AS total FROM lab_user WHERE deleted = 0
UNION ALL SELECT 'detection_project_groups', COUNT(*) FROM lab_detection_project_group WHERE deleted = 0
UNION ALL SELECT 'detection_parameters', COUNT(*) FROM lab_detection_parameter WHERE deleted = 0
UNION ALL SELECT 'detection_methods', COUNT(*) FROM lab_detection_method WHERE deleted = 0
UNION ALL SELECT 'detection_types', COUNT(*) FROM lab_detection_type WHERE deleted = 0
UNION ALL SELECT 'detection_steps', COUNT(*) FROM lab_detection_step WHERE deleted = 0
UNION ALL SELECT 'monitoring_points', COUNT(*) FROM lab_monitoring_point WHERE deleted = 0
UNION ALL SELECT 'sampling_plans', COUNT(*) FROM lab_sampling_plan WHERE deleted = 0
UNION ALL SELECT 'sampling_tasks', COUNT(*) FROM lab_sampling_task WHERE deleted = 0
UNION ALL SELECT 'samples', COUNT(*) FROM lab_sample WHERE deleted = 0
UNION ALL SELECT 'detection_records', COUNT(*) FROM lab_detection_record WHERE deleted = 0
UNION ALL SELECT 'detection_items', COUNT(*) FROM lab_detection_item WHERE deleted = 0
UNION ALL SELECT 'reviews', COUNT(*) FROM lab_review_record WHERE deleted = 0
UNION ALL SELECT 'reports', COUNT(*) FROM lab_report WHERE deleted = 0
UNION ALL SELECT 'report_push_records', COUNT(*) FROM lab_report_push_record WHERE deleted = 0
UNION ALL SELECT 'instruments', COUNT(*) FROM lab_instrument WHERE deleted = 0
UNION ALL SELECT 'instrument_maintenance', COUNT(*) FROM lab_instrument_maintenance WHERE deleted = 0
UNION ALL SELECT 'documents', COUNT(*) FROM lab_document WHERE deleted = 0
UNION ALL SELECT 'document_shares', COUNT(*) FROM lab_document_share WHERE deleted = 0;
