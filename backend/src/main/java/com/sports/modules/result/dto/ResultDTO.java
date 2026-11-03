package com.sports.modules.result.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 成绩录入 DTO
 *
 * @author sports
 */
@Data
public class ResultDTO {

    /**
     * 报名ID（成绩的归属以此为准，服务端据此推导赛事/项目/参赛人）
     */
    @NotNull(message = "报名记录不能为空")
    private Long registrationId;

    /**
     * 赛程ID（可选）
     */
    private Long scheduleId;

    /**
     * 成绩值
     */
    private BigDecimal score;

    /**
     * 成绩文本
     */
    @Size(max = 50, message = "成绩文本最多50个字符")
    private String scoreText;

    /**
     * 名次（留空则按规则自动计算）
     */
    private Integer ranking;

    /**
     * 积分（留空则按名次自动计算）
     */
    private Integer points;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注最多255个字符")
    private String remark;
}
