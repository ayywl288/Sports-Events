package com.sports.modules.sportitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 运动项目实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sport_item")
public class SportItem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属赛事ID
     */
    @TableField("event_id")
    private Long eventId;

    /**
     * 项目名称
     */
    @TableField("item_name")
    private String name;

    /**
     * 项目类型（TRACK-径赛 FIELD-田赛 BALL-球类 SWIMMING-游泳 OTHER-其他）
     */
    @TableField("item_type")
    private String type;

    /**
     * 性别限制（0-不限 1-仅男 2-仅女）
     */
    @TableField("gender_limit")
    private Integer genderLimit;

    /**
     * 最大报名人数
     */
    @TableField("max_participants")
    private Integer maxParticipants;

    /**
     * 当前报名人数
     */
    @TableField("current_participants")
    private Integer currentParticipants;

    /**
     * 比赛规则说明
     */
    private String rules;

    /**
     * 成绩类型（TIME-计时 DISTANCE-计距离 SCORE-计分）
     */
    @TableField("result_type")
    private String scoreType;

    /**
     * 成绩单位
     */
    @TableField("result_unit")
    private String scoreUnit;

    /**
     * 排序规则（ASC-升序(时间) DESC-降序(分数/距离)）
     */
    @TableField("sort_rule")
    private String sortRule;

    /**
     * 状态（0-禁用 1-启用）
     */
    private Integer status;

    /**
     * 所属赛事名称（非数据库字段，列表展示用）
     */
    @TableField(exist = false)
    private String eventName;
}