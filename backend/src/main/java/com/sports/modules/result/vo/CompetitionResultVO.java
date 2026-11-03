package com.sports.modules.result.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 比赛成绩 VO
 *
 * @author sports
 */
@Data
public class CompetitionResultVO {

    private Long id;

    /**
     * 赛事ID
     */
    private Long eventId;

    /**
     * 赛事名称
     */
    private String eventName;

    /**
     * 运动项目ID
     */
    private Long sportItemId;

    /**
     * 运动项目名称
     */
    private String sportItemName;

    /**
     * 报名ID
     */
    private Long registrationId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学院
     */
    private String college;

    /**
     * 成绩值
     */
    private BigDecimal score;

    /**
     * 成绩文本
     */
    private String scoreText;

    /**
     * 成绩单位（取自运动项目配置）
     */
    private String unit;

    /**
     * 名次
     */
    private Integer ranking;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 备注
     */
    private String remark;

    /**
     * 录入人ID
     */
    private Long recordBy;

    /**
     * 录入人姓名
     */
    private String recorderName;

    /**
     * 录入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
