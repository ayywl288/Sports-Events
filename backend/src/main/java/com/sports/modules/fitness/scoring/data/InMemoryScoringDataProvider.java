package com.sports.modules.fitness.scoring.data;

import com.sports.modules.fitness.scoring.model.BonusLevel;
import com.sports.modules.fitness.scoring.model.BonusRule;
import com.sports.modules.fitness.scoring.model.Gender;
import com.sports.modules.fitness.scoring.model.GroupType;
import com.sports.modules.fitness.scoring.model.ItemMeasureType;
import com.sports.modules.fitness.scoring.model.ScoringGrade;
import com.sports.modules.fitness.scoring.model.ScoringItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存版评分数据源：内置《国家学生体质健康标准（2014年修订）》大学组评分表。
 * <p>
 * 数据口径说明：
 * <ul>
 *   <li>体重指数（BMI）：正常 100 分，低体重 80 分，超重 80 分，肥胖 60 分。
 *       国家标准评分表中"超重"档的分值栏在部分版本中未印出，实践中普遍按 80 分执行；
 *       若学校教务处另有口径，只需修改 {@link #bmi} 的调用参数即可。</li>
 *   <li>各项目权重：BMI 15%、肺活量 15%、50 米跑 20%、坐位体前屈 10%、
 *       立定跳远 10%、引体向上/仰卧起坐 10%、1000 米/800 米跑 20%，合计 100%。</li>
 *   <li>耐力跑成绩以"秒"存储，便于比较；录入时建议统一转换为秒。</li>
 *   <li>附加分为"超出 100 分档的程度 → 加分"的查表，最多 10 分/项。</li>
 * </ul>
 */
@Component
public class InMemoryScoringDataProvider implements ScoringDataProvider {

    /** 常规 20 档分数（100 分到 10 分）。 */
    private static final int[] S20 = {
            100, 95, 90, 85, 80, 78, 76, 74, 72, 70, 68, 66, 64, 62, 60, 50, 40, 30, 20, 10
    };

    /** 引体向上只有 15 档（无 78/74/70/66/62 档）。 */
    private static final int[] S_PULL_UP = {
            100, 95, 90, 85, 80, 76, 72, 68, 64, 60, 50, 40, 30, 20, 10
    };

    private final Map<String, List<ScoringItem>> groups = new HashMap<>();

    public InMemoryScoringDataProvider() {
        groups.put(key(Gender.MALE, GroupType.UNIV_1_2), maleGroup12());
        groups.put(key(Gender.MALE, GroupType.UNIV_3_4), maleGroup34());
        groups.put(key(Gender.FEMALE, GroupType.UNIV_1_2), femaleGroup12());
        groups.put(key(Gender.FEMALE, GroupType.UNIV_3_4), femaleGroup34());
    }

    @Override
    public ScoringItem findItem(Gender gender, GroupType groupType, String itemCode) {
        List<ScoringItem> items = findItems(gender, groupType);
        for (ScoringItem item : items) {
            if (item.getCode().equals(itemCode)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public List<ScoringItem> findItems(Gender gender, GroupType groupType) {
        List<ScoringItem> items = groups.get(key(gender, groupType));
        if (items == null) {
            throw new IllegalArgumentException("未配置评分标准: " + gender + "/" + groupType);
        }
        return items;
    }

    // ------------------------------------------------------------------
    // 男生 大一大二
    // ------------------------------------------------------------------
    private static List<ScoringItem> maleGroup12() {
        return List.of(
                bmi(17.9, 23.9, 28.0),
                item("VITAL_CAPACITY", "肺活量", "毫升", 15, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                5040, 4920, 4800, 4550, 4300, 4180, 4060, 3940, 3820, 3700,
                                3580, 3460, 3340, 3220, 3100, 2940, 2780, 2620, 2460, 2300
                        }), null),
                item("RUN_50M", "50米跑", "秒", 20, ItemMeasureType.LOW_BETTER, false,
                        grades(S20, new double[]{
                                6.7, 6.8, 6.9, 7.0, 7.1, 7.3, 7.5, 7.7, 7.9, 8.1,
                                8.3, 8.5, 8.7, 8.9, 9.1, 9.3, 9.5, 9.7, 9.9, 10.1
                        }), null),
                item("SIT_REACH", "坐位体前屈", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                24.9, 23.1, 21.3, 19.5, 17.7, 16.3, 14.9, 13.5, 12.1, 10.7,
                                9.3, 7.9, 6.5, 5.1, 3.7, 2.7, 1.7, 0.7, -0.3, -1.3
                        }), null),
                item("LONG_JUMP", "立定跳远", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                273, 268, 263, 256, 248, 244, 240, 236, 232, 228,
                                224, 220, 216, 212, 208, 203, 198, 193, 188, 183
                        }), null),
                item("PULL_UP", "引体向上", "次", 10, ItemMeasureType.HIGH_BETTER, true,
                        grades(S_PULL_UP, new double[]{
                                19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5
                        }), pullUpBonus()),
                item("RUN_1000M", "1000米跑", "秒", 20, ItemMeasureType.LOW_BETTER, true,
                        grades(S20, new double[]{
                                197, 202, 207, 214, 222, 227, 232, 237, 242, 247,
                                252, 257, 262, 267, 272, 292, 312, 332, 352, 372
                        }), run1000Bonus())
        );
    }

    // ------------------------------------------------------------------
    // 男生 大三大四
    // ------------------------------------------------------------------
    private static List<ScoringItem> maleGroup34() {
        return List.of(
                bmi(17.9, 23.9, 28.0),
                item("VITAL_CAPACITY", "肺活量", "毫升", 15, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                5140, 5020, 4900, 4650, 4400, 4280, 4160, 4040, 3920, 3800,
                                3680, 3560, 3440, 3320, 3200, 3030, 2860, 2690, 2520, 2350
                        }), null),
                item("RUN_50M", "50米跑", "秒", 20, ItemMeasureType.LOW_BETTER, false,
                        grades(S20, new double[]{
                                6.6, 6.7, 6.8, 6.9, 7.0, 7.2, 7.4, 7.6, 7.8, 8.0,
                                8.2, 8.4, 8.6, 8.8, 9.0, 9.2, 9.4, 9.6, 9.8, 10.0
                        }), null),
                item("SIT_REACH", "坐位体前屈", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                25.1, 23.3, 21.5, 19.9, 18.2, 16.8, 15.4, 14.0, 12.6, 11.2,
                                9.8, 8.4, 7.0, 5.6, 4.2, 3.2, 2.2, 1.2, 0.2, -0.8
                        }), null),
                item("LONG_JUMP", "立定跳远", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                275, 270, 265, 258, 250, 246, 242, 238, 234, 230,
                                226, 222, 218, 214, 210, 205, 200, 195, 190, 185
                        }), null),
                item("PULL_UP", "引体向上", "次", 10, ItemMeasureType.HIGH_BETTER, true,
                        grades(S_PULL_UP, new double[]{
                                20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6
                        }), pullUpBonus()),
                item("RUN_1000M", "1000米跑", "秒", 20, ItemMeasureType.LOW_BETTER, true,
                        grades(S20, new double[]{
                                195, 200, 205, 212, 220, 225, 230, 235, 240, 245,
                                250, 255, 260, 265, 270, 290, 310, 330, 350, 370
                        }), run1000Bonus())
        );
    }

    // ------------------------------------------------------------------
    // 女生 大一大二
    // ------------------------------------------------------------------
    private static List<ScoringItem> femaleGroup12() {
        return List.of(
                bmi(17.2, 23.9, 28.0),
                item("VITAL_CAPACITY", "肺活量", "毫升", 15, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                3400, 3350, 3300, 3150, 3000, 2900, 2800, 2700, 2600, 2500,
                                2400, 2300, 2200, 2100, 2000, 1960, 1920, 1880, 1840, 1800
                        }), null),
                item("RUN_50M", "50米跑", "秒", 20, ItemMeasureType.LOW_BETTER, false,
                        grades(S20, new double[]{
                                7.5, 7.6, 7.7, 8.0, 8.3, 8.5, 8.7, 8.9, 9.1, 9.3,
                                9.5, 9.7, 9.9, 10.1, 10.3, 10.5, 10.7, 10.9, 11.1, 11.3
                        }), null),
                item("SIT_REACH", "坐位体前屈", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                25.8, 24.0, 22.2, 20.6, 19.0, 17.7, 16.4, 15.1, 13.8, 12.5,
                                11.2, 9.9, 8.6, 7.3, 6.0, 5.2, 4.4, 3.6, 2.8, 2.0
                        }), null),
                item("LONG_JUMP", "立定跳远", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                207, 201, 195, 188, 181, 178, 175, 172, 169, 166,
                                163, 160, 157, 154, 151, 146, 141, 136, 131, 126
                        }), null),
                item("SIT_UP", "1分钟仰卧起坐", "次", 10, ItemMeasureType.HIGH_BETTER, true,
                        grades(S20, new double[]{
                                56, 54, 52, 49, 46, 44, 42, 40, 38, 36,
                                34, 32, 30, 28, 26, 24, 22, 20, 18, 16
                        }), sitUpBonus()),
                item("RUN_800M", "800米跑", "秒", 20, ItemMeasureType.LOW_BETTER, true,
                        grades(S20, new double[]{
                                198, 204, 210, 217, 224, 229, 234, 239, 244, 249,
                                254, 259, 264, 269, 274, 284, 294, 304, 314, 324
                        }), run800Bonus())
        );
    }

    // ------------------------------------------------------------------
    // 女生 大三大四
    // ------------------------------------------------------------------
    private static List<ScoringItem> femaleGroup34() {
        return List.of(
                bmi(17.2, 23.9, 28.0),
                item("VITAL_CAPACITY", "肺活量", "毫升", 15, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                3450, 3400, 3350, 3200, 3050, 2950, 2850, 2750, 2650, 2550,
                                2450, 2350, 2250, 2150, 2050, 2010, 1970, 1930, 1890, 1850
                        }), null),
                item("RUN_50M", "50米跑", "秒", 20, ItemMeasureType.LOW_BETTER, false,
                        grades(S20, new double[]{
                                7.4, 7.5, 7.6, 7.9, 8.2, 8.4, 8.6, 8.8, 9.0, 9.2,
                                9.4, 9.6, 9.8, 10.0, 10.2, 10.4, 10.6, 10.8, 11.0, 11.2
                        }), null),
                item("SIT_REACH", "坐位体前屈", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                26.3, 24.4, 22.4, 21.0, 19.5, 18.2, 16.9, 15.6, 14.3, 13.0,
                                11.7, 10.4, 9.1, 7.8, 6.5, 5.7, 4.9, 4.1, 3.3, 2.5
                        }), null),
                item("LONG_JUMP", "立定跳远", "厘米", 10, ItemMeasureType.HIGH_BETTER, false,
                        grades(S20, new double[]{
                                208, 202, 196, 189, 182, 179, 176, 173, 170, 167,
                                164, 161, 158, 155, 152, 147, 142, 137, 132, 127
                        }), null),
                item("SIT_UP", "1分钟仰卧起坐", "次", 10, ItemMeasureType.HIGH_BETTER, true,
                        grades(S20, new double[]{
                                57, 55, 53, 50, 47, 45, 43, 41, 39, 37,
                                35, 33, 31, 29, 27, 25, 23, 21, 19, 17
                        }), sitUpBonus()),
                item("RUN_800M", "800米跑", "秒", 20, ItemMeasureType.LOW_BETTER, true,
                        grades(S20, new double[]{
                                196, 202, 208, 215, 222, 227, 232, 237, 242, 247,
                                252, 257, 262, 267, 272, 282, 292, 302, 312, 322
                        }), run800Bonus())
        );
    }

    // ------------------------------------------------------------------
    // 构造辅助
    // ------------------------------------------------------------------

    private static ScoringItem item(String code, String name, String unit, int weight,
                                    ItemMeasureType type, boolean bonus,
                                    List<ScoringGrade> grades, BonusRule rule) {
        return new ScoringItem(code, name, unit, BigDecimal.valueOf(weight), type, bonus,
                grades, rule, null, null, null);
    }

    private static ScoringItem bmi(double normalMin, double normalMax, double obeseMin) {
        return new ScoringItem("BMI", "体重指数(BMI)", "千克/米²", BigDecimal.valueOf(15),
                ItemMeasureType.BMI_RANGE, false, List.of(), null,
                BigDecimal.valueOf(normalMin), BigDecimal.valueOf(normalMax), BigDecimal.valueOf(obeseMin));
    }

    private static List<ScoringGrade> grades(int[] scores, double[] thresholds) {
        if (scores.length != thresholds.length) {
            throw new IllegalArgumentException("评分档位与成绩线数量不一致");
        }
        List<ScoringGrade> list = new ArrayList<>(scores.length);
        for (int i = 0; i < scores.length; i++) {
            list.add(new ScoringGrade(scores[i], BigDecimal.valueOf(thresholds[i])));
        }
        return list;
    }

    private static BonusRule bonusRule(int[][] levels) {
        List<BonusLevel> list = new ArrayList<>(levels.length);
        for (int[] level : levels) {
            list.add(new BonusLevel(BigDecimal.valueOf(level[0]), level[1]));
        }
        return new BonusRule(list);
    }

    /** 引体向上：超过 100 分档后，每多做 1 次加 1 分，最多 10 分。 */
    private static BonusRule pullUpBonus() {
        return bonusRule(new int[][]{
                {1, 1}, {2, 2}, {3, 3}, {4, 4}, {5, 5},
                {6, 6}, {7, 7}, {8, 8}, {9, 9}, {10, 10}
        });
    }

    /** 仰卧起坐：超过 100 分档 2 次加 1 分、4 次加 2 分、6 次加 3 分，之后每多 1 次加 1 分，最多 10 分。 */
    private static BonusRule sitUpBonus() {
        return bonusRule(new int[][]{
                {2, 1}, {4, 2}, {6, 3}, {7, 4}, {8, 5},
                {9, 6}, {10, 7}, {11, 8}, {12, 9}, {13, 10}
        });
    }

    /** 1000米：比 100 分档每快 4 秒加 1 分，20 秒后跳档，最多 10 分。 */
    private static BonusRule run1000Bonus() {
        return bonusRule(new int[][]{
                {4, 1}, {8, 2}, {12, 3}, {16, 4}, {20, 5},
                {23, 6}, {26, 7}, {29, 8}, {32, 9}, {35, 10}
        });
    }

    /** 800米：比 100 分档每快 5 秒加 1 分，最多 10 分。 */
    private static BonusRule run800Bonus() {
        return bonusRule(new int[][]{
                {5, 1}, {10, 2}, {15, 3}, {20, 4}, {25, 5},
                {30, 6}, {35, 7}, {40, 8}, {45, 9}, {50, 10}
        });
    }

    private static String key(Gender gender, GroupType groupType) {
        return gender.name() + ":" + groupType.name();
    }
}
