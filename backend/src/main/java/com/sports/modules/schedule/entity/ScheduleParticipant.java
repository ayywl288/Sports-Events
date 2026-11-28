package com.sports.modules.schedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.sports.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 赛程参赛人员实体（选手与具体场次的分配关系）
 *
 * @author sports
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_participant")
public class ScheduleParticipant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 赛程ID
     */
    @TableField("schedule_id")
    private Long scheduleId;

    /**
     * 报名ID
     */
    @TableField("registration_id")
    private Long registrationId;
}
