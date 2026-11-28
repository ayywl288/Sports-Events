package com.sports.modules.fitness.scoring.model;

import java.util.List;
import java.util.Objects;

/**
 * 某个加分项的完整加分规则：按"超过 100 分档的幅度"从各档中取可获得的最高加分。
 */
public class BonusRule {

    private final List<BonusLevel> levels;

    public BonusRule(List<BonusLevel> levels) {
        this.levels = List.copyOf(Objects.requireNonNull(levels, "levels"));
    }

    public List<BonusLevel> getLevels() {
        return levels;
    }

    public int maxPoints() {
        return levels.stream().mapToInt(BonusLevel::getPoints).max().orElse(0);
    }
}
