package com.sports.modules.result.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成绩修改 DTO（仅允许修改成绩相关字段）
 *
 * @author sports
 */
@Data
public class ResultUpdateDTO {

    private BigDecimal score;

    @Size(max = 50, message = "成绩文本最多50个字符")
    private String scoreText;

    private Integer ranking;

    private Integer points;

    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
