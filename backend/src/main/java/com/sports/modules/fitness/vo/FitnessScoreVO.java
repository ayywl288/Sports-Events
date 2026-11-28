package com.sports.modules.fitness.vo;

import com.sports.modules.fitness.scoring.model.ItemScoreResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 国标体测成绩单视图。
 */
@Data
public class FitnessScoreVO {

    private Long id;
    private Long userId;
    private String studentName;
    private String studentNo;
    private String gender;
    private String genderLabel;
    private String groupType;
    private String groupTypeLabel;
    private String academicYear;
    private LocalDate testDate;

    private Double heightCm;
    private Double weightKg;
    private Double bmi;
    private Double vitalCapacity;
    private Double run50Seconds;
    private Double sitReachCm;
    private Double jumpCm;
    private Integer strengthCount;
    private Double enduranceSeconds;

    private BigDecimal standardScore;
    private Integer bonusScore;
    private BigDecimal totalScore;
    private String grade;
    private String status;
    private String statusLabel;
    private String auditOperator;
    private LocalDateTime auditTime;

    private List<ItemScoreResult> items;
    private List<String> missingItemCodes;
}
