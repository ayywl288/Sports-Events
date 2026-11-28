-- ============================================================
-- 校园体育赛事管理系统 - 增量迁移脚本 v1.2.0
-- 适用：已按旧版 schema.sql / data.sql 初始化过的数据库
-- 内容：新增「赛程参赛人员」表，选手与具体场次建立分配关系
-- 说明：全新库直接执行 schema.sql + data.sql 即可，无需本脚本
-- ============================================================

USE `sports_events`;

-- ------------------------------------------------------------
-- 1. 赛程参赛人员表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `schedule_participant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `schedule_id` BIGINT NOT NULL COMMENT '赛程ID',
    `registration_id` BIGINT NOT NULL COMMENT '报名ID',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_registration` (`schedule_id`, `registration_id`, `is_deleted`),
    KEY `idx_schedule_id` (`schedule_id`),
    KEY `idx_registration_id` (`registration_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛程参赛人员表';

-- ------------------------------------------------------------
-- 2. 演示数据：把已通过报名的选手分配到具体场次
--    正式环境可按实际情况在「赛程编排 - 选手安排」中配置，无需执行本段
-- ------------------------------------------------------------
INSERT IGNORE INTO `schedule_participant` (`schedule_id`, `registration_id`)
SELECT s.`id`, r.`id`
FROM `schedule` s
JOIN `registration` r
  ON r.`event_id` = s.`event_id`
 AND r.`sport_item_id` = s.`sport_item_id`
 AND r.`status` = 2
 AND r.`is_deleted` = 0
WHERE s.`event_id` = 1 AND s.`sport_item_id` = 1 AND s.`group_no` = '1组'
  AND r.`user_id` IN (4, 6)
  AND s.`is_deleted` = 0;

INSERT IGNORE INTO `schedule_participant` (`schedule_id`, `registration_id`)
SELECT s.`id`, r.`id`
FROM `schedule` s
JOIN `registration` r
  ON r.`event_id` = s.`event_id`
 AND r.`sport_item_id` = s.`sport_item_id`
 AND r.`status` = 2
 AND r.`is_deleted` = 0
WHERE s.`event_id` = 1 AND s.`sport_item_id` = 1 AND s.`group_no` = '2组'
  AND r.`user_id` = 8
  AND s.`is_deleted` = 0;

INSERT IGNORE INTO `schedule_participant` (`schedule_id`, `registration_id`)
SELECT s.`id`, r.`id`
FROM `schedule` s
JOIN `registration` r
  ON r.`event_id` = s.`event_id`
 AND r.`sport_item_id` = s.`sport_item_id`
 AND r.`status` = 2
 AND r.`is_deleted` = 0
WHERE s.`event_id` = 1 AND s.`sport_item_id` = 2 AND s.`group_no` = '1组'
  AND r.`user_id` = 5
  AND s.`is_deleted` = 0;

INSERT IGNORE INTO `schedule_participant` (`schedule_id`, `registration_id`)
SELECT s.`id`, r.`id`
FROM `schedule` s
JOIN `registration` r
  ON r.`event_id` = s.`event_id`
 AND r.`sport_item_id` = s.`sport_item_id`
 AND r.`status` = 2
 AND r.`is_deleted` = 0
WHERE s.`event_id` = 1 AND s.`sport_item_id` = 3 AND s.`group_no` IS NULL
  AND r.`user_id` IN (4, 8)
  AND s.`is_deleted` = 0;
