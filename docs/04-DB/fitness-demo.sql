-- 仅填充默认演示学生。已存在的同人同日记录保留。
SET NAMES utf8mb4;

INSERT INTO fitness_record
    (user_id, test_date, height_cm, weight_kg, vital_capacity, run50_seconds, jump_cm)
SELECT u.id, DATE_SUB(CURRENT_DATE(), INTERVAL demo.days_ago DAY),
       demo.height, demo.weight, demo.capacity, demo.seconds, demo.jump
FROM (
    SELECT 'student1' AS username, '2021001' AS student_no, 1 AS days_ago,
           174 AS height, 60 AS weight, 4050 AS capacity, 7.0 AS seconds, 250 AS jump
    UNION ALL SELECT 'student2', '2021002', 1, 164, 54, 3900, 7.3, 229
    UNION ALL SELECT 'student3', '2021003', 1, 172, 72, 3250, 8.6, 205
    UNION ALL SELECT 'student4', '2021004', 1, 164, 67, 3090, 8.8, 197
    UNION ALL SELECT 'student5', '2021005', 1, 177, 87, 2520, 10.8, 166
    UNION ALL SELECT 'student1', '2021001', 31, 174, 60, 3920, 7.3, 242
) AS demo
JOIN sys_user AS u ON u.username = demo.username AND u.student_no = demo.student_no
WHERE u.role = 'STUDENT' AND u.is_deleted = 0
ON DUPLICATE KEY UPDATE id = fitness_record.id;
