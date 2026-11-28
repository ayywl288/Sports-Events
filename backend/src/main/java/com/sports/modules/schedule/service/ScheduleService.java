package com.sports.modules.schedule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.vo.ScheduleVO;

import java.util.List;

/**
 * 赛程 Service
 *
 * @author sports
 */
public interface ScheduleService extends IService<Schedule> {

    /**
     * 分页查询赛程（带关联信息）
     */
    IPage<ScheduleVO> pageSchedulesVO(Integer current, Integer size, Long eventId, String matchDate);

    /**
     * 分页查询赛程
     */
    IPage<Schedule> pageSchedules(Integer current, Integer size, Long eventId, String matchDate);

    /**
     * 创建赛程（含业务校验）
     */
    Long createSchedule(Schedule schedule);

    /**
     * 更新赛程（含业务校验）
     */
    void updateSchedule(Long id, Schedule schedule);

    /**
     * 删除赛程（存在成绩时禁止删除）
     */
    void deleteSchedule(Long id);

    /**
     * 开始比赛
     */
    void startMatch(Long id);

    /**
     * 完成比赛
     */
    void completeMatch(Long id);

    /**
     * 查询某个学生（已通过报名）的个人日程，并标记时间冲突
     */
    List<ScheduleVO> listSchedulesByUser(Long userId);

    /**
     * 预检赛程冲突（场地 / 选手 / 重复场次），返回冲突说明
     */
    List<String> checkConflicts(Schedule schedule);
}
