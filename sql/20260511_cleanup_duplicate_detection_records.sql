-- 作用：
-- 清理同一样品下重复生成的“活跃检测主流程”。
-- 仅处理 detection_status 为 WAIT_ASSIGN / WAIT_DETECT / SUBMITTED 且 deleted = 0 的记录。
-- 保留规则：
-- 1. 已提交子流程数量更多的优先保留
-- 2. 已分配检测员子流程数量更多的优先保留
-- 3. 参数子流程数量更多的优先保留
-- 4. 更新时间、创建时间、主键更晚的优先保留
--
-- 执行前建议先完整备份：
-- mysqldump -h 192.168.7.17 -uroot -p yx_lab lab_detection_record lab_detection_item > detection_backup.sql

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_detection_record_duplicate_cleanup;
CREATE TEMPORARY TABLE tmp_detection_record_duplicate_cleanup (
    record_id BIGINT PRIMARY KEY
);

DROP TEMPORARY TABLE IF EXISTS tmp_detection_record_stats;
CREATE TEMPORARY TABLE tmp_detection_record_stats (
    record_id BIGINT PRIMARY KEY,
    sample_id BIGINT NOT NULL,
    submitted_count INT NOT NULL DEFAULT 0,
    assigned_count INT NOT NULL DEFAULT 0,
    item_count INT NOT NULL DEFAULT 0,
    updated_time DATETIME NULL,
    created_time DATETIME NULL
);

DROP TEMPORARY TABLE IF EXISTS tmp_detection_record_stats_ref;
CREATE TEMPORARY TABLE tmp_detection_record_stats_ref (
    record_id BIGINT PRIMARY KEY,
    sample_id BIGINT NOT NULL,
    submitted_count INT NOT NULL DEFAULT 0,
    assigned_count INT NOT NULL DEFAULT 0,
    item_count INT NOT NULL DEFAULT 0,
    updated_time DATETIME NULL,
    created_time DATETIME NULL
);

INSERT INTO tmp_detection_record_stats (
    record_id,
    sample_id,
    submitted_count,
    assigned_count,
    item_count,
    updated_time,
    created_time
)
SELECT
    r.id AS record_id,
    r.sample_id,
    COALESCE(SUM(CASE WHEN i.item_status = 'SUBMITTED' THEN 1 ELSE 0 END), 0) AS submitted_count,
    COALESCE(SUM(CASE WHEN i.detector_id IS NOT NULL THEN 1 ELSE 0 END), 0) AS assigned_count,
    COUNT(i.id) AS item_count,
    r.updated_time,
    r.created_time
FROM lab_detection_record r
LEFT JOIN lab_detection_item i
    ON i.record_id = r.id
   AND i.deleted = 0
WHERE r.deleted = 0
  AND r.detection_status IN ('WAIT_ASSIGN', 'WAIT_DETECT', 'SUBMITTED')
GROUP BY
    r.id,
    r.sample_id,
    r.updated_time,
    r.created_time;

INSERT INTO tmp_detection_record_stats_ref
SELECT *
FROM tmp_detection_record_stats;

INSERT IGNORE INTO tmp_detection_record_duplicate_cleanup (record_id)
SELECT s1.record_id
FROM tmp_detection_record_stats s1
WHERE EXISTS (
    SELECT 1
    FROM tmp_detection_record_stats_ref s2
    WHERE s2.sample_id = s1.sample_id
      AND (
            s2.submitted_count > s1.submitted_count
         OR (s2.submitted_count = s1.submitted_count AND s2.assigned_count > s1.assigned_count)
         OR (s2.submitted_count = s1.submitted_count AND s2.assigned_count = s1.assigned_count AND s2.item_count > s1.item_count)
         OR (s2.submitted_count = s1.submitted_count AND s2.assigned_count = s1.assigned_count AND s2.item_count = s1.item_count AND s2.updated_time > s1.updated_time)
         OR (s2.submitted_count = s1.submitted_count AND s2.assigned_count = s1.assigned_count AND s2.item_count = s1.item_count AND s2.updated_time = s1.updated_time AND s2.created_time > s1.created_time)
         OR (s2.submitted_count = s1.submitted_count AND s2.assigned_count = s1.assigned_count AND s2.item_count = s1.item_count AND s2.updated_time = s1.updated_time AND s2.created_time = s1.created_time AND s2.record_id > s1.record_id)
      )
);

-- 预览本次将被清理的重复主流程
SELECT
    r.id,
    r.sample_id,
    r.sample_no,
    r.seal_no,
    r.detection_type_name,
    r.detection_status,
    r.created_time,
    r.updated_time
FROM lab_detection_record r
INNER JOIN tmp_detection_record_duplicate_cleanup t
    ON t.record_id = r.id
ORDER BY r.sample_id, r.created_time, r.id;

-- 先逻辑删除子流程
UPDATE lab_detection_item
SET deleted = 1,
    updated_time = NOW()
WHERE deleted = 0
  AND record_id IN (
      SELECT record_id
      FROM tmp_detection_record_duplicate_cleanup
  );

-- 再逻辑删除重复主流程
UPDATE lab_detection_record
SET deleted = 1,
    updated_time = NOW()
WHERE deleted = 0
  AND id IN (
      SELECT record_id
      FROM tmp_detection_record_duplicate_cleanup
  );

-- 输出清理结果汇总
SELECT
    COUNT(*) AS cleaned_record_count
FROM tmp_detection_record_duplicate_cleanup;

DROP TEMPORARY TABLE IF EXISTS tmp_detection_record_duplicate_cleanup;
DROP TEMPORARY TABLE IF EXISTS tmp_detection_record_stats;
DROP TEMPORARY TABLE IF EXISTS tmp_detection_record_stats_ref;

COMMIT;
