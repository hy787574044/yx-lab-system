-- 作用：
-- 为 lab_detection_record 增加“活跃检测主流程唯一约束”。
-- 业务效果：
-- 同一样品在 WAIT_ASSIGN / WAIT_DETECT / SUBMITTED 三种活跃状态下，只允许存在一条检测主流程。
-- APPROVED / REJECTED 等历史记录不受影响，因此不会破坏重检、历史追溯场景。
--
-- 执行前提：
-- 1. 先执行 20260511_cleanup_duplicate_detection_records.sql，清理历史重复活跃记录
-- 2. 再执行本脚本添加数据库约束

ALTER TABLE lab_detection_record
    ADD COLUMN active_sample_guard BIGINT
    GENERATED ALWAYS AS (
        CASE
            WHEN deleted = 0
             AND detection_status IN ('WAIT_ASSIGN', 'WAIT_DETECT', 'SUBMITTED')
            THEN sample_id
            ELSE NULL
        END
    ) STORED COMMENT '活跃检测主流程唯一约束占位字段';

ALTER TABLE lab_detection_record
    ADD UNIQUE INDEX uk_detection_record_active_sample_guard (active_sample_guard);
