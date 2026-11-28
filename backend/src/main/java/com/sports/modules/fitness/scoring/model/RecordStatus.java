package com.sports.modules.fitness.scoring.model;

/**
 * 体测记录状态。
 * <p>
 * 教师/管理员可把明显异常的成绩判定为无效；无效记录不参与评分，
 * 也不出现在学生端的成绩查询结果中。
 */
public enum RecordStatus {
    VALID("有效"),
    INVALID("无效");

    private final String label;

    RecordStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
