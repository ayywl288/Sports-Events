package com.sports.modules.schedule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.common.result.ResultCode;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.service.impl.ScheduleServiceImpl;
import com.sports.modules.schedule.vo.ScheduleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 赛程 Controller
 *
 * @author sports
 */
@Tag(name = "赛程管理", description = "赛程的增删改查")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "分页查询赛程")
    @GetMapping
    public Result<PageResult<ScheduleVO>> pageSchedules(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String matchDate) {
        return Result.success(PageResult.of(scheduleService.pageSchedulesVO(current, size, eventId, matchDate)));
    }

    @Operation(summary = "获取赛程详情")
    @GetMapping("/{id}")
    public Result<ScheduleVO> getById(@PathVariable Long id) {
        Schedule schedule = scheduleService.getById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        ScheduleVO vo = new ScheduleVO();
        org.springframework.beans.BeanUtils.copyProperties(schedule, vo);
        vo.setStatusName(ScheduleServiceImpl.getStatusName(schedule.getStatus()));
        return Result.success(vo);
    }

    @Operation(summary = "创建赛程")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@RequestBody Schedule schedule) {
        return Result.success("创建成功", scheduleService.createSchedule(schedule));
    }

    @Operation(summary = "更新赛程")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Schedule schedule) {
        scheduleService.updateSchedule(id, schedule);
        return Result.success();
    }

    @Operation(summary = "删除赛程")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return Result.success();
    }

    @Operation(summary = "开始比赛")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> startMatch(@PathVariable Long id) {
        scheduleService.startMatch(id);
        return Result.success();
    }

    @Operation(summary = "完成比赛")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> completeMatch(@PathVariable Long id) {
        scheduleService.completeMatch(id);
        return Result.success();
    }
}
