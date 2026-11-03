-- ============================================================
-- 校园体育赛事管理系统 - 数据库建表脚本
-- Database: MySQL 8.0
-- Author: Winston (Architect)
-- Version: 1.0.0
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `sports_events` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `sports_events`;

-- ============================================================
-- 1. 系统管理模块
-- ============================================================

-- 1.1 用户表 (sys_user)
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `gender` TINYINT DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: ADMIN-管理员 TEACHER-教师 STUDENT-学生',
    `student_no` VARCHAR(50) DEFAULT NULL COMMENT '学号(学生角色)',
    `department` VARCHAR(100) DEFAULT NULL COMMENT '院系',
    `class_name` VARCHAR(50) DEFAULT NULL COMMENT '班级',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_student_no` (`student_no`),
    KEY `idx_role` (`role`),
    KEY `idx_department` (`department`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 1.2 字典类型表 (sys_dict_type)
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
    `dict_code` VARCHAR(50) NOT NULL COMMENT '字典编码',
    `dict_name` VARCHAR(100) NOT NULL COMMENT '字典名称',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典类型表';

-- 1.3 字典数据表 (sys_dict_data)
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
    `dict_code` VARCHAR(50) NOT NULL COMMENT '字典编码',
    `item_value` VARCHAR(50) NOT NULL COMMENT '字典项值',
    `item_label` VARCHAR(100) NOT NULL COMMENT '字典项标签',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_item` (`dict_code`, `item_value`),
    KEY `idx_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字典数据表';

-- ============================================================
-- 2. 赛事管理模块
-- ============================================================

-- 2.1 赛事表 (event)
DROP TABLE IF EXISTS `event`;
CREATE TABLE `event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '赛事ID',
    `event_name` VARCHAR(100) NOT NULL COMMENT '赛事名称',
    `event_type` VARCHAR(50) NOT NULL COMMENT '赛事类型(字典: event_type)',
    `description` TEXT DEFAULT NULL COMMENT '赛事描述',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE NOT NULL COMMENT '结束日期',
    `reg_start_date` DATE DEFAULT NULL COMMENT '报名开始日期',
    `reg_end_date` DATE DEFAULT NULL COMMENT '报名截止日期',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '比赛地点',
    `organizer` VARCHAR(100) DEFAULT NULL COMMENT '主办方',
    `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-草稿 2-报名中 3-进行中 4-已结束 5-已取消',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_event_type` (`event_type`),
    KEY `idx_status` (`status`),
    KEY `idx_start_date` (`start_date`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛事表';

-- 2.2 比赛项目表 (sport_item)
DROP TABLE IF EXISTS `sport_item`;
CREATE TABLE `sport_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `event_id` BIGINT NOT NULL COMMENT '赛事ID',
    `item_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
    `item_type` VARCHAR(50) DEFAULT NULL COMMENT '项目类型(字典: item_type)',
    `gender_limit` TINYINT DEFAULT 0 COMMENT '性别限制: 0-不限 1-仅男 2-仅女',
    `max_participants` INT DEFAULT 0 COMMENT '最大参赛人数(0表示不限)',
    `current_participants` INT NOT NULL DEFAULT 0 COMMENT '已通过审核的报名人数',
    `result_type` VARCHAR(20) DEFAULT 'SCORE' COMMENT '成绩类型: TIME-时间 DISTANCE-距离 SCORE-分数',
    `result_unit` VARCHAR(20) DEFAULT NULL COMMENT '成绩单位(秒/米/分)',
    `sort_rule` VARCHAR(10) DEFAULT 'DESC' COMMENT '排序规则: ASC-升序(时间) DESC-降序(分数/距离)',
    `description` TEXT DEFAULT NULL COMMENT '项目描述',
    `rules` TEXT DEFAULT NULL COMMENT '比赛规则',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_item_type` (`item_type`),
    KEY `idx_gender_limit` (`gender_limit`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛项目表';

-- ============================================================
-- 3. 报名管理模块
-- ============================================================

-- 3.1 报名表 (registration)
DROP TABLE IF EXISTS `registration`;
CREATE TABLE `registration` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报名ID',
    `event_id` BIGINT NOT NULL COMMENT '赛事ID',
    `sport_item_id` BIGINT NOT NULL COMMENT '项目ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID(报名人)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-待审核 2-已通过 3-已拒绝 4-已取消',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因',
    `audit_by` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_item` (`user_id`, `sport_item_id`, `is_deleted`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_sport_item_id` (`sport_item_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报名表';

-- ============================================================
-- 4. 赛程管理模块
-- ============================================================

-- 4.1 赛程表 (schedule)
DROP TABLE IF EXISTS `schedule`;
CREATE TABLE `schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '赛程ID',
    `event_id` BIGINT NOT NULL COMMENT '赛事ID',
    `sport_item_id` BIGINT NOT NULL COMMENT '项目ID',
    `schedule_name` VARCHAR(100) DEFAULT NULL COMMENT '赛程名称(如: 预赛/决赛)',
    `round_no` INT DEFAULT 1 COMMENT '轮次',
    `group_no` VARCHAR(20) DEFAULT NULL COMMENT '组别(如: A组/B组)',
    `match_date` DATE NOT NULL COMMENT '比赛日期',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME DEFAULT NULL COMMENT '结束时间',
    `location` VARCHAR(200) DEFAULT NULL COMMENT '比赛场地',
    `referee` VARCHAR(100) DEFAULT NULL COMMENT '裁判',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-未开始 2-进行中 3-已结束',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_sport_item_id` (`sport_item_id`),
    KEY `idx_match_date` (`match_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赛程表';

-- ============================================================
-- 5. 成绩管理模块
-- ============================================================

-- 5.1 成绩表 (result)
DROP TABLE IF EXISTS `result`;
CREATE TABLE `result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '成绩ID',
    `event_id` BIGINT NOT NULL COMMENT '赛事ID',
    `sport_item_id` BIGINT NOT NULL COMMENT '项目ID',
    `schedule_id` BIGINT DEFAULT NULL COMMENT '赛程ID',
    `registration_id` BIGINT NOT NULL COMMENT '报名ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID(参赛者)',
    `score` DECIMAL(10,2) DEFAULT NULL COMMENT '成绩值',
    `score_text` VARCHAR(50) DEFAULT NULL COMMENT '成绩文本(如: 10.25秒)',
    `ranking` INT DEFAULT NULL COMMENT '名次',
    `points` INT DEFAULT 0 COMMENT '积分',
    `record_by` BIGINT DEFAULT NULL COMMENT '录入人ID',
    `record_time` DATETIME DEFAULT NULL COMMENT '录入时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_registration` (`registration_id`, `is_deleted`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_sport_item_id` (`sport_item_id`),
    KEY `idx_schedule_id` (`schedule_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_ranking` (`ranking`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩表';

-- ============================================================
-- 6. 公告管理模块
-- ============================================================

-- 6.1 公告表 (news)
DROP TABLE IF EXISTS `news`;
CREATE TABLE `news` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `news_type` VARCHAR(50) DEFAULT NULL COMMENT '公告类型(字典: news_type)',
    `content` TEXT DEFAULT NULL COMMENT '内容',
    `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
    `event_id` BIGINT DEFAULT NULL COMMENT '关联赛事ID',
    `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶: 0-否 1-是',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿 1-已发布 2-已下架',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_news_type` (`news_type`),
    KEY `idx_event_id` (`event_id`),
    KEY `idx_is_top` (`is_top`),
    KEY `idx_status` (`status`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- ============================================================
-- 7. 初始化数据
-- ============================================================

-- 7.1 初始化管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `role`, `status`) VALUES
('admin', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '系统管理员', 'ADMIN', 1);

-- 7.2 初始化字典类型
INSERT INTO `sys_dict_type` (`dict_code`, `dict_name`, `remark`) VALUES
('event_type', '赛事类型', '赛事类型字典'),
('event_status', '赛事状态', '赛事状态字典'),
('item_type', '项目类型', '比赛项目类型字典'),
('registration_status', '报名状态', '报名状态字典'),
('news_type', '公告类型', '公告类型字典'),
('gender', '性别', '性别字典'),
('result_type', '成绩类型', '成绩类型字典');

-- 7.3 初始化字典数据
-- 赛事类型
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`) VALUES
('event_type', 'TRACK_FIELD', '田径运动会', 1),
('event_type', 'BALL', '球类联赛', 2),
('event_type', 'SWIMMING', '游泳比赛', 3),
('event_type', 'OTHER', '其他', 99);

-- 赛事状态
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`) VALUES
('event_status', '1', '草稿', 1),
('event_status', '2', '报名中', 2),
('event_status', '3', '进行中', 3),
('event_status', '4', '已结束', 4),
('event_status', '5', '已取消', 5);

-- 项目类型
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`) VALUES
('item_type', 'TRACK', '径赛(跑步)', 1),
('item_type', 'FIELD', '田赛(跳跃/投掷)', 2),
('item_type', 'BALL', '球类', 3),
('item_type', 'WATER', '水上项目', 4),
('item_type', 'OTHER', '其他', 99);

-- 报名状态
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`) VALUES
('registration_status', '1', '待审核', 1),
('registration_status', '2', '已通过', 2),
('registration_status', '3', '已拒绝', 3),
('registration_status', '4', '已取消', 4);

-- 公告类型
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`) VALUES
('news_type', 'NOTICE', '赛事通知', 1),
('news_type', 'RESULT', '成绩公告', 2),
('news_type', 'RULE', '规则说明', 3),
('news_type', 'OTHER', '其他', 99);

-- 性别
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`) VALUES
('gender', '0', '未知', 0),
('gender', '1', '男', 1),
('gender', '2', '女', 2);

-- 成绩类型
INSERT INTO `sys_dict_data` (`dict_code`, `item_value`, `item_label`, `sort_order`) VALUES
('result_type', 'TIME', '时间', 1),
('result_type', 'DISTANCE', '距离', 2),
('result_type', 'SCORE', '分数', 3);

-- ============================================================
-- 8. 测试数据 (可选)
-- ============================================================

-- 8.1 测试用户
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `gender`, `role`, `student_no`, `department`, `class_name`, `status`) VALUES
('teacher01', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '张老师', 1, 'TEACHER', NULL, '体育部', NULL, 1),
('student01', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '李明', 1, 'STUDENT', '2021001001', '计算机学院', '计算机2101班', 1),
('student02', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '王芳', 2, 'STUDENT', '2021001002', '计算机学院', '计算机2101班', 1),
('student03', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '张伟', 1, 'STUDENT', '2021002001', '电子学院', '电子2101班', 1);

-- 8.2 测试赛事
INSERT INTO `event` (`event_name`, `event_type`, `description`, `start_date`, `end_date`, `reg_start_date`, `reg_end_date`, `location`, `organizer`, `contact_person`, `status`, `create_by`) VALUES
('2024年春季运动会', 'TRACK_FIELD', '一年一度的春季田径运动会，欢迎全校师生参加！', '2024-04-15', '2024-04-17', '2024-03-01', '2024-04-10', '校田径场', '体育部', '张老师', 2, 1);

-- 8.3 测试项目
INSERT INTO `sport_item` (`event_id`, `item_name`, `item_type`, `gender_limit`, `max_participants`, `result_type`, `result_unit`, `sort_rule`, `sort_order`) VALUES
(1, '男子100米', 'TRACK', 1, 50, 'TIME', '秒', 'ASC', 1),
(1, '女子100米', 'TRACK', 2, 50, 'TIME', '秒', 'ASC', 2),
(1, '男子跳远', 'FIELD', 1, 30, 'DISTANCE', '米', 'DESC', 3),
(1, '女子跳远', 'FIELD', 2, 30, 'DISTANCE', '米', 'DESC', 4),
(1, '男子铅球', 'FIELD', 1, 30, 'DISTANCE', '米', 'DESC', 5);

-- ============================================================
-- END OF SCRIPT
-- ============================================================