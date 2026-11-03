package com.sports.modules.event.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.dto.EventDTO;
import com.sports.modules.event.dto.EventQueryDTO;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.mapper.EventMapper;
import com.sports.modules.event.service.EventService;
import com.sports.modules.event.vo.EventVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.mapper.RegistrationMapper;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.mapper.SportItemMapper;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 赛事 Service 实现
 * 状态说明: 1-草稿 2-报名中 3-进行中 4-已结束 5-已取消
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, Event> implements EventService {

    public static final int STATUS_DRAFT = 1;
    public static final int STATUS_REGISTRATION = 2;
    public static final int STATUS_ONGOING = 3;
    public static final int STATUS_ENDED = 4;
    public static final int STATUS_CANCELLED = 5;

    private final SportItemMapper sportItemMapper;
    private final RegistrationMapper registrationMapper;

    @Override
    public IPage<EventVO> pageEvents(EventQueryDTO queryDTO) {
        Page<Event> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());

        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        // 学生只能看到已对外发布的赛事，不能看到草稿和已取消
        boolean onlyPublished = SecurityUtils.isStudent();
        wrapper.in(onlyPublished, Event::getStatus, List.of(STATUS_REGISTRATION, STATUS_ONGOING, STATUS_ENDED))
                .like(StringUtils.hasText(queryDTO.getName()), Event::getName, queryDTO.getName())
                .eq(StringUtils.hasText(queryDTO.getType()), Event::getType, queryDTO.getType())
                .eq(!onlyPublished && queryDTO.getStatus() != null, Event::getStatus, queryDTO.getStatus())
                .apply(queryDTO.getYear() != null, "YEAR(start_date) = {0}", queryDTO.getYear())
                .orderByDesc(Event::getCreateTime);

        IPage<Event> eventPage = this.page(page, wrapper);
        return fillCounts(eventPage.convert(this::convertToVO));
    }

    @Override
    public EventVO getEventDetail(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (SecurityUtils.isStudent() && !List.of(STATUS_REGISTRATION, STATUS_ONGOING, STATUS_ENDED).contains(event.getStatus())) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        EventVO vo = convertToVO(event);
        fillCounts(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(EventDTO eventDTO) {
        validateEventDates(eventDTO);

        Event event = new Event();
        BeanUtils.copyProperties(eventDTO, event);
        // 新建赛事一律为草稿，禁止前端直接指定状态绕过发布流程
        event.setStatus(STATUS_DRAFT);
        event.setCreatorId(SecurityUtils.getUserId());

        this.save(event);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEvent(Long id, EventDTO eventDTO) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        Integer status = event.getStatus();
        if (status != null && (status == STATUS_ONGOING || status == STATUS_ENDED)) {
            throw new BusinessException("进行中或已结束的赛事不能修改");
        }

        validateEventDates(eventDTO);

        // 已发布赛事不允许改开始日期与类型，避免已产生的报名/赛程失效
        if (Objects.equals(status, STATUS_REGISTRATION)) {
            if (eventDTO.getStartDate() != null && !eventDTO.getStartDate().equals(event.getStartDate())) {
                throw new BusinessException("赛事已开放报名，不能修改开始日期");
            }
            if (StringUtils.hasText(eventDTO.getType()) && !eventDTO.getType().equals(event.getType())
                    && countRegistrations(id) > 0) {
                throw new BusinessException("赛事已有报名记录，不能修改赛事类型");
            }
        }

        BeanUtils.copyProperties(eventDTO, event, "id", "status", "creatorId", "createTime");
        this.updateById(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvent(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        if (!Objects.equals(event.getStatus(), STATUS_DRAFT) && !Objects.equals(event.getStatus(), STATUS_CANCELLED)) {
            throw new BusinessException("只能删除草稿或已取消状态的赛事");
        }

        long itemCount = sportItemMapper.selectCount(new LambdaQueryWrapper<SportItem>().eq(SportItem::getEventId, id));
        if (itemCount > 0) {
            throw new BusinessException("该赛事下还有 " + itemCount + " 个运动项目，请先删除项目");
        }
        if (countRegistrations(id) > 0) {
            throw new BusinessException("该赛事已有报名记录，不能删除");
        }

        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (!Objects.equals(event.getStatus(), STATUS_DRAFT)) {
            throw new BusinessException("只有草稿状态的赛事才能发布");
        }

        long itemCount = sportItemMapper.selectCount(new LambdaQueryWrapper<SportItem>()
                .eq(SportItem::getEventId, id)
                .eq(SportItem::getStatus, 1));
        if (itemCount == 0) {
            throw new BusinessException("发布前请先为赛事添加至少一个启用状态的比赛项目");
        }
        if (event.getRegStartDate() == null || event.getRegEndDate() == null) {
            throw new BusinessException("发布前请先完善报名开始与截止日期");
        }
        if (event.getRegEndDate().isAfter(event.getStartDate())) {
            throw new BusinessException("报名截止日期不能晚于赛事开始日期");
        }

        event.setStatus(STATUS_REGISTRATION);
        this.updateById(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelEvent(Long id) {
        Event event = this.getById(id);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (Objects.equals(event.getStatus(), STATUS_ENDED) || Objects.equals(event.getStatus(), STATUS_CANCELLED)) {
            throw new BusinessException("已结束或已取消的赛事不能再取消");
        }

        event.setStatus(STATUS_CANCELLED);
        this.updateById(event);

        // 未审核的报名一并关闭，已通过的保留用于成绩与统计
        registrationMapper.update(null, new LambdaUpdateWrapper<Registration>()
                .eq(Registration::getEventId, id)
                .eq(Registration::getStatus, 1)
                .set(Registration::getStatus, 4));
    }

    @Override
    public List<EventVO> getAvailableEvents() {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Event::getStatus, STATUS_REGISTRATION)
                .isNotNull(Event::getRegStartDate)
                .isNotNull(Event::getRegEndDate)
                .le(Event::getRegStartDate, today)
                .ge(Event::getRegEndDate, today)
                .orderByAsc(Event::getRegEndDate);

        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventVO> getOngoingEvents() {
        LambdaQueryWrapper<Event> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Event::getStatus, STATUS_ONGOING)
                .orderByAsc(Event::getStartDate);

        return this.list(wrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private long countRegistrations(Long eventId) {
        return registrationMapper.selectCount(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getEventId, eventId));
    }

    /**
     * 验证赛事日期
     */
    private void validateEventDates(EventDTO eventDTO) {
        if (eventDTO.getStartDate() == null || eventDTO.getEndDate() == null) {
            throw new BusinessException("赛事开始与结束日期不能为空");
        }
        if (eventDTO.getStartDate().isAfter(eventDTO.getEndDate())) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        if (eventDTO.getRegStartDate() != null && eventDTO.getRegEndDate() != null) {
            if (eventDTO.getRegStartDate().isAfter(eventDTO.getRegEndDate())) {
                throw new BusinessException("报名开始日期不能晚于报名截止日期");
            }
            if (eventDTO.getRegEndDate().isAfter(eventDTO.getStartDate())) {
                throw new BusinessException("报名截止日期不能晚于赛事开始日期");
            }
        }
    }

    /**
     * 批量填充报名人数、项目数量
     */
    private IPage<EventVO> fillCounts(IPage<EventVO> page) {
        fillCounts(page.getRecords());
        return page;
    }

    private void fillCounts(List<EventVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> eventIds = records.stream().map(EventVO::getId).collect(Collectors.toSet());

        Map<Long, Long> itemCountMap = sportItemMapper.selectList(new LambdaQueryWrapper<SportItem>()
                        .in(SportItem::getEventId, eventIds))
                .stream()
                .collect(Collectors.groupingBy(SportItem::getEventId, Collectors.counting()));

        Map<Long, Long> regCountMap = registrationMapper.selectList(new LambdaQueryWrapper<Registration>()
                        .in(Registration::getEventId, eventIds)
                        .ne(Registration::getStatus, 4))
                .stream()
                .collect(Collectors.groupingBy(Registration::getEventId, Collectors.counting()));

        for (EventVO vo : records) {
            vo.setSportItemCount(itemCountMap.getOrDefault(vo.getId(), 0L).intValue());
            vo.setRegistrationCount(regCountMap.getOrDefault(vo.getId(), 0L).intValue());
        }
    }

    /**
     * 转换为 VO
     */
    private EventVO convertToVO(Event event) {
        EventVO vo = new EventVO();
        BeanUtils.copyProperties(event, vo);

        vo.setTypeName(getTypeName(event.getType()));
        vo.setStatusName(getStatusName(event.getStatus()));
        vo.setRegOpen(isRegOpen(event));

        return vo;
    }

    private boolean isRegOpen(Event event) {
        if (!Objects.equals(event.getStatus(), STATUS_REGISTRATION)) {
            return false;
        }
        if (event.getRegStartDate() == null || event.getRegEndDate() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !today.isBefore(event.getRegStartDate()) && !today.isAfter(event.getRegEndDate());
    }

    private String getTypeName(String type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case "TRACK_FIELD", "TRACK", "FIELD" -> "田径";
            case "BALL" -> "球类";
            case "SWIMMING", "WATER" -> "游泳";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    public static String getStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 1 -> "草稿";
            case 2 -> "报名中";
            case 3 -> "进行中";
            case 4 -> "已结束";
            case 5 -> "已取消";
            default -> String.valueOf(status);
        };
    }
}
