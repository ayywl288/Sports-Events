package com.sports.modules.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 赛事实体
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event")
public class Event extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 赛事ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 赛事名称
     */
    @TableField("event_name")
    private String name;

    /**
     * 赛事描述
     */
    private String description;

    /**
     * 赛事类型（TRACK_FIELD-田径 BALL-球类 SWIMMING-游泳 OTHER-其他）
     */
    @TableField("event_type")
    private String type;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 报名开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate regStartDate;

    /**
     * 报名截止日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate regEndDate;

    /**
     * 举办地点
     */
    private String location;

    /**
     * 主办单位
     */
    private String organizer;

    /**
     * 联系人
     */
    @TableField("contact_person")
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 封面图片URL
     */
    private String coverImage;

    /**
     * 状态（1-草稿 2-报名中 3-进行中 4-已结束 5-已取消）
     */
    private Integer status;

    /**
     * 创建人ID
     */
    @TableField("create_by")
    private Long creatorId;
}