package com.sports.modules.fitness.scoring.model;

/**
 * 评价组别。按《国家学生体质健康标准（2014年修订）》，大学一、二年级为一组，
 * 三、四年级为一组，两组使用不同的评分线。
 */
public enum GroupType {
    UNIV_1_2("大一大二"),
    UNIV_3_4("大三大四");

    private final String label;

    GroupType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
