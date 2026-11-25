-- 本地演示数据。首次初始化空库时加载。
SET NAMES utf8mb4;
SET @demo_day = CURRENT_DATE();

-- 清空表数据（按外键依赖顺序）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE result;
TRUNCATE TABLE schedule;
TRUNCATE TABLE registration;
TRUNCATE TABLE sport_item;
TRUNCATE TABLE event;
TRUNCATE TABLE news;
TRUNCATE TABLE sys_dict_data;
TRUNCATE TABLE sys_dict_type;
TRUNCATE TABLE sys_user;
SET FOREIGN_KEY_CHECKS = 1;

-- 插入用户数据
-- 密码都是 123456 (BCrypt加密)
INSERT INTO sys_user (username, password, real_name, gender, phone, email, role, student_no, department, class_name, status)
VALUES
    ('admin', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '系统管理员', 1, '13800000001', 'admin@school.edu.cn', 'ADMIN', 'A001', '体育部', NULL, 1),
    ('teacher1', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '张教练', 1, '13800000002', 'teacher1@school.edu.cn', 'TEACHER', 'T001', '体育部', NULL, 1),
    ('teacher2', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '李裁判', 2, '13800000003', 'teacher2@school.edu.cn', 'TEACHER', 'T002', '体育部', NULL, 1),
    ('student1', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '王小明', 1, '13800000011', 'student1@school.edu.cn', 'STUDENT', '2021001', '计算机学院', '软件2101', 1),
    ('student2', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '李小红', 2, '13800000012', 'student2@school.edu.cn', 'STUDENT', '2021002', '计算机学院', '软件2101', 1),
    ('student3', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '张小强', 1, '13800000013', 'student3@school.edu.cn', 'STUDENT', '2021003', '机械学院', '机械2101', 1),
    ('student4', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '刘小芳', 2, '13800000014', 'student4@school.edu.cn', 'STUDENT', '2021004', '经济学院', '金融2101', 1),
    ('student5', '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', '陈小伟', 1, '13800000015', 'student5@school.edu.cn', 'STUDENT', '2021005', '外语学院', '英语2101', 1);

-- 字典类型
INSERT INTO sys_dict_type (dict_code, dict_name, remark, status)
VALUES
    ('event_type','赛事类型','TRACK_FIELD田径,BALL球类,SWIMMING游泳',1),
    ('item_type','项目类型','TRACK径赛,FIELD田赛,BALL球类,SWIMMING游泳',1),
    ('news_type','公告类型','NOTICE赛事通知,RESULT成绩公告,RULE规则说明,OTHER其他',1);

-- 字典数据
INSERT INTO sys_dict_data (dict_code, item_value, item_label, sort_order, status)
VALUES
    ('event_type','TRACK_FIELD','田径赛事',1,1),
    ('event_type','BALL','球类赛事',2,1),
    ('event_type','SWIMMING','游泳赛事',3,1),
    ('item_type','TRACK','径赛',1,1),
    ('item_type','FIELD','田赛',2,1),
    ('item_type','BALL','球类',3,1),
    ('item_type','SWIMMING','游泳',4,1),
    ('news_type','NOTICE','赛事通知',1,1),
    ('news_type','RESULT','成绩公告',2,1),
    ('news_type','RULE','规则说明',3,1),
    ('news_type','OTHER','其他',99,1);

-- 以首次启动日期安排演示赛事，首次进入系统时可以体验报名。
-- status:1草稿 2报名中 3进行中 4已结束
INSERT INTO event (event_name, event_type, description, start_date, end_date, reg_start_date, reg_end_date, location, organizer, contact_phone, status, create_by)
VALUES
    ('校园田径运动会', 'TRACK_FIELD', '设有100米、跳远和铅球项目，按项目公布检录时间和比赛分组。',
        @demo_day + INTERVAL 14 DAY, @demo_day + INTERVAL 16 DAY,
        @demo_day - INTERVAL 1 DAY, @demo_day + INTERVAL 10 DAY, '校田径场', '体育部', '0571-88888888', 2, 1),
    ('校园篮球联赛', 'BALL', '各学院可组队报名，比赛采用小组赛和淘汰赛。',
        @demo_day + INTERVAL 28 DAY, @demo_day + INTERVAL 41 DAY,
        @demo_day - INTERVAL 1 DAY, @demo_day + INTERVAL 21 DAY, '体育馆', '体育部', '0571-88888889', 2, 1),
    ('校园游泳比赛', 'SWIMMING', '拟设男子和女子50米自由泳项目，报名安排待发布。',
        @demo_day + INTERVAL 56 DAY, @demo_day + INTERVAL 58 DAY,
        @demo_day + INTERVAL 35 DAY, @demo_day + INTERVAL 49 DAY, '游泳馆', '体育部', '0571-88888890', 1, 1),
    ('上届校园田径运动会', 'TRACK_FIELD', '已结束的田径赛事，保留男子100米项目的报名和成绩示例。',
        @demo_day - INTERVAL 90 DAY, @demo_day - INTERVAL 88 DAY,
        @demo_day - INTERVAL 118 DAY, @demo_day - INTERVAL 97 DAY, '校田径场', '体育部', '0571-88888888', 4, 1);

-- 插入运动项目数据
-- gender_limit：0不限 1仅男 2仅女
INSERT INTO sport_item (event_id, item_name, item_type, gender_limit, max_participants, result_type, result_unit, sort_rule, rules, status)
VALUES
-- 校园田径运动会项目 event_id=1
(1, '男子100米', 'TRACK', 1, 30, 'TIME', '秒', 'ASC', '预赛取前8名进入决赛', 1),
(1, '女子100米', 'TRACK', 2, 30, 'TIME', '秒', 'ASC', '预赛取前8名进入决赛', 1),
(1, '男子跳远', 'FIELD', 1, 20, 'DISTANCE', '米', 'DESC', '每人3次试跳，取最好成绩', 1),
(1, '女子跳远', 'FIELD', 2, 20, 'DISTANCE', '米', 'DESC', '每人3次试跳，取最好成绩', 1),
(1, '男子铅球', 'FIELD', 1, 15, 'DISTANCE', '米', 'DESC', '每人3次投掷，取最好成绩', 1),
-- 篮球联赛项目 event_id=2
(2, '男子篮球', 'BALL', 1, 120, 'SCORE', '分', 'DESC', '5人制，小组赛+淘汰赛', 1),
(2, '女子篮球', 'BALL', 2, 80, 'SCORE', '分', 'DESC', '5人制，小组赛+淘汰赛', 1),
-- 游泳比赛项目 event_id=3
(3, '男子50米自由泳', 'SWIMMING', 1, 20, 'TIME', '秒', 'ASC', '预赛取前8名进入决赛', 1),
(3, '女子50米自由泳', 'SWIMMING', 2, 20, 'TIME', '秒', 'ASC', '预赛取前8名进入决赛', 1);

-- 插入报名数据
-- status：1待审核 2已通过 3已拒绝 4已取消
INSERT INTO registration (event_id, sport_item_id, user_id, status, remark)
VALUES
    (1, 1, 4, 2, '希望能取得好成绩'),
    (1, 1, 6, 2, NULL),
    (1, 3, 4, 2, NULL),
    (1, 3, 6, 1, '第一次参加'),
    (1, 2, 5, 2, NULL),
    (1, 4, 5, 2, NULL),
    (1, 5, 4, 2, NULL),
    (1, 1, 8, 2, NULL),
    (1, 3, 8, 2, NULL);

-- 插入赛程数据
-- status：1未开始 2进行中 3已结束
INSERT INTO schedule (event_id, sport_item_id, schedule_name, round_no, group_no, match_date, start_time, end_time, location, status, remark)
VALUES
    (1, 1, '男子100米预赛', 1, '1组', @demo_day + INTERVAL 14 DAY, '09:00:00', '09:30:00', '校田径场', 1, '男子100米预赛第1组'),
    (1, 1, '男子100米预赛', 1, '2组', @demo_day + INTERVAL 14 DAY, '09:30:00', '10:00:00', '校田径场', 1, '男子100米预赛第2组'),
    (1, 1, '男子100米决赛', 2, NULL, @demo_day + INTERVAL 15 DAY, '15:00:00', '15:30:00', '校田径场', 1, '男子100米决赛'),
    (1, 2, '女子100米预赛', 1, '1组', @demo_day + INTERVAL 14 DAY, '10:15:00', '10:45:00', '校田径场', 1, '女子100米预赛'),
    (1, 2, '女子100米决赛', 2, NULL, @demo_day + INTERVAL 15 DAY, '15:45:00', '16:15:00', '校田径场', 1, '女子100米决赛'),
    (1, 3, '男子跳远决赛', 1, NULL, @demo_day + INTERVAL 16 DAY, '09:00:00', '12:00:00', '跳远沙坑', 1, '男子跳远决赛');

-- 插入新闻/公告数据
-- status：0草稿 1已发布
INSERT INTO news (title, news_type, content, event_id, is_top, view_count, publish_time, status, create_by)
VALUES
    ('校园田径运动会开放报名', 'NOTICE', '请在赛事列表选择参赛项目，提交后可在个人工作台查看审核状态。报名截止时间以赛事详情为准。', 1, 1, 0, NOW(), 1, 1),
    ('校园篮球联赛报名通知', 'NOTICE', '请各学院组织队伍报名，并在比赛前查看赛程。比赛地点为校体育馆。', 2, 0, 0, NOW(), 1, 1),
    ('运动会安全须知', 'NOTICE', '请提前到达检录区并完成热身。观赛同学请在指定区域停留，听从现场工作人员指引。', 1, 1, 0, NOW(), 1, 1),
    ('上届田径运动会成绩公告', 'RESULT', '上届田径运动会已结束。男子100米项目的成绩和名次可在赛事成绩页查询。', 4, 0, 0, TIMESTAMP(@demo_day - INTERVAL 87 DAY, '09:00:00'), 1, 1);

-- 写入上届田径运动会的项目、已通过的报名和比赛成绩。
INSERT INTO sport_item (event_id, item_name, item_type, gender_limit, max_participants, result_type, result_unit, sort_rule, rules, status)
VALUES (4, '男子100米', 'TRACK', 1, 30, 'TIME', '秒', 'ASC', '预赛取前8名进入决赛', 1);

INSERT INTO registration (event_id, sport_item_id, user_id, status, remark, create_time, update_time)
VALUES
    (4, 10, 4, 2, '报名参加上届田径运动会', @demo_day - INTERVAL 110 DAY, @demo_day - INTERVAL 109 DAY),
    (4, 10, 6, 2, '报名参加上届田径运动会', @demo_day - INTERVAL 109 DAY, @demo_day - INTERVAL 108 DAY),
    (4, 10, 8, 2, '报名参加上届田径运动会', @demo_day - INTERVAL 108 DAY, @demo_day - INTERVAL 107 DAY);

-- 成绩数据按「用户 + 项目」关联写入，避免硬编码 registration_id 因自增顺序变化而错位
INSERT INTO result (event_id, sport_item_id, registration_id, user_id, score, score_text, ranking, points, record_by, record_time, remark)
SELECT r.event_id, r.sport_item_id, r.id, r.user_id, t.score, t.score_text, t.ranking, t.points,
       2, TIMESTAMP(@demo_day - INTERVAL 88 DAY, '17:30:00'), t.remark
FROM (
    SELECT 4 AS uid, 11.25 AS score, '11.25秒' AS score_text, 1 AS ranking, 9 AS points, '表现优秀' AS remark UNION ALL
    SELECT 6, 11.58, '11.58秒', 2, 7, NULL UNION ALL
    SELECT 8, 11.89, '11.89秒', 3, 6, NULL
) t
JOIN registration r ON r.user_id = t.uid AND r.event_id = 4 AND r.sport_item_id = 10 AND r.is_deleted = 0;

-- 回填项目当前报名人数（只统计已通过）
UPDATE sport_item si
SET si.current_participants = (
    SELECT COUNT(*) FROM registration r
    WHERE r.sport_item_id = si.id AND r.status = 2 AND r.is_deleted = 0
);
