package com.sports.modules.fitness.scoring.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * 评分结果：单项明细、标准分、附加分、学年总分、等级，以及缺测项目。
 */
public class ScoreReport {

    private final List<ItemScoreResult> items;
    private final BigDecimal standardScore;
    private final int bonusScore;
    private final BigDecimal totalScore;
    private final String grade;
    private final List<String> missingItemCodes;

    public ScoreReport(List<ItemScoreResult> items,
                       BigDecimal standardScore,
                       int bonusScore,
                       BigDecimal totalScore,
                       String grade,
                       List<String> missingItemCodes) {
        this.items = List.copyOf(items);
        this.standardScore = standardScore;
        this.bonusScore = bonusScore;
        this.totalScore = totalScore;
        this.grade = grade;
        this.missingItemCodes = List.copyOf(missingItemCodes);
    }

    public List<ItemScoreResult> getItems() {
        return items;
    }

    public BigDecimal getStandardScore() {
        return standardScore;
    }

    public int getBonusScore() {
        return bonusScore;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public String getGrade() {
        return grade;
    }

    public List<String> getMissingItemCodes() {
        return missingItemCodes;
    }
}
