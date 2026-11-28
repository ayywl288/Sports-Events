package com.sports.modules.fitness.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 体测录入页的学生下拉选项。
 */
@Data
@AllArgsConstructor
public class FitnessStudentVO {
    private Long id;
    private String name;
    private String studentNo;
    /** 1-男 2-女 0/空-未知，用于自动带出性别。 */
    private Integer gender;
    private String className;
}
