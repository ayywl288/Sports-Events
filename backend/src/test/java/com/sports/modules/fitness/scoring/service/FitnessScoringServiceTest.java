package com.sports.modules.fitness.scoring.service;

import com.sports.modules.fitness.scoring.data.InMemoryScoringDataProvider;
import com.sports.modules.fitness.scoring.model.Gender;
import com.sports.modules.fitness.scoring.model.GroupType;
import com.sports.modules.fitness.scoring.model.RawScore;
import com.sports.modules.fitness.scoring.model.ScoreInput;
import com.sports.modules.fitness.scoring.model.ScoreReport;
import com.sports.modules.fitness.scoring.model.ScoringItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FitnessScoringServiceTest {

    private InMemoryScoringDataProvider provider;
    private FitnessScoringService service;

    @BeforeEach
    void setUp() {
        provider = new InMemoryScoringDataProvider();
        service = new FitnessScoringService(provider);
    }

    private ScoringItem item(Gender gender, GroupType groupType, String code) {
        return provider.findItem(gender, groupType, code);
    }

    @Test
    @DisplayName("高优指标：达到或超过成绩线即得该档分数")
    void highBetterLookup() {
        ScoringItem vital = item(Gender.MALE, GroupType.UNIV_1_2, "VITAL_CAPACITY");

        assertEquals(100, service.lookupScore(vital, new BigDecimal("5040")));
        assertEquals(95, service.lookupScore(vital, new BigDecimal("5039")));
        assertEquals(95, service.lookupScore(vital, new BigDecimal("4920")));
        assertEquals(74, service.lookupScore(vital, new BigDecimal("4000")));
        assertEquals(60, service.lookupScore(vital, new BigDecimal("3100")));
        assertEquals(0, service.lookupScore(vital, new BigDecimal("2299")));
    }

    @Test
    @DisplayName("低优指标：等于或小于成绩线即得该档分数")
    void lowBetterLookup() {
        ScoringItem run50 = item(Gender.MALE, GroupType.UNIV_1_2, "RUN_50M");

        assertEquals(100, service.lookupScore(run50, new BigDecimal("6.7")));
        assertEquals(95, service.lookupScore(run50, new BigDecimal("6.71")));
        assertEquals(95, service.lookupScore(run50, new BigDecimal("6.8")));
        assertEquals(90, service.lookupScore(run50, new BigDecimal("6.9")));
        assertEquals(60, service.lookupScore(run50, new BigDecimal("9.1")));
        assertEquals(10, service.lookupScore(run50, new BigDecimal("10.1")));
        assertEquals(0, service.lookupScore(run50, new BigDecimal("10.2")));
    }

    @Test
    @DisplayName("BMI 按区间归类")
    void bmiLookup() {
        ScoringItem maleBmi = item(Gender.MALE, GroupType.UNIV_1_2, "BMI");
        assertEquals(80, service.lookupScore(maleBmi, new BigDecimal("17.8")));
        assertEquals(100, service.lookupScore(maleBmi, new BigDecimal("17.9")));
        assertEquals(100, service.lookupScore(maleBmi, new BigDecimal("21.0")));
        assertEquals(100, service.lookupScore(maleBmi, new BigDecimal("23.9")));
        assertEquals(80, service.lookupScore(maleBmi, new BigDecimal("24.0")));
        assertEquals(80, service.lookupScore(maleBmi, new BigDecimal("27.9")));
        assertEquals(60, service.lookupScore(maleBmi, new BigDecimal("28.0")));

        ScoringItem femaleBmi = item(Gender.FEMALE, GroupType.UNIV_1_2, "BMI");
        assertEquals(80, service.lookupScore(femaleBmi, new BigDecimal("17.1")));
        assertEquals(100, service.lookupScore(femaleBmi, new BigDecimal("17.2")));
        assertEquals(100, service.lookupScore(femaleBmi, new BigDecimal("23.9")));
        assertEquals(80, service.lookupScore(femaleBmi, new BigDecimal("24.0")));
        assertEquals(60, service.lookupScore(femaleBmi, new BigDecimal("28.0")));
    }

    @Test
    @DisplayName("综合得分：标准分 = Σ(单项得分 × 权重)")
    void comprehensiveScore() {
        ScoreInput input = new ScoreInput(Gender.MALE, GroupType.UNIV_1_2, List.of(
                new RawScore("BMI", new BigDecimal("21.0")),            // 100 × 15%
                new RawScore("VITAL_CAPACITY", new BigDecimal("4000")), // 74 × 15%
                new RawScore("RUN_50M", new BigDecimal("9.2")),         // 50 × 20%
                new RawScore("SIT_REACH", new BigDecimal("10.0")),      // 68 × 10%
                new RawScore("LONG_JUMP", new BigDecimal("230")),       // 70 × 10%
                new RawScore("PULL_UP", new BigDecimal("12")),          // 68 × 10%
                new RawScore("RUN_1000M", new BigDecimal("250"))        // 68 × 20%
        ));

        ScoreReport report = service.calculate(input);

        assertEquals(7, report.getItems().size());
        assertTrue(report.getMissingItemCodes().isEmpty());
        assertEquals(new BigDecimal("70.30"), report.getStandardScore());
        assertEquals(0, report.getBonusScore());
        assertEquals(new BigDecimal("70.30"), report.getTotalScore());
        assertEquals("及格", report.getGrade());

        assertEquals(74, report.getItems().stream()
                .filter(i -> i.getItemCode().equals("VITAL_CAPACITY"))
                .findFirst().orElseThrow().getScore());
        assertEquals(new BigDecimal("10.00"), report.getItems().stream()
                .filter(i -> i.getItemCode().equals("RUN_50M"))
                .findFirst().orElseThrow().getWeightedScore());
    }

    @Test
    @DisplayName("附加分：按超出 100 分档幅度查表，单项封顶 10 分")
    void bonusRules() {
        ScoringItem pullUp = item(Gender.MALE, GroupType.UNIV_1_2, "PULL_UP");
        assertEquals(0, service.calculateBonus(pullUp, new BigDecimal("18")));
        assertEquals(6, service.calculateBonus(pullUp, new BigDecimal("25")));
        assertEquals(10, service.calculateBonus(pullUp, new BigDecimal("40")));

        ScoringItem run1000 = item(Gender.MALE, GroupType.UNIV_1_2, "RUN_1000M");
        assertEquals(0, service.calculateBonus(run1000, new BigDecimal("197")));
        assertEquals(5, service.calculateBonus(run1000, new BigDecimal("177")));
        assertEquals(10, service.calculateBonus(run1000, new BigDecimal("160")));

        ScoringItem sitUp = item(Gender.FEMALE, GroupType.UNIV_1_2, "SIT_UP");
        assertEquals(0, service.calculateBonus(sitUp, new BigDecimal("56")));
        assertEquals(1, service.calculateBonus(sitUp, new BigDecimal("58")));
        assertEquals(2, service.calculateBonus(sitUp, new BigDecimal("60")));
        assertEquals(10, service.calculateBonus(sitUp, new BigDecimal("69")));

        ScoringItem run800 = item(Gender.FEMALE, GroupType.UNIV_1_2, "RUN_800M");
        assertEquals(4, service.calculateBonus(run800, new BigDecimal("178")));
        assertEquals(10, service.calculateBonus(run800, new BigDecimal("148")));
    }

    @Test
    @DisplayName("全项满分的总分应为标准分 100 + 附加分")
    void fullMarks() {
        ScoreInput input = new ScoreInput(Gender.MALE, GroupType.UNIV_1_2, List.of(
                new RawScore("BMI", new BigDecimal("21.0")),
                new RawScore("VITAL_CAPACITY", new BigDecimal("5040")),
                new RawScore("RUN_50M", new BigDecimal("6.7")),
                new RawScore("SIT_REACH", new BigDecimal("24.9")),
                new RawScore("LONG_JUMP", new BigDecimal("273")),
                new RawScore("PULL_UP", new BigDecimal("25")),
                new RawScore("RUN_1000M", new BigDecimal("197"))
        ));

        ScoreReport report = service.calculate(input);

        assertEquals(new BigDecimal("100.00"), report.getStandardScore());
        assertEquals(6, report.getBonusScore());
        assertEquals(new BigDecimal("106.00"), report.getTotalScore());
        assertEquals("优秀", report.getGrade());
    }

    @Test
    @DisplayName("缺测项目不按 0 分计入，并在结果中标记")
    void missingItems() {
        ScoreInput input = new ScoreInput(Gender.MALE, GroupType.UNIV_1_2, List.of(
                new RawScore("BMI", new BigDecimal("21.0")),
                new RawScore("VITAL_CAPACITY", new BigDecimal("4000"))
        ));

        ScoreReport report = service.calculate(input);

        assertEquals(new BigDecimal("26.10"), report.getStandardScore());
        assertEquals(5, report.getMissingItemCodes().size());
        assertTrue(report.getMissingItemCodes().contains("RUN_50M"));
        assertTrue(report.getMissingItemCodes().contains("RUN_1000M"));
    }

    @Test
    @DisplayName("未知项目、重复项目、空性别应被拒绝")
    void invalidInput() {
        ScoreInput unknown = new ScoreInput(Gender.MALE, GroupType.UNIV_1_2, List.of(
                new RawScore("NOT_EXIST", new BigDecimal("1"))
        ));
        assertThrows(IllegalArgumentException.class, () -> service.calculate(unknown));

        ScoreInput duplicated = new ScoreInput(Gender.MALE, GroupType.UNIV_1_2, List.of(
                new RawScore("BMI", new BigDecimal("21.0")),
                new RawScore("BMI", new BigDecimal("22.0"))
        ));
        assertThrows(IllegalArgumentException.class, () -> service.calculate(duplicated));

        ScoreInput noGender = new ScoreInput(null, GroupType.UNIV_1_2, List.of());
        assertThrows(IllegalArgumentException.class, () -> service.calculate(noGender));
    }
}
