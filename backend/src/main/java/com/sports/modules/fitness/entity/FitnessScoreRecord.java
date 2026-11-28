package com.sports.modules.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 国标体测评分记录：一条记录对应一名学生一次测试的原始成绩与评分结果。
 * <p>
 * 与小组已有的 fitness_record（基础测量/聚类）分表存储，互不影响。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fitness_score_record")
public class FitnessScoreRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate testDate;

    private Double heightCm;

    private Double weightKg;

    private Double vitalCapacity;

    private Double run50Seconds;

    private Double sitReachCm;

    private Double jumpCm;

    /** 男生引体向上 / 女生仰卧起坐。 */
    private Integer strengthCount;

    /** 男生 1000 米 / 女生 800 米，单位秒。 */
    private Double enduranceSeconds;

    /** MALE / FEMALE。 */
    private String gender;

    /** UNIV_1_2 / UNIV_3_4。 */
    private String groupType;

    private String academicYear;

    private BigDecimal standardScore;

    private Integer bonusScore;

    private BigDecimal totalScore;

    private String grade;

    /** VALID / INVALID。 */
    private String status;

    private String auditOperator;

    private LocalDateTime auditTime;
}
