package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.mapper.CompetitionResultMapper;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.mapper.ScheduleMapper;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 赛程 Service 实现
 * 状态说明：1-未开始 2-进行中 3-已结束
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    private static final int STATUS_PENDING = 1;
    private static final int STATUS_ONGOING = 2;
    private static final int STATUS_COMPLETED = 3;

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final CompetitionResultMapper competitionResultMapper;

    @Override
    public IPage<ScheduleVO> pageSchedulesVO(Integer current, Integer size, Long eventId, String matchDate) {
        IPage<Schedule> schedulePage = pageSchedules(current, size, eventId, matchDate);
        return convertToVOPage(schedulePage);
    }

    /**
     * 批量转换，避免 N+1 查询
     */
    private IPage<ScheduleVO> convertToVOPage(IPage<Schedule> page) {
        List<Schedule> records = page.getRecords();
        Page<ScheduleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (records.isEmpty()) {
            voPage.setRecords(List.of());
            return voPage;
        }

        Set<Long> eventIds = records.stream().map(Schedule::getEventId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> itemIds = records.stream().map(Schedule::getSportItemId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> eventNameMap = eventIds.isEmpty() ? Collections.emptyMap()
                : eventService.listByIds(eventIds).stream().collect(Collectors.toMap(Event::getId, Event::getName));
        Map<Long, String> itemNameMap = itemIds.isEmpty() ? Collections.emptyMap()
                : sportItemService.listByIds(itemIds).stream().collect(Collectors.toMap(SportItem::getId, SportItem::getName));

        List<ScheduleVO> voList = records.stream().map(schedule -> {
            ScheduleVO vo = new ScheduleVO();
            BeanUtils.copyProperties(schedule, vo);
            vo.setEventName(eventNameMap.get(schedule.getEventId()));
            vo.setSportItemName(itemNameMap.get(schedule.getSportItemId()));
            vo.setStatusName(getStatusName(schedule.getStatus()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public IPage<Schedule> pageSchedules(Integer current, Integer size, Long eventId, String matchDate) {
        Page<Schedule> page = new Page<>(current, size);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, Schedule::getEventId, eventId)
                .eq(StringUtils.hasText(matchDate), Schedule::getMatchDate, parseDate(matchDate))
                .orderByAsc(Schedule::getMatchDate)
                .orderByAsc(Schedule::getStartTime);

        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(Schedule schedule) {
        validate(schedule, null);
        if (schedule.getStatus() == null) {
            schedule.setStatus(STATUS_PENDING);
        }
        this.save(schedule);
        return schedule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSchedule(Long id, Schedule schedule) {
        Schedule existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        if (Objects.equals(existing.getStatus(), STATUS_COMPLETED)) {
            throw new BusinessException("已结束的赛程不能修改");
        }

        validate(schedule, id);

        // 状态不允许通过普通编辑篡改，只能走开始/完成接口
        schedule.setStatus(existing.getStatus());
        schedule.setId(id);
        this.updateById(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        Schedule existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        if (Objects.equals(existing.getStatus(), STATUS_ONGOING)) {
            throw new BusinessException("进行中的赛程不能删除");
        }
        long resultCount = competitionResultMapper.selectCount(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getScheduleId, id));
        if (resultCount > 0) {
            throw new BusinessException("该赛程下已录入成绩，不能删除");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startMatch(Long id) {
        Schedule schedule = this.getById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        if (!Objects.equals(schedule.getStatus(), STATUS_PENDING)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有未开始的赛程才能开始");
        }
        schedule.setStatus(STATUS_ONGOING);
        this.updateById(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeMatch(Long id) {
        Schedule schedule = this.getById(id);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        if (!Objects.equals(schedule.getStatus(), STATUS_ONGOING)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只有进行中的赛程才能完成");
        }
        schedule.setStatus(STATUS_COMPLETED);
        this.updateById(schedule);
    }

    /**
     * 赛程业务校验
     *
     * @param schedule 赛程信息
     * @param excludeId 更新时排除自身
     */
    private void validate(Schedule schedule, Long excludeId) {
        if (schedule.getEventId() == null || schedule.getSportItemId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "所属赛事与运动项目不能为空");
        }
        if (schedule.getMatchDate() == null || schedule.getStartTime() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "比赛日期与开始时间不能为空");
        }

        Event event = eventService.getById(schedule.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (Objects.equals(event.getStatus(), 1)) {
            throw new BusinessException("草稿状态的赛事不能安排赛程，请先发布赛事");
        }
        if (Objects.equals(event.getStatus(), 5)) {
            throw new BusinessException("已取消的赛事不能安排赛程");
        }

        SportItem item = sportItemService.getById(schedule.getSportItemId());
        if (item == null || !schedule.getEventId().equals(item.getEventId())) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND, "运动项目不属于该赛事");
        }

        // 日期必须落在赛事区间内
        if (event.getStartDate() != null && schedule.getMatchDate().isBefore(event.getStartDate())) {
            throw new BusinessException("比赛日期不能早于赛事开始日期");
        }
        if (event.getEndDate() != null && schedule.getMatchDate().isAfter(event.getEndDate())) {
            throw new BusinessException("比赛日期不能晚于赛事结束日期");
        }

        // 时间先后顺序
        LocalTime start = schedule.getStartTime();
        LocalTime end = schedule.getEndTime();
        if (end != null && !end.isAfter(start)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        // 场地冲突：同一天、同场地、时间段重叠
        if (StringUtils.hasText(schedule.getLocation())) {
            Schedule conflict = findVenueConflict(schedule, excludeId);
            if (conflict != null) {
                throw new BusinessException(ResultCode.SCHEDULE_VENUE_CONFLICT,
                        "场地时间冲突：与「" + conflict.getScheduleName() + "」("
                                + conflict.getStartTime() + "-" + conflict.getEndTime() + ") 重叠");
            }
        }

        // 同项目同轮次同组别重复
        long duplicate = this.count(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getEventId, schedule.getEventId())
                .eq(Schedule::getSportItemId, schedule.getSportItemId())
                .eq(schedule.getRoundNo() != null, Schedule::getRoundNo, schedule.getRoundNo())
                .eq(StringUtils.hasText(schedule.getGroupNo()), Schedule::getGroupNo, schedule.getGroupNo())
                .ne(excludeId != null, Schedule::getId, excludeId));
        if (duplicate > 0 && schedule.getRoundNo() != null) {
            throw new BusinessException(ResultCode.SCHEDULE_CONFLICT, "同一项目相同轮次与组别的赛程已存在");
        }
    }

    private Schedule findVenueConflict(Schedule schedule, Long excludeId) {
        List<Schedule> sameDay = this.list(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getMatchDate, schedule.getMatchDate())
                .eq(Schedule::getLocation, schedule.getLocation())
                .ne(excludeId != null, Schedule::getId, excludeId));

        LocalTime newStart = schedule.getStartTime();
        LocalTime newEnd = schedule.getEndTime() != null ? schedule.getEndTime() : schedule.getStartTime();

        for (Schedule other : sameDay) {
            LocalTime s = other.getStartTime();
            LocalTime e = other.getEndTime() != null ? other.getEndTime() : other.getStartTime();
            if (s == null) {
                continue;
            }
            // 半开区间重叠判断
            if (newStart.isBefore(e) && s.isBefore(newEnd)) {
                return other;
            }
        }
        return null;
    }

    private LocalDate parseDate(String matchDate) {
        if (!StringUtils.hasText(matchDate)) {
            return null;
        }
        try {
            return LocalDate.parse(matchDate);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_TYPE_ERROR, "比赛日期格式应为 yyyy-MM-dd");
        }
    }

    public static String getStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 1 -> "未开始";
            case 2 -> "进行中";
            case 3 -> "已结束";
            default -> String.valueOf(status);
        };
    }
}
