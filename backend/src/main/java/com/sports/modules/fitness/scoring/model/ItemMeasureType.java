package com.sports.modules.fitness.scoring.model;

/**
 * 单项指标的测量类型，决定"原始成绩 → 单项得分"的归档方向。
 */
public enum ItemMeasureType {
    /** 高优指标：数值越大越好（肺活量、立定跳远、坐位体前屈、引体向上、仰卧起坐）。 */
    HIGH_BETTER,
    /** 低优指标：数值越小越好（50米跑、1000米/800米跑）。 */
    LOW_BETTER,
    /** 体重指数（BMI）：按区间归类，不是单调阈值。 */
    BMI_RANGE
}
