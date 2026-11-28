package com.sports.modules.fitness.scoring.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 评分表中的"一个档次"：达到该成绩线可得对应分数。
 * <p>
 * 高优指标：原始成绩 ≥ threshold 即得 score；
 * 低优指标：原始成绩 ≤ threshold 即得 score。
 */
public class ScoringGrade {

    private final int score;
    private final BigDecimal threshold;

    public ScoringGrade(int score, BigDecimal threshold) {
        this.score = score;
        this.threshold = Objects.requireNonNull(threshold, "threshold");
    }

    public int getScore() {
        return score;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    @Override
    public String toString() {
        return score + "@" + threshold;
    }
}
