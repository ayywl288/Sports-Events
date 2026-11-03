package com.sports.modules.result.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 比赛成绩实体（对应 result 表）
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("result")
public class CompetitionResult extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 成绩ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 赛事ID
     */
    @TableField("event_id")
    private Long eventId;

    /**
     * 运动项目ID
     */
    @TableField("sport_item_id")
    private Long sportItemId;

    /**
     * 赛程ID
     */
    @TableField("schedule_id")
    private Long scheduleId;

    /**
     * 报名ID（成绩与报名一一对应）
     */
    @TableField("registration_id")
    private Long registrationId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 成绩值
     */
    private BigDecimal score;

    /**
     * 成绩文本（如: 10.25秒）
     */
    @TableField("score_text")
    private String scoreText;

    /**
     * 名次
     */
    private Integer ranking;

    /**
     * 积分
     */
    private Integer points;

    /**
     * 录入人ID
     */
    @TableField("record_by")
    private Long recordBy;

    /**
     * 录入时间
     */
    @TableField("record_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    /**
     * 备注
     */
    private String remark;
}
