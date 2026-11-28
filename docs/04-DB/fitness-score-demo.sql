-- 国标体测成绩单示例。仅为既有演示学生填入完整的模拟成绩。
-- 标准分、附加分、总分和等级由项目的国标评分引擎算出。
SET NAMES utf8mb4;
SET @score_demo_date = COALESCE(@score_demo_date, CURRENT_DATE() - INTERVAL 1 DAY);
SET @score_demo_year = YEAR(@score_demo_date) - (MONTH(@score_demo_date) < 9);

INSERT INTO fitness_score_record (
    user_id, test_date, height_cm, weight_kg, vital_capacity, run50_seconds, sit_reach_cm,
    jump_cm, strength_count, endurance_seconds, gender, group_type, academic_year,
    standard_score, bonus_score, total_score, grade, status, audit_operator, audit_time
)
SELECT
    u.id, @score_demo_date, demo.height, demo.weight, demo.capacity, demo.sprint, demo.reach,
    demo.jump, demo.strength, demo.endurance, demo.gender, demo.group_type,
    CONCAT(@score_demo_year, '-', @score_demo_year + 1, '学年'),
    demo.standard_score, demo.bonus_score, demo.total_score, demo.grade, 'VALID',
    '演示初始化', CURRENT_TIMESTAMP
FROM (
    SELECT 'student1' AS username, '2021001' AS student_no, 'MALE' AS gender, 'UNIV_1_2' AS group_type,
           174 AS height, 60 AS weight, 4050 AS capacity, 7.0 AS sprint, 21.5 AS reach,
           250 AS jump, 16 AS strength, 227 AS endurance,
           84.20 AS standard_score, 0 AS bonus_score, 84.20 AS total_score, '良好' AS grade
    UNION ALL SELECT 'student2', '2021002', 'FEMALE', 'UNIV_1_2',
           164, 54, 3900, 7.3, 26.0, 229, 60, 193, 100.00, 3, 103.00, '优秀'
    UNION ALL SELECT 'student3', '2021003', 'MALE', 'UNIV_3_4',
           172, 72, 3250, 8.6, 9.8, 205, 12, 255, 65.20, 0, 65.20, '及格'
    UNION ALL SELECT 'student4', '2021004', 'FEMALE', 'UNIV_3_4',
           164, 67, 3090, 8.8, 15.6, 197, 40, 245, 76.40, 0, 76.40, '及格'
    UNION ALL SELECT 'student5', '2021005', 'MALE', 'UNIV_1_2',
           177, 87, 2520, 10.8, 1.7, 166, 5, 340, 24.00, 0, 24.00, '不及格'
) AS demo
JOIN sys_user AS u ON u.username = demo.username AND u.student_no = demo.student_no
WHERE u.role = 'STUDENT' AND u.status = 1 AND u.is_deleted = 0
ORDER BY u.id
ON DUPLICATE KEY UPDATE id = fitness_score_record.id;
