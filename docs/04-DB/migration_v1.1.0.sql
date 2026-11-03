-- ============================================================
-- 校园体育赛事管理系统 - 增量迁移脚本 v1.1.0
-- 适用：已按旧版 schema.sql / data.sql 初始化过的数据库
-- 说明：全新库直接执行 schema.sql + data.sql 即可，无需本脚本
-- ============================================================

USE `sports_events`;

-- ------------------------------------------------------------
-- 1. sport_item 补充 current_participants（报名人数统计依赖）
-- ------------------------------------------------------------
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'sports_events'
      AND TABLE_NAME = 'sport_item'
      AND COLUMN_NAME = 'current_participants'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `sport_item` ADD COLUMN `current_participants` INT NOT NULL DEFAULT 0 COMMENT ''已通过审核的报名人数'' AFTER `max_participants`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. 回填已报名人数（只统计状态=2 已通过）
-- ------------------------------------------------------------
UPDATE `sport_item` si
SET si.`current_participants` = (
    SELECT COUNT(*) FROM `registration` r
    WHERE r.`sport_item_id` = si.`id` AND r.`status` = 2 AND r.`is_deleted` = 0
);

-- ------------------------------------------------------------
-- 3. 修正历史成绩的 registration_id（按 用户+项目 重新关联）
-- ------------------------------------------------------------
UPDATE `result` res
JOIN `registration` r
  ON r.`user_id` = res.`user_id`
 AND r.`sport_item_id` = res.`sport_item_id`
 AND r.`is_deleted` = 0
SET res.`registration_id` = r.`id`
WHERE res.`is_deleted` = 0;

-- ------------------------------------------------------------
-- 4. 统一公告类型字典与历史数据
-- ------------------------------------------------------------
DELETE FROM `sys_dict_data` WHERE `dict_code` = 'news_type';
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`, `status`) VALUES
('news_type', 'NOTICE', '赛事通知', 1, 1),
('news_type', 'RESULT', '成绩公告', 2, 1),
('news_type', 'RULE', '规则说明', 3, 1),
('news_type', 'OTHER', '其他', 99, 1);

UPDATE `news` SET `news_type` = 'NOTICE' WHERE `news_type` IN ('ANNOUNCEMENT', 'NEWS');
UPDATE `news` SET `news_type` = 'OTHER' WHERE `news_type` IS NULL OR `news_type` = '';

-- ------------------------------------------------------------
-- 5. 赛事状态字典补充「已取消」
-- ------------------------------------------------------------
DELETE FROM `sys_dict_data` WHERE `dict_code` = 'event_status';
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`, `status`) VALUES
('event_status', '1', '草稿', 1, 1),
('event_status', '2', '报名中', 2, 1),
('event_status', '3', '进行中', 3, 1),
('event_status', '4', '已结束', 4, 1),
('event_status', '5', '已取消', 5, 1);

-- ------------------------------------------------------------
-- 6. 【仅演示环境】重置演示账号密码为 123456
--    生产环境请务必删除或注释本段
-- ------------------------------------------------------------
UPDATE `sys_user`
SET `password` = '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm'
WHERE `is_deleted` = 0;

-- ------------------------------------------------------------
-- 7. 校验：查看当前状态分布，便于人工确认
-- ------------------------------------------------------------
SELECT 'event' AS t, `status`, COUNT(*) AS cnt FROM `event` GROUP BY `status`
UNION ALL
SELECT 'registration', `status`, COUNT(*) FROM `registration` GROUP BY `status`
UNION ALL
SELECT 'news', `status`, COUNT(*) FROM `news` GROUP BY `status`;
