package com.sports.modules.fitness.dto;

import com.sports.modules.fitness.scoring.model.Gender;
import com.sports.modules.fitness.scoring.model.GroupType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

/**
 * 国标体测评分录入请求。BMI 不直接传入，由身高、体重计算。
 */
@Data
public class FitnessScoreRequest {

    @NotNull(message = "请选择学生")
    private Long userId;

    @NotNull(message = "请选择测试日期")
    @PastOrPresent(message = "测试日期不能晚于今天")
    private LocalDate testDate;

    @NotNull(message = "请填写身高")
    @DecimalMin(value = "50", message = "身高应在 50~250cm")
    @DecimalMax(value = "250", message = "身高应在 50~250cm")
    private Double heightCm;

    @NotNull(message = "请填写体重")
    @DecimalMin(value = "10", message = "体重应在 10~400kg")
    @DecimalMax(value = "400", message = "体重应在 10~400kg")
    private Double weightKg;

    @DecimalMin(value = "100", message = "肺活量应在 100~10000ml")
    @DecimalMax(value = "10000", message = "肺活量应在 100~10000ml")
    private Double vitalCapacity;

    @DecimalMin(value = "3", message = "50 米成绩应在 3~30 秒")
    @DecimalMax(value = "30", message = "50 米成绩应在 3~30 秒")
    private Double run50Seconds;

    @DecimalMin(value = "-30", message = "坐位体前屈应在 -30~50cm")
    @DecimalMax(value = "50", message = "坐位体前屈应在 -30~50cm")
    private Double sitReachCm;

    @DecimalMin(value = "20", message = "立定跳远应在 20~400cm")
    @DecimalMax(value = "400", message = "立定跳远应在 20~400cm")
    private Double jumpCm;

    @Min(value = 0, message = "力量项目次数不能为负数")
    @Max(value = 120, message = "力量项目次数不能超过 120")
    private Integer strengthCount;

    @DecimalMin(value = "90", message = "耐力跑成绩应在 90~1200 秒")
    @DecimalMax(value = "1200", message = "耐力跑成绩应在 90~1200 秒")
    private Double enduranceSeconds;

    /** 未传时按 sys_user.gender 推断。 */
    private Gender gender;

    @NotNull(message = "请选择年级组别")
    private GroupType groupType;

    /** 未传时按测试日期推导，9 月及以后归入新学年。 */
    private String academicYear;
}
