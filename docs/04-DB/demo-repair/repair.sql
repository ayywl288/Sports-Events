-- 修复旧版 Docker 导入的演示记录。此处保留旧种子的原文，用于比对。
-- 只把与旧种子经 MySQL latin1 误读后逐字节相同的字段改回中文。
-- 使用演示记录的键定位，保留自定义字段、业务状态和更新时间。可重复执行。
SET NAMES utf8mb4;
START TRANSACTION;

WITH demo (id, username, real_name, department, class_name) AS (
    SELECT 1, 'admin', '系统管理员', '体育部', NULL
    UNION ALL SELECT 2, 'teacher1', '张教练', '体育部', NULL
    UNION ALL SELECT 3, 'teacher2', '李裁判', '体育部', NULL
    UNION ALL SELECT 4, 'student1', '王小明', '计算机学院', '软件2101'
    UNION ALL SELECT 5, 'student2', '李小红', '计算机学院', '软件2101'
    UNION ALL SELECT 6, 'student3', '张小强', '机械学院', '机械2101'
    UNION ALL SELECT 7, 'student4', '刘小芳', '经济学院', '金融2101'
    UNION ALL SELECT 8, 'student5', '陈小伟', '外语学院', '英语2101'
)
UPDATE sys_user AS live
JOIN demo ON live.id = demo.id AND live.username = demo.username
SET
    live.real_name = IF(
        CAST(live.real_name AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.real_name AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.real_name, live.real_name
    ),
    live.department = IF(
        CAST(live.department AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.department AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.department, live.department
    ),
    live.class_name = IF(
        CAST(live.class_name AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.class_name AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.class_name, live.class_name
    ),
    live.password = IF(
        demo.username = 'student5'
        AND CAST(live.password AS BINARY) = CAST('$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EH' AS BINARY),
        '$2a$10$HgSUmDRaol7.LJfTyh3YceD7SQ.y8qrL5oQE.h5jnEP1aQMXhOaxm', live.password
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (dict_code, dict_name, remark) AS (
    SELECT 'event_type', '赛事类型', 'TRACK_FIELD田径,BALL球类,SWIMMING游泳'
    UNION ALL SELECT 'item_type', '项目类型', 'TRACK径赛,FIELD田赛,BALL球类,SWIMMING游泳'
    UNION ALL SELECT 'news_type', '公告类型', 'NOTICE赛事通知,RESULT成绩公告,RULE规则说明,OTHER其他'
)
UPDATE sys_dict_type AS live
JOIN demo ON live.dict_code = demo.dict_code
SET
    live.dict_name = IF(
        CAST(live.dict_name AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.dict_name AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.dict_name, live.dict_name
    ),
    live.remark = IF(
        CAST(live.remark AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.remark AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.remark, live.remark
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (dict_code, item_value, item_label) AS (
    SELECT 'event_type', 'TRACK_FIELD', '田径赛事'
    UNION ALL SELECT 'event_type', 'BALL', '球类赛事'
    UNION ALL SELECT 'event_type', 'SWIMMING', '游泳赛事'
    UNION ALL SELECT 'item_type', 'TRACK', '径赛'
    UNION ALL SELECT 'item_type', 'FIELD', '田赛'
    UNION ALL SELECT 'item_type', 'BALL', '球类'
    UNION ALL SELECT 'item_type', 'SWIMMING', '游泳'
    UNION ALL SELECT 'news_type', 'NOTICE', '赛事通知'
    UNION ALL SELECT 'news_type', 'RESULT', '成绩公告'
    UNION ALL SELECT 'news_type', 'RULE', '规则说明'
    UNION ALL SELECT 'news_type', 'OTHER', '其他'
)
UPDATE sys_dict_data AS live
JOIN demo ON live.dict_code = demo.dict_code AND live.item_value = demo.item_value
SET
    live.item_label = IF(
        CAST(live.item_label AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.item_label AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.item_label, live.item_label
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (id, event_type, event_name, description, location, organizer) AS (
    SELECT 1, 'TRACK_FIELD', '2024年春季田径运动会', '一年一度的春季田径运动会，展现青春活力，挑战自我极限！', '校田径场', '体育部'
    UNION ALL SELECT 2, 'BALL', '2024年篮球联赛', '校园篮球联赛，以球会友，展现团队精神！', '体育馆', '体育部'
    UNION ALL SELECT 3, 'SWIMMING', '2024年游泳比赛', '夏日清凉，泳池竞技！', '游泳馆', '体育部'
    UNION ALL SELECT 4, 'TRACK_FIELD', '2023年秋季运动会', '2023年秋季综合运动会', '校田径场', '体育部'
)
UPDATE event AS live
JOIN demo ON live.id = demo.id AND live.event_type = demo.event_type
SET
    live.event_name = IF(
        CAST(live.event_name AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.event_name AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.event_name, live.event_name
    ),
    live.description = IF(
        CAST(live.description AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.description AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.description, live.description
    ),
    live.location = IF(
        CAST(live.location AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.location AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.location, live.location
    ),
    live.organizer = IF(
        CAST(live.organizer AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.organizer AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.organizer, live.organizer
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (id, event_id, item_name, result_unit, rules) AS (
    SELECT 1, 1, '男子100米', '秒', '预赛取前8名进入决赛'
    UNION ALL SELECT 2, 1, '女子100米', '秒', '预赛取前8名进入决赛'
    UNION ALL SELECT 3, 1, '男子跳远', '米', '每人3次试跳，取最好成绩'
    UNION ALL SELECT 4, 1, '女子跳远', '米', '每人3次试跳，取最好成绩'
    UNION ALL SELECT 5, 1, '男子铅球', '米', '每人3次投掷，取最好成绩'
    UNION ALL SELECT 6, 2, '男子篮球', '分', '5人制，小组赛+淘汰赛'
    UNION ALL SELECT 7, 2, '女子篮球', '分', '5人制，小组赛+淘汰赛'
    UNION ALL SELECT 8, 3, '男子50米自由泳', '秒', '预赛取前8名进入决赛'
    UNION ALL SELECT 9, 3, '女子50米自由泳', '秒', '预赛取前8名进入决赛'
    UNION ALL SELECT 10, 4, '男子100米', '秒', '预赛取前8名进入决赛'
)
UPDATE sport_item AS live
JOIN demo ON live.id = demo.id AND live.event_id = demo.event_id
SET
    live.item_name = IF(
        CAST(live.item_name AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.item_name AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.item_name, live.item_name
    ),
    live.result_unit = IF(
        CAST(live.result_unit AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.result_unit AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.result_unit, live.result_unit
    ),
    live.rules = IF(
        CAST(live.rules AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.rules AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.rules, live.rules
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (id, event_id, sport_item_id, user_id, remark) AS (
    SELECT 1, 1, 1, 4, '希望能取得好成绩'
    UNION ALL SELECT 4, 1, 3, 6, '第一次参加'
    UNION ALL SELECT 10, 4, 10, 4, '2023秋运会参赛'
    UNION ALL SELECT 11, 4, 10, 6, '2023秋运会参赛'
    UNION ALL SELECT 12, 4, 10, 8, '2023秋运会参赛'
)
UPDATE registration AS live
JOIN demo ON live.id = demo.id AND live.event_id = demo.event_id AND live.sport_item_id = demo.sport_item_id AND live.user_id = demo.user_id
SET
    live.remark = IF(
        CAST(live.remark AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.remark AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.remark, live.remark
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (id, sport_item_id, schedule_name, group_no, location, remark) AS (
    SELECT 1, 1, '男子100米预赛', '1组', '田径场1号跑道', '男子100米预赛第1组'
    UNION ALL SELECT 2, 1, '男子100米预赛', '2组', '田径场1号跑道', '男子100米预赛第2组'
    UNION ALL SELECT 3, 1, '男子100米决赛', NULL, '田径场1号跑道', '男子100米决赛'
    UNION ALL SELECT 4, 2, '女子100米预赛', '1组', '田径场2号跑道', '女子100米预赛'
    UNION ALL SELECT 5, 2, '女子100米决赛', NULL, '田径场2号跑道', '女子100米决赛'
    UNION ALL SELECT 6, 3, '男子跳远决赛', NULL, '跳远沙坑', '男子跳远决赛'
)
UPDATE schedule AS live
JOIN demo ON live.id = demo.id AND live.sport_item_id = demo.sport_item_id AND live.event_id = 1
SET
    live.schedule_name = IF(
        CAST(live.schedule_name AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.schedule_name AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.schedule_name, live.schedule_name
    ),
    live.group_no = IF(
        CAST(live.group_no AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.group_no AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.group_no, live.group_no
    ),
    live.location = IF(
        CAST(live.location AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.location AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.location, live.location
    ),
    live.remark = IF(
        CAST(live.remark AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.remark AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.remark, live.remark
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (registration_id, user_id, score_text, remark) AS (
    SELECT 10, 4, '11.25秒', '表现优秀'
    UNION ALL SELECT 11, 6, '11.58秒', NULL
    UNION ALL SELECT 12, 8, '11.89秒', NULL
)
UPDATE result AS live
JOIN demo ON live.registration_id = demo.registration_id AND live.user_id = demo.user_id AND live.event_id = 4 AND live.sport_item_id = 10
SET
    live.score_text = IF(
        CAST(live.score_text AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.score_text AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.score_text, live.score_text
    ),
    live.remark = IF(
        CAST(live.remark AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.remark AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.remark, live.remark
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

WITH demo (id, event_id, title, content) AS (
    SELECT 1, 1, '2024年春季田径运动会报名开始',
        '各位同学，2024年春季田径运动会报名已经开始，请有意参加的同学尽快报名！报名截止日期为4月10日。'
    UNION ALL SELECT 2, 2, '篮球联赛即将开赛',
        '2024年校园篮球联赛将于5月1日正式开赛，欢迎各学院组队参加！'
    UNION ALL SELECT 3, 1, '运动会安全须知',
        '为确保运动会顺利进行，请所有参赛选手和观众遵守以下安全规定...'
    UNION ALL SELECT 4, 4, '上届运动会精彩回顾',
        '2023年秋季运动会圆满落幕，让我们一起回顾那些精彩瞬间...'
)
UPDATE news AS live
JOIN demo ON live.id = demo.id AND live.event_id = demo.event_id
SET
    live.title = IF(
        CAST(live.title AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.title AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.title, live.title
    ),
    live.content = IF(
        CAST(live.content AS BINARY) = CAST(CONVERT(
            CONVERT(CAST(demo.content AS BINARY) USING latin1) USING utf8mb4
        ) AS BINARY),
        demo.content, live.content
    ),
    live.update_time = live.update_time
WHERE live.is_deleted = 0;

COMMIT;
