-- 模拟体测数据。可通过 @fitness_demo_date 将样本追加到已有演示日期。
-- 已存在的同人同日记录保留，附加的演示学生仅供展示，不开放登录。
SET NAMES utf8mb4;
SET @fitness_demo_date = COALESCE(@fitness_demo_date, CURRENT_DATE() - INTERVAL 1 DAY);

CREATE TEMPORARY TABLE fitness_demo_sample (
    sample_no INT PRIMARY KEY,
    height_cm DOUBLE NOT NULL,
    weight_kg DOUBLE NOT NULL,
    vital_capacity DOUBLE NOT NULL,
    run50_seconds DOUBLE NOT NULL,
    jump_cm DOUBLE NOT NULL
);

-- 31 条附加样本与 5 条原演示记录一同验证分组。
INSERT INTO fitness_demo_sample VALUES
    (1, 171, 55, 4200, 7.21, 242),
    (2, 168, 53, 3720, 6.97, 246),
    (3, 176, 67, 4210, 7.48, 233),
    (4, 160, 51, 3700, 7.34, 218),
    (5, 169, 56, 3850, 7.55, 232),
    (6, 183, 70, 4360, 6.86, 259),
    (7, 157, 48, 3590, 7.59, 225),
    (8, 172, 64, 4020, 7.25, 249),
    (9, 164, 57, 4120, 7.66, 232),
    (10, 174, 63, 3880, 7.1, 243),
    (11, 170, 68, 3360, 8.51, 210),
    (12, 166, 66, 3000, 8.84, 207),
    (13, 181, 79, 3500, 8.46, 217),
    (14, 158, 64, 3190, 9.02, 191),
    (15, 172, 71, 3050, 8.91, 197),
    (16, 175, 73, 3270, 8.53, 211),
    (17, 163, 63, 3150, 8.39, 198),
    (18, 178, 82, 3480, 8.7, 214),
    (19, 160, 61, 3140, 8.67, 201),
    (20, 169, 74, 3030, 8.98, 195),
    (21, 173, 76, 3360, 8.32, 208),
    (22, 167, 65, 3050, 8.76, 203),
    (23, 171, 81, 2710, 10.12, 169),
    (24, 168, 81, 2350, 10.63, 162),
    (25, 182, 91, 2660, 9.82, 180),
    (26, 164, 73, 2580, 10.45, 172),
    (27, 157, 72, 2310, 10.78, 160),
    (28, 176, 90, 2730, 9.94, 182),
    (29, 169, 77, 2870, 10.23, 174),
    (30, 184, 93, 2620, 10.53, 171),
    (31, 162, 76, 2470, 10.91, 165);

START TRANSACTION;

INSERT INTO fitness_record
    (user_id, test_date, height_cm, weight_kg, vital_capacity, run50_seconds, jump_cm)
SELECT u.id, DATE_SUB(@fitness_demo_date, INTERVAL demo.days_ago DAY),
       demo.height, demo.weight, demo.capacity, demo.seconds, demo.jump
FROM (
    SELECT 'student1' AS username, '2021001' AS student_no, 0 AS days_ago,
           174 AS height, 60 AS weight, 4050 AS capacity, 7.0 AS seconds, 250 AS jump
    UNION ALL SELECT 'student2', '2021002', 0, 164, 54, 3900, 7.3, 229
    UNION ALL SELECT 'student3', '2021003', 0, 172, 72, 3250, 8.6, 205
    UNION ALL SELECT 'student4', '2021004', 0, 164, 67, 3090, 8.8, 197
    UNION ALL SELECT 'student5', '2021005', 0, 177, 87, 2520, 10.8, 166
    UNION ALL SELECT 'student1', '2021001', 30, 174, 60, 3920, 7.3, 242
) AS demo
JOIN sys_user AS u ON u.username = demo.username AND u.student_no = demo.student_no
WHERE u.role = 'STUDENT' AND u.is_deleted = 0
ORDER BY demo.days_ago, u.id
ON DUPLICATE KEY UPDATE id = fitness_record.id;

INSERT INTO sys_user (username, password, real_name, role, student_no, department, status)
SELECT CONCAT('fitness_demo_', LPAD(sample_no, 2, '0')), '!',
       CONCAT('体测演示', LPAD(sample_no, 2, '0')), 'STUDENT',
       CONCAT('FIT-DEMO-', LPAD(sample_no, 2, '0')), '模拟数据', 0
FROM fitness_demo_sample
ORDER BY sample_no
ON DUPLICATE KEY UPDATE id = sys_user.id;

INSERT INTO fitness_record
    (user_id, test_date, height_cm, weight_kg, vital_capacity, run50_seconds, jump_cm)
SELECT u.id, @fitness_demo_date, sample.height_cm, sample.weight_kg, sample.vital_capacity,
       sample.run50_seconds, sample.jump_cm
FROM fitness_demo_sample AS sample
JOIN sys_user AS u ON u.username = CONCAT('fitness_demo_', LPAD(sample.sample_no, 2, '0'))
    AND u.student_no = CONCAT('FIT-DEMO-', LPAD(sample.sample_no, 2, '0'))
WHERE u.role = 'STUDENT' AND u.department = '模拟数据' AND u.status = 0 AND u.is_deleted = 0
ORDER BY sample.sample_no
ON DUPLICATE KEY UPDATE id = fitness_record.id;

COMMIT;
DROP TEMPORARY TABLE fitness_demo_sample;
