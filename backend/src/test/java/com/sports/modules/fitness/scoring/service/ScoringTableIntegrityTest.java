package com.sports.modules.fitness.scoring.service;

import com.sports.modules.fitness.scoring.data.InMemoryScoringDataProvider;
import com.sports.modules.fitness.scoring.model.Gender;
import com.sports.modules.fitness.scoring.model.GroupType;
import com.sports.modules.fitness.scoring.model.ItemMeasureType;
import com.sports.modules.fitness.scoring.model.ScoringGrade;
import com.sports.modules.fitness.scoring.model.ScoringItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 评分表完整性校验：把内存中的每一张评分表的每一档都回代到查表函数，
 * 保证"成绩线 → 得分"与标准一致，同时校验权重、档位单调性和附加分上限。
 */
class ScoringTableIntegrityTest {

    private static final BigDecimal EPS = new BigDecimal("0.01");

    private InMemoryScoringDataProvider provider;
    private FitnessScoringService service;

    @BeforeEach
    void setUp() {
        provider = new InMemoryScoringDataProvider();
        service = new FitnessScoringService(provider);
    }

    @Test
    @DisplayName("四套大学组评分表：档位、权重、附加分全部可复算")
    void everyGradeRowIsConsistent() {
        for (Gender gender : Gender.values()) {
            for (GroupType groupType : GroupType.values()) {
                verifyGroup(gender, groupType);
            }
        }
    }

    private void verifyGroup(Gender gender, GroupType groupType) {
        List<ScoringItem> items = provider.findItems(gender, groupType);

        assertEquals(7, items.size(), gender + "/" + groupType + " 应有 7 个测试项目");

        BigDecimal weightSum = items.stream()
                .map(ScoringItem::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, weightSum.compareTo(new BigDecimal("100")),
                gender + "/" + groupType + " 权重合计应为 100%");

        for (ScoringItem item : items) {
            assertNotNull(item.getMeasureType());
            if (item.getMeasureType() == ItemMeasureType.BMI_RANGE) {
                verifyBmi(item, gender);
                continue;
            }

            List<ScoringGrade> grades = item.getGrades();
            assertTrue(grades.size() >= 15, item.getCode() + " 评分档位过少");
            assertEquals(100, grades.get(0).getScore(), item.getCode() + " 首档应为 100 分");

            for (int i = 0; i < grades.size(); i++) {
                ScoringGrade grade = grades.get(i);

                // 1) 成绩线本身应落在该档
                assertEquals(grade.getScore(),
                        service.lookupScore(item, grade.getThreshold()),
                        item.getCode() + " 成绩线 " + grade.getThreshold() + " 应得 " + grade.getScore());

                // 2) 比该档差一点，应落到下一档；最后一档则应为 0
                BigDecimal worse = worseValue(item, grade.getThreshold());
                int expected = i + 1 < grades.size() ? grades.get(i + 1).getScore() : 0;
                assertEquals(expected, service.lookupScore(item, worse),
                        item.getCode() + " 成绩略差于 " + grade.getThreshold() + " 应得 " + expected);
            }

            // 3) 档位单调性：高优阈值递减，低优阈值递增
            for (int i = 1; i < grades.size(); i++) {
                int cmp = grades.get(i).getThreshold().compareTo(grades.get(i - 1).getThreshold());
                if (item.getMeasureType() == ItemMeasureType.HIGH_BETTER) {
                    assertTrue(cmp < 0, item.getCode() + " 高优指标阈值应递减");
                } else {
                    assertTrue(cmp > 0, item.getCode() + " 低优指标阈值应递增");
                }
            }

            // 4) 附加分：必须封顶 10 分
            if (item.isBonusItem()) {
                assertNotNull(item.getBonusRule());
                assertEquals(10, item.getBonusRule().maxPoints());
                BigDecimal farOver = item.getMeasureType() == ItemMeasureType.HIGH_BETTER
                        ? item.getHundredThreshold().add(new BigDecimal("999"))
                        : item.getHundredThreshold().subtract(new BigDecimal("999"));
                assertEquals(10, service.calculateBonus(item, farOver));
            } else {
                assertEquals(0, service.calculateBonus(item, item.getHundredThreshold()));
            }
        }
    }

    private void verifyBmi(ScoringItem item, Gender gender) {
        BigDecimal normalMin = item.getBmiNormalMin();
        BigDecimal normalMax = item.getBmiNormalMax();
        BigDecimal obeseMin = item.getBmiObeseMin();

        assertEquals(80, service.lookupScore(item, normalMin.subtract(EPS)));
        assertEquals(100, service.lookupScore(item, normalMin));
        assertEquals(100, service.lookupScore(item, normalMax));
        assertEquals(80, service.lookupScore(item, normalMax.add(EPS)));
        assertEquals(60, service.lookupScore(item, obeseMin));
    }

    private BigDecimal worseValue(ScoringItem item, BigDecimal threshold) {
        return item.getMeasureType() == ItemMeasureType.LOW_BETTER
                ? threshold.add(EPS)
                : threshold.subtract(EPS);
    }
}
