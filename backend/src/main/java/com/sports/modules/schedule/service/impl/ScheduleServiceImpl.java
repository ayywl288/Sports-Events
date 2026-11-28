package com.sports.modules.schedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.vo.RegistrationVO;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.mapper.CompetitionResultMapper;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.entity.ScheduleParticipant;
import com.sports.modules.schedule.mapper.ScheduleMapper;
import com.sports.modules.schedule.mapper.ScheduleParticipantMapper;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.schedule.vo.ScheduleVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
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

    private static final int REGISTRATION_APPROVED = 2;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final CompetitionResultMapper competitionResultMapper;
    private final RegistrationService registrationService;
    private final SysUserService sysUserService;
    private final ScheduleParticipantMapper scheduleParticipantMapper;

    @Override
    public IPage<ScheduleVO> pageSchedulesVO(Integer current, Integer size, Long eventId, String matchDate) {
        IPage<Schedule> schedulePage = pageSchedules(current, size, eventId, matchDate);
        return convertToVOPage(schedulePage);
    }

    /**
     * 批量转换，避免 N+1 查询
     */
    private IPage<ScheduleVO> convertToVOPage(IPage<Schedule> page) {
        Page<ScheduleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(convertToVOList(page.getRecords()));
        return voPage;
    }

    private List<ScheduleVO> convertToVOList(List<Schedule> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = records.stream().map(Schedule::getEventId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> itemIds = records.stream().map(Schedule::getSportItemId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> eventNameMap = eventIds.isEmpty() ? Collections.emptyMap()
                : eventService.listByIds(eventIds).stream().collect(Collectors.toMap(Event::getId, Event::getName));
        Map<Long, String> itemNameMap = itemIds.isEmpty() ? Collections.emptyMap()
                : sportItemService.listByIds(itemIds).stream().collect(Collectors.toMap(SportItem::getId, SportItem::getName));

        return records.stream().map(schedule -> {
            ScheduleVO vo = new ScheduleVO();
            BeanUtils.copyProperties(schedule, vo);
            vo.setEventName(eventNameMap.get(schedule.getEventId()));
            vo.setSportItemName(itemNameMap.get(schedule.getSportItemId()));
            vo.setStatusName(getStatusName(schedule.getStatus()));
            return vo;
        }).collect(Collectors.toList());
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
        boolean itemChanged = !Objects.equals(existing.getEventId(), schedule.getEventId())
                || !Objects.equals(existing.getSportItemId(), schedule.getSportItemId());
        if (itemChanged && !participantRegistrationIds(id).isEmpty()) {
            throw new BusinessException("该赛程已有选手安排，不能修改所属赛事或运动项目，请先清空选手安排");
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
        scheduleParticipantMapper.deleteByScheduleId(id);
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
        if (schedule.getMatchDate() == null || schedule.getStartTime() == null || schedule.getEndTime() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "比赛日期、开始时间与结束时间不能为空");
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

        // 时间先后顺序（起止时间均必填）
        if (!schedule.getEndTime().isAfter(schedule.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        // 场地冲突 / 选手冲突 / 重复场次
        List<String> conflicts = collectConflicts(schedule, excludeId);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ResultCode.SCHEDULE_CONFLICT, String.join("；", conflicts));
        }
    }

    @Override
    public List<String> checkConflicts(Schedule schedule) {
        if (schedule.getEventId() == null || schedule.getSportItemId() == null
                || schedule.getMatchDate() == null || schedule.getStartTime() == null
                || schedule.getEndTime() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请先选择赛事、项目、比赛日期以及完整的起止时间");
        }
        if (!schedule.getEndTime().isAfter(schedule.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        return collectConflicts(schedule, schedule.getId());
    }

    /**
     * 汇总赛程冲突：场地冲突、选手冲突、重复场次
     */
    private List<String> collectConflicts(Schedule schedule, Long excludeId) {
        List<String> conflicts = new ArrayList<>();

        if (StringUtils.hasText(schedule.getLocation())) {
            Schedule venue = findVenueConflict(schedule, excludeId);
            if (venue != null) {
                conflicts.add("场地冲突：与「" + scheduleLabel(venue) + "」(" + timeRange(venue) + ") 时间重叠");
            }
        }

        conflicts.addAll(findPlayerConflicts(schedule, participantRegistrationIds(excludeId), excludeId));

        if (schedule.getRoundNo() != null) {
            long duplicate = this.count(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getEventId, schedule.getEventId())
                    .eq(Schedule::getSportItemId, schedule.getSportItemId())
                    .eq(Schedule::getRoundNo, schedule.getRoundNo())
                    .eq(StringUtils.hasText(schedule.getGroupNo()), Schedule::getGroupNo, schedule.getGroupNo())
                    .ne(excludeId != null, Schedule::getId, excludeId));
            if (duplicate > 0) {
                conflicts.add("重复场次：同一项目相同轮次与组别的赛程已存在");
            }
        }

        return conflicts;
    }

    @Override
    public List<RegistrationVO> listScheduleParticipants(Long scheduleId) {
        Schedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        Set<Long> assignedIds = new HashSet<>(participantRegistrationIds(scheduleId));
        if (assignedIds.isEmpty()) {
            return List.of();
        }
        return registrationService.listParticipants(schedule.getEventId(), schedule.getSportItemId())
                .stream()
                .filter(participant -> assignedIds.contains(participant.getId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignScheduleParticipants(Long scheduleId, List<Long> registrationIds) {
        Schedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND);
        }
        if (Objects.equals(schedule.getStatus(), STATUS_COMPLETED)) {
            throw new BusinessException("已结束的赛程不能调整参赛人员");
        }

        List<Long> ids = registrationIds == null ? List.of()
                : registrationIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());

        if (!ids.isEmpty()) {
            List<Registration> registrations = registrationService.listByIds(ids);
            if (registrations.size() != ids.size()) {
                throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND, "存在无效的报名记录");
            }
            for (Registration registration : registrations) {
                if (!Objects.equals(registration.getStatus(), REGISTRATION_APPROVED)) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "只能安排已通过审核的报名人员");
                }
                if (!Objects.equals(registration.getEventId(), schedule.getEventId())
                        || !Objects.equals(registration.getSportItemId(), schedule.getSportItemId())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "报名人员不属于该赛程的赛事或运动项目");
                }
            }

            List<String> conflicts = findPlayerConflicts(schedule, ids, scheduleId);
            if (!conflicts.isEmpty()) {
                throw new BusinessException(ResultCode.SCHEDULE_CONFLICT, String.join("；", conflicts));
            }
        }

        // 整场替换：先物理删除旧安排，再写入新名单
        scheduleParticipantMapper.deleteByScheduleId(scheduleId);
        for (Long registrationId : ids) {
            ScheduleParticipant participant = new ScheduleParticipant();
            participant.setScheduleId(scheduleId);
            participant.setRegistrationId(registrationId);
            scheduleParticipantMapper.insert(participant);
        }
    }

    /**
     * 查询某场赛程已安排的报名ID
     */
    private List<Long> participantRegistrationIds(Long scheduleId) {
        if (scheduleId == null) {
            return List.of();
        }
        return scheduleParticipantMapper.selectList(new LambdaQueryWrapper<ScheduleParticipant>()
                        .eq(ScheduleParticipant::getScheduleId, scheduleId))
                .stream()
                .map(ScheduleParticipant::getRegistrationId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 选手冲突：按「赛程参赛人员」名单，检查选手在同一时段是否已被安排其他场次
     *
     * @param schedule           待保存/待安排的赛程时间
     * @param registrationIds    本场选手报名ID
     * @param excludeScheduleId  排除当前赛程自身
     */
    private List<String> findPlayerConflicts(Schedule schedule, List<Long> registrationIds, Long excludeScheduleId) {
        if (registrationIds == null || registrationIds.isEmpty()
                || schedule.getMatchDate() == null || schedule.getStartTime() == null) {
            return List.of();
        }

        List<Registration> assignedRegistrations = registrationService.listByIds(registrationIds);
        Set<Long> userIds = assignedRegistrations.stream()
                .filter(registration -> Objects.equals(registration.getStatus(), REGISTRATION_APPROVED))
                .map(Registration::getUserId)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return List.of();
        }

        // 这些选手名下的全部有效报名，用于找出他们被安排的其他场次
        List<Registration> userRegistrations = registrationService.list(new LambdaQueryWrapper<Registration>()
                .in(Registration::getUserId, userIds)
                .eq(Registration::getStatus, REGISTRATION_APPROVED));
        if (userRegistrations.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> registrationUserMap = userRegistrations.stream()
                .collect(Collectors.toMap(Registration::getId, Registration::getUserId, (left, right) -> left));

        List<ScheduleParticipant> assignments = scheduleParticipantMapper.selectList(
                new LambdaQueryWrapper<ScheduleParticipant>()
                        .in(ScheduleParticipant::getRegistrationId, registrationUserMap.keySet())
                        .ne(excludeScheduleId != null, ScheduleParticipant::getScheduleId, excludeScheduleId));
        if (assignments.isEmpty()) {
            return List.of();
        }

        Set<Long> otherScheduleIds = assignments.stream()
                .map(ScheduleParticipant::getScheduleId)
                .collect(Collectors.toSet());
        Map<Long, Schedule> conflictSchedules = this.listByIds(otherScheduleIds).stream()
                .filter(other -> other.getMatchDate() != null
                        && other.getMatchDate().equals(schedule.getMatchDate())
                        && isTimeOverlap(schedule.getStartTime(), schedule.getEndTime(),
                        other.getStartTime(), other.getEndTime()))
                .collect(Collectors.toMap(Schedule::getId, Function.identity(), (left, right) -> left));
        if (conflictSchedules.isEmpty()) {
            return List.of();
        }

        Map<Long, Schedule> userConflicts = new LinkedHashMap<>();
        for (ScheduleParticipant assignment : assignments) {
            Schedule other = conflictSchedules.get(assignment.getScheduleId());
            if (other == null) {
                continue;
            }
            Long userId = registrationUserMap.get(assignment.getRegistrationId());
            if (userId != null) {
                userConflicts.putIfAbsent(userId, other);
            }
        }
        if (userConflicts.isEmpty()) {
            return List.of();
        }

        Set<Long> itemIds = userConflicts.values().stream()
                .map(Schedule::getSportItemId)
                .collect(Collectors.toSet());
        Map<Long, String> itemNameMap = sportItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, SportItem::getName));
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<String> messages = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Long, Schedule> entry : userConflicts.entrySet()) {
            if (index >= 3) {
                messages.add("等共 " + userConflicts.size() + " 名选手存在时间冲突");
                break;
            }
            SysUser user = userMap.get(entry.getKey());
            String name = user != null && StringUtils.hasText(user.getRealName())
                    ? user.getRealName() : "用户" + entry.getKey();
            Schedule other = entry.getValue();
            String itemName = itemNameMap.getOrDefault(other.getSportItemId(), "其他项目");
            messages.add("选手冲突：选手「" + name + "」在 " + timeRange(other) + " 已安排「" + itemName + "」");
            index++;
        }
        return messages;
    }

    @Override
    public List<ScheduleVO> listSchedulesByUser(Long userId) {
        List<Registration> registrations = registrationService.list(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getStatus, REGISTRATION_APPROVED));
        if (registrations.isEmpty()) {
            return List.of();
        }

        Set<Long> registrationIds = registrations.stream()
                .map(Registration::getId)
                .collect(Collectors.toSet());
        List<ScheduleParticipant> assignments = scheduleParticipantMapper.selectList(
                new LambdaQueryWrapper<ScheduleParticipant>()
                        .in(ScheduleParticipant::getRegistrationId, registrationIds));
        if (assignments.isEmpty()) {
            return List.of();
        }

        Set<Long> scheduleIds = assignments.stream()
                .map(ScheduleParticipant::getScheduleId)
                .collect(Collectors.toSet());
        List<Schedule> schedules = this.listByIds(scheduleIds).stream()
                .sorted(Comparator.comparing(Schedule::getMatchDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Schedule::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        List<ScheduleVO> voList = convertToVOList(schedules);
        markScheduleConflicts(voList);
        return voList;
    }

    /**
     * 标记个人日程之间的时间冲突（同一时段被安排了多场）
     */
    private void markScheduleConflicts(List<ScheduleVO> schedules) {
        for (int i = 0; i < schedules.size(); i++) {
            for (int j = i + 1; j < schedules.size(); j++) {
                ScheduleVO current = schedules.get(i);
                ScheduleVO other = schedules.get(j);
                if (current.getMatchDate() == null || !current.getMatchDate().equals(other.getMatchDate())) {
                    continue;
                }
                if (!isTimeOverlap(current.getStartTime(), current.getEndTime(), other.getStartTime(), other.getEndTime())) {
                    continue;
                }
                current.setConflict(true);
                other.setConflict(true);
                current.setConflictRemark(appendConflictRemark(current.getConflictRemark(), other));
                other.setConflictRemark(appendConflictRemark(other.getConflictRemark(), current));
            }
        }
    }

    private String appendConflictRemark(String origin, ScheduleVO other) {
        String text = "与「" + other.getSportItemName() + "」("
                + formatTimeRange(other.getStartTime(), other.getEndTime()) + ") 时间冲突";
        return StringUtils.hasText(origin) ? origin + "；" + text : text;
    }

    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || start2 == null) {
            return false;
        }
        LocalTime safeEnd1 = end1 != null ? end1 : start1;
        LocalTime safeEnd2 = end2 != null ? end2 : start2;
        return start1.isBefore(safeEnd2) && start2.isBefore(safeEnd1);
    }

    private String scheduleLabel(Schedule schedule) {
        return StringUtils.hasText(schedule.getScheduleName()) ? schedule.getScheduleName() : "未命名赛程";
    }

    private String timeRange(Schedule schedule) {
        return formatTimeRange(schedule.getStartTime(), schedule.getEndTime());
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        String startText = start == null ? "" : TIME_FORMATTER.format(start);
        String endText = end == null ? "" : TIME_FORMATTER.format(end);
        return endText.isEmpty() ? startText : startText + "-" + endText;
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
