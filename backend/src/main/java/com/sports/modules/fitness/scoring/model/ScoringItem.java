package com.sports.modules.fitness.scoring.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 一个体测单项指标的完整定义：名称、单位、权重、测量方向、评分档次、加分规则。
 */
public class ScoringItem {

    private final String code;
    private final String name;
    private final String unit;
    /** 权重，百分数口径，如 15 表示 15%。 */
    private final BigDecimal weight;
    private final ItemMeasureType measureType;
    private final boolean bonusItem;
    private final List<ScoringGrade> grades;
    private final BonusRule bonusRule;

    // 以下三个字段仅 BMI_RANGE 使用
    private final BigDecimal bmiNormalMin;
    private final BigDecimal bmiNormalMax;
    private final BigDecimal bmiObeseMin;

    public ScoringItem(String code,
                       String name,
                       String unit,
                       BigDecimal weight,
                       ItemMeasureType measureType,
                       boolean bonusItem,
                       List<ScoringGrade> grades,
                       BonusRule bonusRule,
                       BigDecimal bmiNormalMin,
                       BigDecimal bmiNormalMax,
                       BigDecimal bmiObeseMin) {
        this.code = Objects.requireNonNull(code, "code");
        this.name = Objects.requireNonNull(name, "name");
        this.unit = unit;
        this.weight = Objects.requireNonNull(weight, "weight");
        this.measureType = Objects.requireNonNull(measureType, "measureType");
        this.bonusItem = bonusItem;
        this.grades = grades == null ? List.of() : List.copyOf(grades);
        this.bonusRule = bonusRule;
        this.bmiNormalMin = bmiNormalMin;
        this.bmiNormalMax = bmiNormalMax;
        this.bmiObeseMin = bmiObeseMin;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public ItemMeasureType getMeasureType() {
        return measureType;
    }

    public boolean isBonusItem() {
        return bonusItem;
    }

    public List<ScoringGrade> getGrades() {
        return grades;
    }

    public BonusRule getBonusRule() {
        return bonusRule;
    }

    public BigDecimal getBmiNormalMin() {
        return bmiNormalMin;
    }

    public BigDecimal getBmiNormalMax() {
        return bmiNormalMax;
    }

    public BigDecimal getBmiObeseMin() {
        return bmiObeseMin;
    }

    /**
     * 100 分档成绩线；BMI 为区间指标，没有成绩线，返回 null。
     * 该值由服务层内部使用，不对外序列化。
     */
    @JsonIgnore
    public BigDecimal getHundredThreshold() {
        return grades.isEmpty() ? null : grades.get(0).getThreshold();
    }
}
