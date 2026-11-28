-- 国标体测评分表（与 fitness_record 基础测量表分表存储，互不影响）
-- 在现有 sports_events 数据库执行；不删除、不修改已有表。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS fitness_score_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL COMMENT '学生用户ID',
  test_date DATE NOT NULL COMMENT '测试日期',
  height_cm DOUBLE NOT NULL COMMENT '身高cm',
  weight_kg DOUBLE NOT NULL COMMENT '体重kg',
  vital_capacity DOUBLE NULL COMMENT '肺活量ml',
  run50_seconds DOUBLE NULL COMMENT '50米秒数',
  sit_reach_cm DOUBLE NULL COMMENT '坐位体前屈cm',
  jump_cm DOUBLE NULL COMMENT '立定跳远cm',
  strength_count INT NULL COMMENT '男生引体向上/女生仰卧起坐次数',
  endurance_seconds DOUBLE NULL COMMENT '男生1000米/女生800米秒数',
  gender VARCHAR(10) NOT NULL COMMENT 'MALE/FEMALE',
  group_type VARCHAR(16) NOT NULL COMMENT 'UNIV_1_2/UNIV_3_4',
  academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2026-2027学年',
  standard_score DECIMAL(6,2) NULL COMMENT '标准分，满分100',
  bonus_score INT NULL COMMENT '附加分，满分20',
  total_score DECIMAL(6,2) NULL COMMENT '学年总分，满分120',
  grade VARCHAR(10) NULL COMMENT '优秀/良好/及格/不及格',
  status VARCHAR(10) NOT NULL DEFAULT 'VALID' COMMENT 'VALID有效/INVALID无效',
  audit_operator VARCHAR(50) NULL COMMENT '审核人',
  audit_time DATETIME NULL COMMENT '审核时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_score_user_date (user_id, test_date),
  KEY idx_score_status (status),
  KEY idx_score_year (academic_year),
  CONSTRAINT fk_score_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国标体测原始成绩与评分结果';
