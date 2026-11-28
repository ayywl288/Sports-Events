package com.sports.modules.fitness.scoring.service;

import com.sports.modules.fitness.scoring.data.ScoringDataProvider;
import com.sports.modules.fitness.scoring.model.BonusLevel;
import com.sports.modules.fitness.scoring.model.Gender;
import com.sports.modules.fitness.scoring.model.GroupType;
import com.sports.modules.fitness.scoring.model.ItemMeasureType;
import com.sports.modules.fitness.scoring.model.ItemScoreResult;
import com.sports.modules.fitness.scoring.model.RawScore;
import com.sports.modules.fitness.scoring.model.RecordStatus;
import com.sports.modules.fitness.scoring.model.ScoreInput;
import com.sports.modules.fitness.scoring.model.ScoreReport;
import com.sports.modules.fitness.scoring.model.ScoringGrade;
import com.sports.modules.fitness.scoring.model.ScoringItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 体测评分引擎：把《国家学生体质健康标准（2014年修订）》大学组的评分表翻译成可执行规则。
 * <p>
 * 四条规则：
 * <ol>
 *   <li>单项得分：按测量方向把原始成绩归档到评分表某个档次；</li>
 *   <li>标准分：Σ(单项得分 × 权重)，满分 100；</li>
 *   <li>附加分：对加分项超出 100 分档的部分按规则加分，单项最多 10 分，合计最多 20 分；</li>
 *   <li>等级：按学年总分（标准分 + 附加分）评定。</li>
 * </ol>
 */
@Service
public class FitnessScoringService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal NINETY = BigDecimal.valueOf(90);
    private static final BigDecimal EIGHTY = BigDecimal.valueOf(80);
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final int MAX_BONUS_PER_ITEM = 10;

    private final ScoringDataProvider dataProvider;

    public FitnessScoringService(ScoringDataProvider dataProvider) {
        this.dataProvider = Objects.requireNonNull(dataProvider, "dataProvider");
    }

    /**
     * 单项查表：把原始成绩归档到评分档。
     *
     * @return 单项得分；低于最低档时返回 0
     */
    public int lookupScore(ScoringItem item, BigDecimal value) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(value, "value");

        if (item.getMeasureType() == ItemMeasureType.BMI_RANGE) {
            return lookupBmi(item, value);
        }

        // grades 按分数从高到低排列；高优指标阈值随之递减，低优指标阈值随之递增。
        for (ScoringGrade grade : item.getGrades()) {
            int cmp = value.compareTo(grade.getThreshold());
            boolean hit = item.getMeasureType() == ItemMeasureType.HIGH_BETTER
                    ? cmp >= 0     // 高优：达到或超过成绩线
                    : cmp <= 0;    // 低优：等于或小于成绩线
            if (hit) {
                return grade.getScore();
            }
        }
        return 0;
    }

    /**
     * BMI 区间归类：肥胖 60；低体重 80；正常 100；超重 80。
     */
    private int lookupBmi(ScoringItem item, BigDecimal bmi) {
        if (bmi.compareTo(item.getBmiObeseMin()) >= 0) {
            return 60;
        }
        if (bmi.compareTo(item.getBmiNormalMin()) < 0) {
            return 80;
        }
        if (bmi.compareTo(item.getBmiNormalMax()) <= 0) {
            return 100;
        }
        return 80; // 超重区间 (normalMax, obeseMin)
    }

    /**
     * 附加分：计算某加分项超过 100 分档后可获得的加分，最多 10 分。
     * 对低优指标，成绩越快"超出量"越大，故用 threshold - value。
     */
    public int calculateBonus(ScoringItem item, BigDecimal value) {
        if (!item.isBonusItem() || item.getBonusRule() == null) {
            return 0;
        }
        BigDecimal hundredThreshold = item.getHundredThreshold();
        BigDecimal over = item.getMeasureType() == ItemMeasureType.LOW_BETTER
                ? hundredThreshold.subtract(value)
                : value.subtract(hundredThreshold);
        if (over.signum() <= 0) {
            return 0;
        }
        int best = 0;
        for (BonusLevel level : item.getBonusRule().getLevels()) {
            if (over.compareTo(level.getOverThreshold()) >= 0) {
                best = Math.max(best, level.getPoints());
            }
        }
        return Math.min(best, MAX_BONUS_PER_ITEM);
    }

    /**
     * 等级评定：90.0 及以上优秀；80.0~89.9 良好；60.0~79.9 及格；59.9 及以下不及格。
     */
    public String judgeLevel(BigDecimal totalScore) {
        Objects.requireNonNull(totalScore, "totalScore");
        if (totalScore.compareTo(NINETY) >= 0) {
            return "优秀";
        }
        if (totalScore.compareTo(EIGHTY) >= 0) {
            return "良好";
        }
        if (totalScore.compareTo(SIXTY) >= 0) {
            return "及格";
        }
        return "不及格";
    }

    /**
     * 带记录状态的评分入口：教师/管理员已判定无效的记录，直接拒绝评分。
     * <p>
     * 该守卫必须放在查表、加权、等级判定之前，避免已作废的成绩被算出分数后进入档案。
     */
    public ScoreReport calculate(ScoreInput input, RecordStatus status) {
        if (status == RecordStatus.INVALID) {
            throw new InvalidRecordException("该体测记录已判定无效，不参与评分");
        }
        return calculate(input);
    }

    /**
     * 计算一名学生一个学年的完整体测成绩。
     */
    public ScoreReport calculate(ScoreInput input) {
        if (input == null || input.getGender() == null || input.getGroupType() == null) {
            throw new IllegalArgumentException("性别（gender）与评价组别（groupType）不能为空");
        }
        Gender gender = input.getGender();
        GroupType groupType = input.getGroupType();
        List<ScoringItem> items = dataProvider.findItems(gender, groupType);

        Map<String, BigDecimal> rawScores = collectRawScores(input.getRawScores());
        validateKnownItems(gender, groupType, rawScores);

        List<ItemScoreResult> details = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        BigDecimal standardScore = BigDecimal.ZERO;
        int bonusScore = 0;

        for (ScoringItem item : items) {
            BigDecimal raw = rawScores.get(item.getCode());
            if (raw == null) {
                missing.add(item.getCode());
                continue;
            }
            int score = lookupScore(item, raw);
            BigDecimal weighted = BigDecimal.valueOf(score)
                    .multiply(item.getWeight())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            standardScore = standardScore.add(weighted);

            int bonus = calculateBonus(item, raw);
            bonusScore += bonus;

            details.add(new ItemScoreResult(item.getCode(), item.getName(), item.getUnit(),
                    raw, score, item.getWeight(), weighted, bonus));
        }

        BigDecimal totalScore = standardScore.add(BigDecimal.valueOf(bonusScore));
        return new ScoreReport(details, standardScore, bonusScore, totalScore,
                judgeLevel(totalScore), missing);
    }

    private Map<String, BigDecimal> collectRawScores(List<RawScore> rawScores) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (rawScores == null) {
            return map;
        }
        for (RawScore raw : rawScores) {
            if (raw == null || raw.getItemCode() == null || raw.getValue() == null) {
                throw new IllegalArgumentException("原始成绩的项目编码与成绩值不能为空");
            }
            if (map.putIfAbsent(raw.getItemCode(), raw.getValue()) != null) {
                throw new IllegalArgumentException("原始成绩存在重复项目: " + raw.getItemCode());
            }
            validateValueRange(raw.getItemCode(), raw.getValue());
        }
        return map;
    }

    /**
     * 成绩合理性校验：拦截明显录错单位或符号的数值（例如 50 米跑填成 92 秒、引体向上填 -3 次）。
     * 范围可按学校实际录入口径调整；不在此表内的项目只做非空校验。
     */
    private void validateValueRange(String code, BigDecimal value) {
        BigDecimal min;
        BigDecimal max;
        switch (code) {
            case "BMI" -> { min = bd("5"); max = bd("60"); }
            case "VITAL_CAPACITY" -> { min = bd("100"); max = bd("10000"); }
            case "RUN_50M" -> { min = bd("3"); max = bd("30"); }
            case "SIT_REACH" -> { min = bd("-30"); max = bd("50"); }
            case "LONG_JUMP" -> { min = bd("20"); max = bd("400"); }
            case "PULL_UP", "SIT_UP" -> { min = bd("0"); max = bd("120"); }
            case "RUN_1000M" -> { min = bd("100"); max = bd("1200"); }
            case "RUN_800M" -> { min = bd("90"); max = bd("1200"); }
            default -> {
                return;
            }
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException("项目 " + code + " 的成绩 " + value.toPlainString()
                    + " 超出合理范围 [" + min.toPlainString() + ", " + max.toPlainString()
                    + "]，请检查录入单位与数值");
        }
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private void validateKnownItems(Gender gender, GroupType groupType, Map<String, BigDecimal> rawScores) {
        for (String code : rawScores.keySet()) {
            if (dataProvider.findItem(gender, groupType, code) == null) {
                throw new IllegalArgumentException("未知体测项目: " + code);
            }
        }
    }
}
