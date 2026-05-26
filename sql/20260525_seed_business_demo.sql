-- 业务演示数据补充脚本
-- 用途：在清理业务数据后，重建一批可串起来的演示数据，覆盖采样、样品、检测、审核、报告、设备、文档、日志等节点
-- 说明：本脚本默认系统配置数据已保留，尤其是角色、用户、流程配置、检测配置、字典等基础数据

USE yx_lab;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 先清理业务数据，保留系统管理配置
TRUNCATE TABLE lab_report_push_record;
TRUNCATE TABLE lab_report;
TRUNCATE TABLE lab_review_record;
TRUNCATE TABLE lab_detection_item;
TRUNCATE TABLE lab_detection_record;
TRUNCATE TABLE lab_sample;
TRUNCATE TABLE lab_sampling_task;
TRUNCATE TABLE lab_sampling_plan;
TRUNCATE TABLE lab_monitoring_point;
TRUNCATE TABLE lab_instrument_maintenance;
TRUNCATE TABLE lab_instrument;
TRUNCATE TABLE lab_document_share;
TRUNCATE TABLE lab_document;
TRUNCATE TABLE lab_login_log;

-- 取默认流程配置，方便样品登录、审核记录关联
SELECT id INTO @reviewFlowId
FROM lab_flow_config
WHERE flow_type = 'REVIEW' AND status = 1
ORDER BY default_flag DESC, id
LIMIT 1;

SELECT flow_name INTO @reviewFlowName
FROM lab_flow_config
WHERE id = @reviewFlowId
LIMIT 1;

SELECT id INTO @reviewNodeId
FROM lab_flow_node
WHERE flow_id = @reviewFlowId AND required_flag = 1
ORDER BY node_order
LIMIT 1;

SELECT node_name, node_order, required_flag
INTO @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag
FROM lab_flow_node
WHERE id = @reviewNodeId
LIMIT 1;

SELECT id INTO @publishFlowId
FROM lab_flow_config
WHERE flow_type = 'PUBLISH' AND status = 1
ORDER BY default_flag DESC, id
LIMIT 1;

SELECT flow_name INTO @publishFlowName
FROM lab_flow_config
WHERE id = @publishFlowId
LIMIT 1;

-- 清理本次演示数据，便于重复执行。
DELETE FROM lab_report_push_record WHERE id BETWEEN 820001 AND 820010;
DELETE FROM lab_report WHERE id BETWEEN 810001 AND 810010;
DELETE FROM lab_review_record WHERE id BETWEEN 800001 AND 800010;
DELETE FROM lab_detection_item WHERE id BETWEEN 990001 AND 990100;
DELETE FROM lab_detection_record WHERE id BETWEEN 980001 AND 980010;
DELETE FROM lab_sample WHERE id BETWEEN 970001 AND 970010;
DELETE FROM lab_sampling_task WHERE id BETWEEN 960001 AND 960020;
DELETE FROM lab_sampling_plan WHERE id BETWEEN 950001 AND 950020;
DELETE FROM lab_monitoring_point WHERE id BETWEEN 940001 AND 940020;
DELETE FROM lab_instrument_maintenance WHERE id BETWEEN 840001 AND 840020;
DELETE FROM lab_instrument WHERE id BETWEEN 830001 AND 830020;
DELETE FROM lab_document_share WHERE id BETWEEN 860001 AND 860020;
DELETE FROM lab_document WHERE id BETWEEN 850001 AND 850020;
DELETE FROM lab_login_log WHERE id BETWEEN 870001 AND 870020;

-- 1. 监测点位
INSERT INTO lab_monitoring_point (
    id, point_name, longitude, latitude, region_name, service_population, frequency_type,
    owner_id, owner_name, contact_phone, point_type, point_status, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(940001, '城东水厂出厂水', '115.2121', '30.2211', '城东片区', 36000, 'DAILY', 1101, '陈采样', '13810001001', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 30 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(940002, '城西水厂出厂水', '115.1781', '30.2051', '城西片区', 42000, 'DAILY', 1102, '李采样', '13810001002', 'FACTORY', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 28 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(940003, '富河原水取水口', '115.0912', '30.1548', '富河流域', 68000, 'WEEKLY', 1101, '陈采样', '13810001001', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 25 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(940004, '兴国大道管网末梢', '115.2266', '30.2199', '兴国大道', 12000, 'DAILY', 1102, '李采样', '13810001002', 'TERMINAL', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(940005, '莲花湖社区末梢点', '115.2442', '30.2366', '莲花湖社区', 9800, 'WEEKLY', 1101, '陈采样', '13810001001', 'TERMINAL', 'DISABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 15 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(940006, '应急加密监测点', '115.2622', '30.2411', '城南片区', 6000, 'DAILY', 1102, '李采样', '13810001002', 'RAW', 'ENABLED', 0, 1001, '系统管理员', NOW() - INTERVAL 12 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY);

-- 2. 采样计划
INSERT INTO lab_sampling_plan (
    id, plan_name, point_id, point_name, start_time, end_time, sampler_id, sampler_name,
    sampling_type, sample_type, cycle_type, plan_status, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(950001, '城东水厂每日采样计划', 940001, '城东水厂出厂水', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 1101, '陈采样', 'ROUTINE', 'FACTORY', 'DAILY', 'ACTIVE', '演示：启用中的每日计划。', 0, 1001, '系统管理员', NOW() - INTERVAL 10 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(950002, '城西水厂每日采样计划', 940002, '城西水厂出厂水', DATE_SUB(NOW(), INTERVAL 12 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), 1102, '李采样', 'ROUTINE', 'FACTORY', 'DAILY', 'PAUSED', '演示：暂停中的计划。', 0, 1001, '系统管理员', NOW() - INTERVAL 12 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(950003, '富河原水每周采样计划', 940003, '富河原水取水口', DATE_SUB(NOW(), INTERVAL 16 DAY), DATE_ADD(NOW(), INTERVAL 60 DAY), 1101, '陈采样', 'ROUTINE', 'RAW', 'WEEKLY', 'DISPATCHED', '演示：已派发等待执行。', 0, 1001, '系统管理员', NOW() - INTERVAL 16 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(950004, '管网末梢已完成计划', 940004, '兴国大道管网末梢', DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 1102, '李采样', 'ROUTINE', 'TERMINAL', 'DAILY', 'COMPLETED', '演示：已完成计划。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(950005, '莲花湖社区待发布计划', 940005, '莲花湖社区末梢点', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 35 DAY), 1101, '陈采样', 'ROUTINE', 'TERMINAL', 'WEEKLY', 'UNPUBLISHED', '演示：待发布计划。', 0, 1001, '系统管理员', NOW() - INTERVAL 3 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(950006, '应急加密监测执行计划', 940006, '应急加密监测点', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 15 DAY), 1102, '李采样', 'ROUTINE', 'RAW', 'DAILY', 'ACTIVE', '演示：额外的活跃计划。', 0, 1001, '系统管理员', NOW() - INTERVAL 5 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY);

-- 3. 采样任务
INSERT INTO lab_sampling_task (
    id, task_no, plan_id, point_id, point_name, sampling_time, sampler_id, sampler_name,
    sample_type, seal_no, sample_register_status, sample_id, detection_items, task_status,
    started_time, onsite_metrics, weather, temperature, photo_urls, abandon_reason,
    finished_time, remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(960001, 'TASK-202605-0001', 950001, 940001, '城东水厂出厂水', DATE_SUB(NOW(), INTERVAL 4 DAY), 1101, '陈采样', 'FACTORY', 'SEAL-202605-0001', 'REGISTERED', 970001, 'pH,浊度,余氯', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY), '现场指标：温度18.2，余氯0.48', '晴', '18.2', 'http://192.168.7.24:9010/resource/demo/photo-0001.jpg,http://192.168.7.24:9010/resource/demo/photo-0002.jpg', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), '演示：任务已完成并回填样品。', 0, 1101, '陈采样', NOW() - INTERVAL 4 DAY, 1101, '陈采样', NOW() - INTERVAL 3 DAY),
(960002, 'TASK-202605-0002', 950001, 940001, '城东水厂出厂水', DATE_SUB(NOW(), INTERVAL 3 DAY), 1101, '陈采样', 'FACTORY', NULL, 'UNREGISTERED', NULL, 'pH,浊度,余氯', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 3 DAY), '现场指标：温度19.1，浊度略高', '多云', '19.1', 'http://192.168.7.24:9010/resource/demo/photo-0003.jpg', NULL, NULL, '演示：执行中任务。', 0, 1101, '陈采样', NOW() - INTERVAL 3 DAY, 1101, '陈采样', NOW() - INTERVAL 2 DAY),
(960003, 'TASK-202605-0003', 950002, 940002, '城西水厂出厂水', DATE_SUB(NOW(), INTERVAL 7 DAY), 1102, '李采样', 'FACTORY', NULL, 'UNREGISTERED', NULL, 'pH,浊度,色度', 'ABANDONED', NULL, NULL, NULL, NULL, NULL, '现场道路施工，无法到达点位。', NULL, '演示：废弃任务。', 0, 1102, '李采样', NOW() - INTERVAL 7 DAY, 1102, '李采样', NOW() - INTERVAL 6 DAY),
(960004, 'TASK-202605-0004', 950003, 940003, '富河原水取水口', DATE_SUB(NOW(), INTERVAL 6 DAY), 1101, '陈采样', 'RAW', NULL, 'UNREGISTERED', NULL, 'pH,浊度,氨氮,高锰酸盐指数', 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '演示：待处理任务。', 0, 1101, '陈采样', NOW() - INTERVAL 6 DAY, 1101, '陈采样', NOW() - INTERVAL 5 DAY),
(960005, 'TASK-202605-0005', 950004, 940004, '兴国大道管网末梢', DATE_SUB(NOW(), INTERVAL 2 DAY), 1102, '李采样', 'TERMINAL', 'SEAL-202605-0002', 'REGISTERED', 970002, 'pH,浊度,余氯,肉眼可见物', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY), '现场指标：余氯0.03，需复核', '晴', '23.5', 'http://192.168.7.24:9010/resource/demo/photo-0004.jpg', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), '演示：已完成并登录样品。', 0, 1102, '李采样', NOW() - INTERVAL 2 DAY, 1102, '李采样', NOW() - INTERVAL 1 DAY),
(960006, 'TASK-202605-0006', 950005, 940005, '莲花湖社区末梢点', DATE_ADD(NOW(), INTERVAL 2 DAY), 1101, '陈采样', 'TERMINAL', NULL, 'UNREGISTERED', NULL, 'pH,浊度,余氯', 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '演示：未发布计划对应任务。', 0, 1101, '陈采样', NOW() - INTERVAL 1 DAY, 1101, '陈采样', NOW() - INTERVAL 1 DAY),
(960007, 'TASK-202605-0007', 950006, 940006, '应急加密监测点', DATE_SUB(NOW(), INTERVAL 1 DAY), 1102, '李采样', 'RAW', NULL, 'UNREGISTERED', NULL, 'pH,浊度,氨氮', 'IN_PROGRESS', DATE_SUB(NOW(), INTERVAL 1 DAY), '现场指标：温度21.0，天气闷热', '阴', '21.0', 'http://192.168.7.24:9010/resource/demo/photo-0005.jpg', NULL, NULL, '演示：进行中任务。', 0, 1102, '李采样', NOW() - INTERVAL 1 DAY, 1102, '李采样', NOW()),
(960008, 'TASK-202605-0008', 950006, 940006, '应急加密监测点', DATE_SUB(NOW(), INTERVAL 8 DAY), 1102, '李采样', 'RAW', 'SEAL-202605-0003', 'REGISTERED', 970003, 'pH,浊度,氨氮', 'COMPLETED', DATE_SUB(NOW(), INTERVAL 8 DAY), '现场指标：异常复核样', '阴', '20.6', 'http://192.168.7.24:9010/resource/demo/photo-0006.jpg', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY), '演示：重检关联任务。', 0, 1102, '李采样', NOW() - INTERVAL 8 DAY, 1102, '李采样', NOW() - INTERVAL 7 DAY);

-- 4. 样品主档
INSERT INTO lab_sample (
    id, sample_no, seal_no, task_id, point_id, point_name, sample_type, quality_control_type,
    detection_items, detection_type_id, detection_type_name, detection_config_snapshot,
    review_flow_id, review_flow_name, publish_flow_id, publish_flow_name,
    sampling_time, seal_time, sampler_id, sampler_name, weather, storage_condition,
    sample_status, result_summary, remark, trace_log, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(970001, '2026050001', 'SEAL-202605-0001', 960001, 940001, '城东水厂出厂水', 'FACTORY', 'PARALLEL', 'pH,浊度,余氯', 720001, '出厂水常规九项', '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","methodBasis":"校准 pH 计后直接测定。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","methodBasis":"使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD分光光度法","methodBasis":"加入 DPD 试剂显色后测定。"}]', @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 1101, '陈采样', '晴', '4℃冷藏', 'LOGGED', '待检测', '演示：样品已登录。', '样品登录：封签号=SEAL-202605-0001，样品编号=2026050001，点位=城东水厂出厂水，采样人=陈采样，采样时间=' , 0, 1101, '陈采样', NOW() - INTERVAL 4 DAY, 1101, '陈采样', NOW() - INTERVAL 3 DAY),
(970002, '2026050002', 'SEAL-202605-0002', 960005, 940004, '兴国大道管网末梢', 'TERMINAL', 'BLANK', 'pH,浊度,余氯,肉眼可见物', 720003, '管网末梢四项', '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","methodBasis":"校准 pH 计后直接测定。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","methodBasis":"使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD分光光度法","methodBasis":"加入 DPD 试剂显色后测定。"},{"parameterId":700007,"parameterName":"肉眼可见物","unit":"项","standardMin":0.00,"standardMax":0.00,"referenceStandard":"GB 5749-2022","methodId":710007,"methodName":"目视观察法","methodBasis":"自然光下观察是否存在可见物。"}]', @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 1102, '李采样', '晴', '4℃冷藏', 'REVIEWING', '待审核', '演示：结果待审查。', '样品登录：封签号=SEAL-202605-0002，样品编号=2026050002，点位=兴国大道管网末梢，采样人=李采样，采样时间=' , 0, 1102, '李采样', NOW() - INTERVAL 2 DAY, 1102, '李采样', NOW() - INTERVAL 1 DAY),
(970003, '2026050003', 'SEAL-202605-0003', 960008, 940006, '应急加密监测点', 'RAW', 'QUALITY_CONTROL', 'pH,浊度,氨氮', 720004, '应急复检套牌', '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","methodBasis":"校准 pH 计后直接测定。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","methodBasis":"使用浊度仪读取 NTU 值。"},{"parameterId":700004,"parameterName":"氨氮","unit":"mg/L","standardMin":0.00,"standardMax":0.50,"referenceStandard":"GB 5749-2022","methodId":710004,"methodName":"纳氏试剂分光光度法","methodBasis":"加入纳氏试剂显色后测定。"}]', @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), 1102, '李采样', '阴', '4℃冷藏', 'RETEST', '待重检', '演示：被驳回后进入重检。', '样品登录：封签号=SEAL-202605-0003，样品编号=2026050003，点位=应急加密监测点，采样人=李采样，采样时间=' , 0, 1102, '李采样', NOW() - INTERVAL 8 DAY, 1102, '李采样', NOW() - INTERVAL 7 DAY),
(970004, '2026050004', 'SEAL-202605-0004', NULL, 940003, '富河原水取水口', 'SOURCE_WATER', NULL, 'pH,浊度,氨氮,高锰酸盐指数', 720002, '原水重点五项', '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","methodBasis":"校准 pH 计后直接测定。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","methodBasis":"使用浊度仪读取 NTU 值。"},{"parameterId":700004,"parameterName":"氨氮","unit":"mg/L","standardMin":0.00,"standardMax":0.50,"referenceStandard":"GB 5749-2022","methodId":710004,"methodName":"纳氏试剂分光光度法","methodBasis":"加入纳氏试剂显色后测定。"},{"parameterId":700010,"parameterName":"高锰酸盐指数","unit":"mg/L","standardMin":0.00,"standardMax":3.00,"referenceStandard":"GB 5749-2022","methodId":710010,"methodName":"酸性高锰酸钾滴定法","methodBasis":"酸性条件下滴定计算耗氧量。"}]', @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 1101, '陈采样', '多云', '4℃冷藏', 'COMPLETED', '已完成', '演示：直接登录的样品。', '样品登录：封签号=SEAL-202605-0004，样品编号=2026050004，点位=富河原水取水口，采样人=陈采样，采样时间=' , 0, 1101, '陈采样', NOW() - INTERVAL 5 DAY, 1101, '陈采样', NOW() - INTERVAL 4 DAY),
(970005, '2026050005', 'SEAL-202605-0005', NULL, 940005, '莲花湖社区末梢点', 'TERMINAL', 'PARALLEL', 'pH,浊度,余氯', 720001, '出厂水常规九项', '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","methodBasis":"校准 pH 计后直接测定。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","methodBasis":"使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD分光光度法","methodBasis":"加入 DPD 试剂显色后测定。"}]', @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 1101, '陈采样', '阴', '4℃冷藏', 'LOGGED', '待检测', '演示：备用样品。', '样品登录：封签号=SEAL-202605-0005，样品编号=2026050005，点位=莲花湖社区末梢点，采样人=陈采样，采样时间=' , 0, 1101, '陈采样', NOW() - INTERVAL 1 DAY, 1101, '陈采样', NOW()),
(970006, '2026050006', 'SEAL-202605-0006', NULL, 940002, '城西水厂出厂水', 'FACTORY', 'BLANK', 'pH,浊度,余氯', 720002, '原水重点五项', '[{"parameterId":700001,"parameterName":"pH","unit":"","standardMin":6.50,"standardMax":8.50,"referenceStandard":"GB 5749-2022","methodId":710001,"methodName":"玻璃电极法","methodBasis":"校准 pH 计后直接测定。"},{"parameterId":700002,"parameterName":"浊度","unit":"NTU","standardMin":0.00,"standardMax":1.00,"referenceStandard":"GB 5749-2022","methodId":710002,"methodName":"散射光浊度法","methodBasis":"使用浊度仪读取 NTU 值。"},{"parameterId":700003,"parameterName":"余氯","unit":"mg/L","standardMin":0.05,"standardMax":2.00,"referenceStandard":"GB 5749-2022","methodId":710003,"methodName":"DPD分光光度法","methodBasis":"加入 DPD 试剂显色后测定。"}]', @reviewFlowId, @reviewFlowName, @publishFlowId, @publishFlowName, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), 1102, '李采样', '晴', '4℃冷藏', 'LOGGED', '待检测', '演示：备用样品。', '样品登录：封签号=SEAL-202605-0006，样品编号=2026050006，点位=城西水厂出厂水，采样人=李采样，采样时间=' , 0, 1102, '李采样', NOW() - INTERVAL 6 DAY, 1102, '李采样', NOW() - INTERVAL 5 DAY);

-- 5. 检测主流程
INSERT INTO lab_detection_record (
    id, sample_id, sample_no, seal_no, detection_type_id, detection_type_name, detection_time,
    detector_id, detector_name, detection_result, abnormal_remark, detection_status, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(980001, 970001, '2026050001', 'SEAL-202605-0001', 720001, '出厂水常规九项', NULL, NULL, NULL, NULL, NULL, 'WAIT_ASSIGN', 0, 1101, '陈采样', NOW() - INTERVAL 4 DAY, 1101, '陈采样', NOW() - INTERVAL 4 DAY),
(980002, 970002, '2026050002', 'SEAL-202605-0002', 720003, '管网末梢四项', DATE_SUB(NOW(), INTERVAL 2 DAY), 1201, '王检测', NULL, NULL, 'WAIT_DETECT', 0, 1102, '李采样', NOW() - INTERVAL 2 DAY, 1201, '王检测', NOW() - INTERVAL 2 DAY),
(980003, 970003, '2026050003', 'SEAL-202605-0003', 720004, '应急复检套牌', DATE_SUB(NOW(), INTERVAL 8 DAY), 1202, '赵检测', 'ABNORMAL', '浊度和氨氮超出控制要求，待复核。', 'SUBMITTED', 0, 1102, '李采样', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY),
(980004, 970004, '2026050004', 'SEAL-202605-0004', 720002, '原水重点五项', DATE_SUB(NOW(), INTERVAL 5 DAY), 1203, '周检测', 'NORMAL', '全部指标正常。', 'APPROVED', 0, 1101, '陈采样', NOW() - INTERVAL 5 DAY, 1203, '周检测', NOW() - INTERVAL 4 DAY),
(980005, 970005, '2026050005', 'SEAL-202605-0005', 720001, '出厂水常规九项', NULL, NULL, NULL, NULL, NULL, 'WAIT_DETECT', 0, 1101, '陈采样', NOW() - INTERVAL 1 DAY, 1101, '陈采样', NOW() - INTERVAL 1 DAY),
(980006, 970006, '2026050006', 'SEAL-202605-0006', 720002, '原水重点五项', DATE_SUB(NOW(), INTERVAL 6 DAY), 1202, '赵检测', 'ABNORMAL', '原水氨氮偏高，待复审。', 'REJECTED', 0, 1102, '李采样', NOW() - INTERVAL 6 DAY, 1202, '赵检测', NOW() - INTERVAL 5 DAY);

-- 6. 检测子流程
INSERT INTO lab_detection_item (
    id, record_id, parameter_id, parameter_name, standard_min, standard_max, result_value,
    unit, reference_standard, method_id, method_name, detector_id, detector_name,
    item_status, exceed_flag, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(990001, 980001, 700001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', 710001, '玻璃电极法', NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1101, '陈采样', NOW() - INTERVAL 4 DAY, 1101, '陈采样', NOW() - INTERVAL 4 DAY),
(990002, 980001, 700002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', NULL, NULL, 'WAIT_ASSIGN', 0, 0, 1101, '陈采样', NOW() - INTERVAL 4 DAY, 1101, '陈采样', NOW() - INTERVAL 4 DAY),
(990003, 980002, 700003, '余氯', 0.05, 2.00, NULL, 'mg/L', 'GB 5749-2022', 710003, 'DPD分光光度法', 1201, '王检测', 'WAIT_DETECT', 0, 0, 1201, '王检测', NOW() - INTERVAL 2 DAY, 1201, '王检测', NOW() - INTERVAL 2 DAY),
(990004, 980002, 700004, '肉眼可见物', 0.00, 0.00, NULL, '项', 'GB 5749-2022', 710007, '目视观察法', 1201, '王检测', 'WAIT_DETECT', 0, 0, 1201, '王检测', NOW() - INTERVAL 2 DAY, 1201, '王检测', NOW() - INTERVAL 2 DAY),
(990005, 980003, 700002, '浊度', 0.00, 1.00, 1.38, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1202, '赵检测', 'SUBMITTED', 1, 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY),
(990006, 980003, 700004, '氨氮', 0.00, 0.50, 0.72, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1202, '赵检测', 'SUBMITTED', 1, 0, 1202, '赵检测', NOW() - INTERVAL 8 DAY, 1202, '赵检测', NOW() - INTERVAL 8 DAY),
(990007, 980004, 700001, 'pH', 6.50, 8.50, 7.10, '', 'GB 5749-2022', 710001, '玻璃电极法', 1203, '周检测', 'APPROVED', 0, 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1203, '周检测', NOW() - INTERVAL 4 DAY),
(990008, 980004, 700003, '余氯', 0.05, 2.00, 0.46, 'mg/L', 'GB 5749-2022', 710003, 'DPD分光光度法', 1203, '周检测', 'APPROVED', 0, 0, 1203, '周检测', NOW() - INTERVAL 5 DAY, 1203, '周检测', NOW() - INTERVAL 4 DAY),
(990009, 980005, 700001, 'pH', 6.50, 8.50, NULL, '', 'GB 5749-2022', 710001, '玻璃电极法', 1201, '王检测', 'WAIT_DETECT', 0, 0, 1201, '王检测', NOW() - INTERVAL 1 DAY, 1201, '王检测', NOW() - INTERVAL 1 DAY),
(990010, 980005, 700002, '浊度', 0.00, 1.00, NULL, 'NTU', 'GB 5749-2022', 710002, '散射光浊度法', 1201, '王检测', 'WAIT_DETECT', 0, 0, 1201, '王检测', NOW() - INTERVAL 1 DAY, 1201, '王检测', NOW() - INTERVAL 1 DAY),
(990011, 980006, 700001, 'pH', 6.50, 8.50, 7.80, '', 'GB 5749-2022', 710001, '玻璃电极法', 1202, '赵检测', 'REJECTED', 0, 0, 1202, '赵检测', NOW() - INTERVAL 6 DAY, 1202, '赵检测', NOW() - INTERVAL 5 DAY),
(990012, 980006, 700004, '氨氮', 0.00, 0.50, 0.92, 'mg/L', 'GB 5749-2022', 710004, '纳氏试剂分光光度法', 1202, '赵检测', 'REJECTED', 1, 0, 1202, '赵检测', NOW() - INTERVAL 6 DAY, 1202, '赵检测', NOW() - INTERVAL 5 DAY);

-- 7. 审核记录
INSERT INTO lab_review_record (
    id, detection_record_id, sample_id, sample_no, seal_no, flow_id, flow_node_id, flow_node_name,
    flow_node_order, required_flag, reviewer_id, reviewer_name, review_time, review_result,
    reject_reason, review_remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(800001, 980003, 970003, '2026050003', 'SEAL-202605-0003', @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1301, '刘审核', DATE_SUB(NOW(), INTERVAL 7 DAY), 'APPROVED', NULL, '第一节点审核通过。', 0, 1301, '刘审核', NOW() - INTERVAL 7 DAY, 1301, '刘审核', NOW() - INTERVAL 7 DAY),
(800002, 980006, 970006, '2026050006', 'SEAL-202605-0006', @reviewFlowId, @reviewNodeId, @reviewNodeName, @reviewNodeOrder, @reviewRequiredFlag, 1302, '何审核', DATE_SUB(NOW(), INTERVAL 4 DAY), 'REJECTED', '氨氮超限，建议重检。', '已要求重检。', 0, 1302, '何审核', NOW() - INTERVAL 4 DAY, 1302, '何审核', NOW() - INTERVAL 4 DAY);

-- 8. 报告
INSERT INTO lab_report (
    id, report_name, report_type, generated_time, sample_id, sample_no, seal_no, detection_record_id,
    report_status, published_time, published_by, published_by_name, file_path, content_snapshot,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(810001, '2026050004-检测报告草稿', 'DAILY', DATE_SUB(NOW(), INTERVAL 5 DAY), 970004, '2026050004', 'SEAL-202605-0004', 980004, 'DRAFT', NULL, NULL, NULL, NULL, '样品编号：2026050004；封签编号：SEAL-202605-0004；点位：富河原水取水口；检测结果：正常；说明：草稿状态演示。', 0, 1301, '刘审核', NOW() - INTERVAL 5 DAY, 1301, '刘审核', NOW() - INTERVAL 5 DAY),
(810002, '2026050003-检测报告已生成', 'DAILY', DATE_SUB(NOW(), INTERVAL 4 DAY), 970003, '2026050003', 'SEAL-202605-0003', 980003, 'GENERATED', NULL, NULL, NULL, '/home/resource/report/2026050003.pdf', '样品编号：2026050003；封签编号：SEAL-202605-0003；点位：应急加密监测点；检测结果：异常；说明：已生成未发布。', 0, 1301, '刘审核', NOW() - INTERVAL 4 DAY, 1301, '刘审核', NOW() - INTERVAL 4 DAY),
(810003, '2026050004-检测报告正式版', 'DAILY', DATE_SUB(NOW(), INTERVAL 3 DAY), 970004, '2026050004', 'SEAL-202605-0004', 980004, 'PUBLISHED', DATE_SUB(NOW(), INTERVAL 2 DAY), 1401, '孙报告', '/home/resource/report/2026050004.pdf', '样品编号：2026050004；封签编号：SEAL-202605-0004；点位：富河原水取水口；检测结果：正常；说明：正式发布演示。', 0, 1301, '刘审核', NOW() - INTERVAL 3 DAY, 1401, '孙报告', NOW() - INTERVAL 2 DAY);

-- 9. 报告推送
INSERT INTO lab_report_push_record (
    id, report_id, sample_id, sample_no, seal_no, recipient_user_id, recipient_name, recipient_phone,
    push_channel, push_status, push_message, push_time, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(820001, 810003, 970004, '2026050004', 'SEAL-202605-0004', 1401, '孙报告', '13810004001', 'UNIFIED', 'SUCCESS', '报告已成功推送。', DATE_SUB(NOW(), INTERVAL 2 DAY), 0, 1401, '孙报告', NOW() - INTERVAL 2 DAY, 1401, '孙报告', NOW() - INTERVAL 2 DAY),
(820002, 810002, 970003, '2026050003', 'SEAL-202605-0003', 1401, '孙报告', '13810004001', 'UNIFIED', 'PENDING', '等待推送。', NULL, 0, 1401, '孙报告', NOW() - INTERVAL 4 DAY, 1401, '孙报告', NOW() - INTERVAL 4 DAY),
(820003, 810001, 970004, '2026050004', 'SEAL-202605-0004', 1401, '孙报告', '13810004001', 'UNIFIED', 'CANCELLED', '推送已撤回。', DATE_SUB(NOW(), INTERVAL 3 DAY), 0, 1401, '孙报告', NOW() - INTERVAL 3 DAY, 1401, '孙报告', NOW() - INTERVAL 3 DAY),
(820004, 810003, 970004, '2026050004', 'SEAL-202605-0004', 1401, '孙报告', '13810004001', 'UNIFIED', 'FAILED', '推送失败，稍后重试。', DATE_SUB(NOW(), INTERVAL 2 DAY), 0, 1401, '孙报告', NOW() - INTERVAL 2 DAY, 1401, '孙报告', NOW() - INTERVAL 2 DAY);

-- 10. 设备台账与维修
INSERT INTO lab_instrument (
    id, instrument_name, instrument_model, manufacturer, purchase_date, service_life_years,
    calibration_cycle, owner_name, instrument_status, storage_location, certificate_url, remark,
    deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(830001, '紫外可见分光光度计', 'UV-2600i', '岛津', DATE_SUB(CURDATE(), INTERVAL 900 DAY), 8, '12个月', '王检测', 'NORMAL', '理化室A-01', NULL, '用于氨氮、余氯等比色检测。', 0, 1001, '系统管理员', NOW() - INTERVAL 60 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(830002, '浊度仪', '2100Q', 'HACH', DATE_SUB(CURDATE(), INTERVAL 600 DAY), 6, '6个月', '赵检测', 'DISABLED', '理化室A-02', NULL, '设备停用演示。', 0, 1001, '系统管理员', NOW() - INTERVAL 55 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(830003, 'pH计', 'SevenCompact', '梅特勒', DATE_SUB(CURDATE(), INTERVAL 500 DAY), 5, '6个月', '周检测', 'MAINTENANCE', '理化室A-03', NULL, '设备维修演示。', 0, 1001, '系统管理员', NOW() - INTERVAL 50 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(830004, '原子吸收分光光度计', 'AA-7000', '岛津', DATE_SUB(CURDATE(), INTERVAL 1200 DAY), 10, '12个月', '王检测', 'CALIBRATING', '金属室B-01', NULL, '校准中演示。', 0, 1001, '系统管理员', NOW() - INTERVAL 48 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY);

INSERT INTO lab_instrument_maintenance (
    id, instrument_id, instrument_name, maintenance_time, maintenance_reason, maintainer_name,
    maintenance_company, maintenance_result, maintenance_cost, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(840001, 830003, 'pH计', DATE_SUB(NOW(), INTERVAL 7 DAY), '电极老化，响应变慢。', '张工', '武汉精仪维保有限公司', '已更换电极，等待复测。', 2800.00, '维修记录演示。', 0, 1001, '系统管理员', NOW() - INTERVAL 7 DAY, 1001, '系统管理员', NOW() - INTERVAL 7 DAY),
(840002, 830004, '原子吸收分光光度计', DATE_SUB(NOW(), INTERVAL 2 DAY), '灯源能量波动。', '周工', '内部校准', '校准中，待复检。', 0.00, '校准记录演示。', 0, 1203, '周检测', NOW() - INTERVAL 2 DAY, 1203, '周检测', NOW() - INTERVAL 2 DAY);

-- 11. 文档与共享
INSERT INTO lab_document (
    id, document_name, document_category, file_type, file_size, file_url, remark, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(850001, '生活饮用水检测作业指导书', '操作规程', 'pdf', 286720, '/home/resource/docs/drinking-water-sop.pdf', '演示文档。', 0, 1001, '系统管理员', NOW() - INTERVAL 20 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(850002, '采样封签管理制度', '管理制度', 'docx', 143360, '/home/resource/docs/seal-management.docx', '演示文档。', 0, 1001, '系统管理员', NOW() - INTERVAL 18 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY),
(850003, '仪器期间核查模板', '表单模板', 'xlsx', 102400, '/home/resource/docs/instrument-check-template.xlsx', '演示文档。', 0, 1001, '系统管理员', NOW() - INTERVAL 12 DAY, 1001, '系统管理员', NOW() - INTERVAL 1 DAY);

INSERT INTO lab_document_share (
    id, document_id, user_id, username, real_name, deleted,
    created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(860001, 850001, 1201, 'detector01', '王检测', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860002, 850001, 1202, 'detector02', '赵检测', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860003, 850002, 1101, 'sampler01', '陈采样', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW()),
(860004, 850003, 1102, 'sampler02', '李采样', 0, 1001, '系统管理员', NOW(), 1001, '系统管理员', NOW());

-- 12. 登录日志
INSERT INTO lab_login_log (
    id, user_id, username, real_name, role_code, login_channel, login_status, login_time,
    remark, deleted, created_by, created_name, created_time, updated_by, updated_name, updated_time
)
VALUES
(870001, 1101, 'sampler01', '陈采样', 'SAMPLER', 'PC', 'SUCCESS', NOW() - INTERVAL 5 DAY, '采样员登录成功。', 0, 1101, '陈采样', NOW() - INTERVAL 5 DAY, 1101, '陈采样', NOW() - INTERVAL 5 DAY),
(870002, 1201, 'detector01', '王检测', 'DETECTOR', 'MOBILE', 'SUCCESS', NOW() - INTERVAL 4 DAY, '移动端登录成功。', 0, 1201, '王检测', NOW() - INTERVAL 4 DAY, 1201, '王检测', NOW() - INTERVAL 4 DAY),
(870003, 1301, 'reviewer01', '刘审核', 'REVIEWER', 'PC', 'SUCCESS', NOW() - INTERVAL 3 DAY, '审核员登录成功。', 0, 1301, '刘审核', NOW() - INTERVAL 3 DAY, 1301, '刘审核', NOW() - INTERVAL 3 DAY),
(870004, 1401, 'reporter01', '孙报告', 'REPORTER', 'PC', 'SUCCESS', NOW() - INTERVAL 2 DAY, '报告员登录成功。', 0, 1401, '孙报告', NOW() - INTERVAL 2 DAY, 1401, '孙报告', NOW() - INTERVAL 2 DAY),
(870005, 1102, 'sampler02', '李采样', 'SAMPLER', 'PC', 'FAILED', NOW() - INTERVAL 1 DAY, '验证码错误。', 0, 1102, '李采样', NOW() - INTERVAL 1 DAY, 1102, '李采样', NOW() - INTERVAL 1 DAY),
(870006, 1202, 'detector02', '赵检测', 'DETECTOR', 'MOBILE', 'SUCCESS', NOW(), '移动端登录成功。', 0, 1202, '赵检测', NOW(), 1202, '赵检测', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- 简单核对一下演示数据是否落库。
SELECT 'lab_monitoring_point' AS table_name, COUNT(*) AS total FROM lab_monitoring_point WHERE id BETWEEN 940001 AND 940020
UNION ALL SELECT 'lab_sampling_plan', COUNT(*) FROM lab_sampling_plan WHERE id BETWEEN 950001 AND 950020
UNION ALL SELECT 'lab_sampling_task', COUNT(*) FROM lab_sampling_task WHERE id BETWEEN 960001 AND 960020
UNION ALL SELECT 'lab_sample', COUNT(*) FROM lab_sample WHERE id BETWEEN 970001 AND 970020
UNION ALL SELECT 'lab_detection_record', COUNT(*) FROM lab_detection_record WHERE id BETWEEN 980001 AND 980020
UNION ALL SELECT 'lab_detection_item', COUNT(*) FROM lab_detection_item WHERE id BETWEEN 990001 AND 990100
UNION ALL SELECT 'lab_review_record', COUNT(*) FROM lab_review_record WHERE id BETWEEN 800001 AND 800020
UNION ALL SELECT 'lab_report', COUNT(*) FROM lab_report WHERE id BETWEEN 810001 AND 810020
UNION ALL SELECT 'lab_report_push_record', COUNT(*) FROM lab_report_push_record WHERE id BETWEEN 820001 AND 820020
UNION ALL SELECT 'lab_instrument', COUNT(*) FROM lab_instrument WHERE id BETWEEN 830001 AND 830020
UNION ALL SELECT 'lab_instrument_maintenance', COUNT(*) FROM lab_instrument_maintenance WHERE id BETWEEN 840001 AND 840020
UNION ALL SELECT 'lab_document', COUNT(*) FROM lab_document WHERE id BETWEEN 850001 AND 850020
UNION ALL SELECT 'lab_document_share', COUNT(*) FROM lab_document_share WHERE id BETWEEN 860001 AND 860020
UNION ALL SELECT 'lab_login_log', COUNT(*) FROM lab_login_log WHERE id BETWEEN 870001 AND 870020;
