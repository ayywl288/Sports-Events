package com.sports.modules.fitness.scoring.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 附加分档：原始成绩超过 100 分档达到 overThreshold 时，可加 points 分。
 * <p>
 * 注意国家标准中附加分并非全部线性：女生仰卧起坐、男生 1000 米、女生 800 米
 * 的加分档位存在跳档，因此用"超出的量 → 加分"的查表方式表达，不能用单一公式。
 */
public class BonusLevel {

    private final BigDecimal overThreshold;
    private final int points;

    public BonusLevel(BigDecimal overThreshold, int points) {
        this.overThreshold = Objects.requireNonNull(overThreshold, "overThreshold");
        this.points = points;
    }

    public BigDecimal getOverThreshold() {
        return overThreshold;
    }

    public int getPoints() {
        return points;
    }

    @Override
    public String toString() {
        return "over" + overThreshold + "=" + points;
    }
}
