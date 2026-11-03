package com.sports.modules.schedule.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 赛程 VO
 *
 * @author sports
 */
@Data
public class ScheduleVO {

    /**
     * 赛程ID
     */
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
     * 赛程名称
     */
    private String scheduleName;

    /**
     * 轮次
     */
    private Integer roundNo;

    /**
     * 组别
     */
    private String groupNo;

    /**
     * 比赛日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate matchDate;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /**
     * 比赛场地
     */
    private String location;

    /**
     * 裁判
     */
    private String referee;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;
}
