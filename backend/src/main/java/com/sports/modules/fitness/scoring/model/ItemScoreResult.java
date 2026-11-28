package com.sports.modules.fitness.scoring.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 单个项目的评分明细。
 */
public class ItemScoreResult {

    private final String itemCode;
    private final String itemName;
    private final String unit;
    private final BigDecimal rawValue;
    private final int score;
    private final BigDecimal weight;
    private final BigDecimal weightedScore;
    private final int bonus;

    public ItemScoreResult(String itemCode,
                           String itemName,
                           String unit,
                           BigDecimal rawValue,
                           int score,
                           BigDecimal weight,
                           BigDecimal weightedScore,
                           int bonus) {
        this.itemCode = Objects.requireNonNull(itemCode, "itemCode");
        this.itemName = Objects.requireNonNull(itemName, "itemName");
        this.unit = unit;
        this.rawValue = rawValue;
        this.score = score;
        this.weight = weight;
        this.weightedScore = weightedScore;
        this.bonus = bonus;
    }

    public String getItemCode() {
        return itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getRawValue() {
        return rawValue;
    }

    public int getScore() {
        return score;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getWeightedScore() {
        return weightedScore;
    }

    public int getBonus() {
        return bonus;
    }
}
