package com.sports.modules.schedule.dto;

import lombok.Data;

import java.util.List;

/**
 * 赛程选手安排 DTO
 *
 * @author sports
 */
@Data
public class ScheduleParticipantDTO {

    /**
     * 安排到该场次的报名ID列表
     */
    private List<Long> registrationIds;
}
