package com.sports.modules.event.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 赛事 VO
 *
 * @author sports
 */
@Data
public class EventVO {

    /**
     * 赛事ID
     */
    private Long id;

    /**
     * 赛事名称
     */
    private String name;

    /**
     * 赛事描述
     */
    private String description;

    /**
     * 赛事类型
     */
    private String type;

    /**
     * 赛事类型名称
     */
    private String typeName;

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
     * 状态 (1-草稿 2-报名中 3-进行中 4-已结束 5-已取消)
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 当前是否处于可报名窗口
     */
    private Boolean regOpen;

    /**
     * 报名人数
     */
    private Integer registrationCount;

    /**
     * 项目数量
     */
    private Integer sportItemCount;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}