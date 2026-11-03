package com.sports.modules.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.event.service.impl.EventServiceImpl;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.mapper.RegistrationMapper;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.vo.RegistrationVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.mapper.SportItemMapper;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 报名 Service 实现
 * 状态说明：1-待审核 2-已通过 3-已拒绝 4-已取消
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    /** 同一赛事最多可报项目数 */
    private static final int MAX_ITEMS_PER_EVENT = 3;

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final SportItemMapper sportItemMapper;
    private final SysUserService sysUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegistrationDTO dto) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!"STUDENT".equals(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有学生可以报名参赛");
        }

        Event event = eventService.getById(dto.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (event.getStatus() == null || event.getStatus() != EventServiceImpl.STATUS_REGISTRATION) {
            throw new BusinessException(ResultCode.EVENT_NOT_PUBLISHED);
        }

        // 报名时间窗口（空值容错）
        LocalDate today = LocalDate.now();
        LocalDate regStart = event.getRegStartDate() != null ? event.getRegStartDate() : event.getStartDate();
        LocalDate regEnd = event.getRegEndDate() != null ? event.getRegEndDate() : event.getStartDate();
        if (regStart != null && today.isBefore(regStart)) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_NOT_STARTED);
        }
        if (regEnd != null && today.isAfter(regEnd)) {
            throw new BusinessException(ResultCode.EVENT_REGISTRATION_ENDED);
        }

        SportItem sportItem = sportItemService.getById(dto.getSportItemId());
        if (sportItem == null || !dto.getEventId().equals(sportItem.getEventId())) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        if (sportItem.getStatus() != null && sportItem.getStatus() != 1) {
            throw new BusinessException(ResultCode.SPORT_ITEM_DISABLED);
        }

        // 性别限制（0-不限 1-仅男 2-仅女）
        Integer genderLimit = sportItem.getGenderLimit();
        if (genderLimit != null && genderLimit != 0 && !genderLimit.equals(user.getGender())) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED,
                    "该项目限制" + (genderLimit == 1 ? "男" : "女") + "性参加");
        }

        // 单赛事报名项目数限制
        long activeCount = this.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getEventId, dto.getEventId())
                .in(Registration::getStatus, 1, 2));
        if (activeCount >= MAX_ITEMS_PER_EVENT) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED,
                    "同一赛事最多报名 " + MAX_ITEMS_PER_EVENT + " 个项目");
        }

        // 名额校验（只统计已通过）
        if (isFull(sportItem)) {
            throw new BusinessException(ResultCode.REGISTRATION_LIMIT_EXCEEDED);
        }

        // 已存在记录：待审核/已通过直接拒绝；已拒绝/已取消则复用原记录重新提交
        Registration existing = this.getOne(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getUserId, userId)
                .eq(Registration::getSportItemId, dto.getSportItemId())
                .last("LIMIT 1"), false);

        if (existing != null) {
            if (existing.getStatus() != null && (existing.getStatus() == 1 || existing.getStatus() == 2)) {
                throw new BusinessException(ResultCode.REGISTRATION_ALREADY_EXISTS);
            }
            existing.setStatus(1);
            existing.setRemark(dto.getRemark());
            existing.setReviewerId(null);
            existing.setReviewTime(null);
            existing.setReviewRemark(null);
            this.updateById(existing);
            return existing.getId();
        }

        Registration registration = new Registration();
        registration.setEventId(dto.getEventId());
        registration.setSportItemId(dto.getSportItemId());
        registration.setUserId(userId);
        registration.setStatus(1);
        registration.setRemark(dto.getRemark());
        this.save(registration);

        return registration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long id, String reason) {
        Registration registration = this.getById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }

        boolean self = registration.getUserId().equals(SecurityUtils.getUserId());
        boolean manager = SecurityUtils.isAdmin() || SecurityUtils.isTeacher();
        if (!self && !manager) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        if (registration.getStatus() == null || registration.getStatus() == 4) {
            throw new BusinessException(ResultCode.REGISTRATION_CANNOT_CANCEL, "该报名已取消");
        }

        // 学生只能取消待审核的报名；管理员/教师可取消任意未取消状态
        if (!manager && registration.getStatus() != 1) {
            throw new BusinessException(ResultCode.REGISTRATION_CANNOT_CANCEL, "只能取消待审核的报名");
        }
        if (manager && !self && !StringUtils.hasText(reason)) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "管理员取消报名需填写原因");
        }

        registration.setStatus(4);
        if (StringUtils.hasText(reason)) {
            registration.setReviewRemark(reason);
        }
        this.updateById(registration);

        // 已通过的报名取消后需要释放名额
        sportItemMapper.recalculateParticipants(registration.getSportItemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRegistration(Long id, Integer status, String remark) {
        if (status == null || (status != 2 && status != 3)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "审核结果只能是 2-通过 或 3-拒绝");
        }
        if (status == 3 && !StringUtils.hasText(remark)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "拒绝报名必须填写原因");
        }

        Registration registration = this.getById(id);
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            throw new BusinessException("只能审核待审核的报名");
        }

        if (status == 2) {
            SportItem sportItem = sportItemService.getById(registration.getSportItemId());
            if (sportItem != null && isFull(sportItem)) {
                throw new BusinessException(ResultCode.REGISTRATION_LIMIT_EXCEEDED);
            }
        }

        registration.setStatus(status);
        registration.setReviewerId(SecurityUtils.getUserId());
        registration.setReviewTime(LocalDateTime.now());
        registration.setReviewRemark(remark);
        this.updateById(registration);

        // 以实际通过数为准重算名额，避免并发下计数漂移
        sportItemMapper.recalculateParticipants(registration.getSportItemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchReview(List<Long> ids, Integer status, String remark) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请选择要审核的报名记录");
        }
        int success = 0;
        List<Map<String, Object>> failed = new ArrayList<>();
        for (Long id : ids) {
            try {
                reviewRegistration(id, status, remark);
                success++;
            } catch (BusinessException e) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", id);
                item.put("reason", e.getMessage());
                failed.add(item);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        return result;
    }

    @Override
    public IPage<RegistrationVO> pageRegistrations(Integer current, Integer size, Long eventId, Long sportItemId, Integer status) {
        Page<Registration> page = new Page<>(current, size);

        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, Registration::getEventId, eventId)
                .eq(sportItemId != null, Registration::getSportItemId, sportItemId)
                .eq(status != null, Registration::getStatus, status)
                .orderByDesc(Registration::getCreateTime);

        return convertToVOPage(this.page(page, wrapper));
    }

    @Override
    public IPage<RegistrationVO> pageMyRegistrations(Integer current, Integer size, Long eventId, Integer status) {
        Page<Registration> page = new Page<>(current, size);

        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, SecurityUtils.getUserId())
                .eq(eventId != null, Registration::getEventId, eventId)
                .eq(status != null, Registration::getStatus, status)
                .orderByDesc(Registration::getCreateTime);

        return convertToVOPage(this.page(page, wrapper));
    }

    @Override
    public List<RegistrationVO> listParticipants(Long eventId, Long sportItemId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getStatus, 2)
                .eq(eventId != null, Registration::getEventId, eventId)
                .eq(sportItemId != null, Registration::getSportItemId, sportItemId)
                .orderByAsc(Registration::getId);
        return convertToVOList(this.list(wrapper));
    }

    @Override
    public boolean hasRegistered(Long eventId, Long sportItemId, Long userId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getEventId, eventId)
                .eq(Registration::getSportItemId, sportItemId)
                .eq(Registration::getUserId, userId)
                .in(Registration::getStatus, 1, 2);

        return this.count(wrapper) > 0;
    }

    /**
     * 名额是否已满（0 或 null 表示不限）
     */
    private boolean isFull(SportItem sportItem) {
        Integer max = sportItem.getMaxParticipants();
        if (max == null || max <= 0) {
            return false;
        }
        long approved = this.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getSportItemId, sportItem.getId())
                .eq(Registration::getStatus, 2));
        return approved >= max;
    }

    private IPage<RegistrationVO> convertToVOPage(IPage<Registration> page) {
        List<RegistrationVO> voList = convertToVOList(page.getRecords());
        Page<RegistrationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 批量转换为 VO（避免 N+1 查询）
     */
    private List<RegistrationVO> convertToVOList(List<Registration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = registrations.stream().map(Registration::getEventId).collect(Collectors.toSet());
        Set<Long> sportItemIds = registrations.stream().map(Registration::getSportItemId).collect(Collectors.toSet());
        Set<Long> userIds = registrations.stream().map(Registration::getUserId).collect(Collectors.toSet());
        Set<Long> reviewerIds = registrations.stream()
                .map(Registration::getReviewerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        userIds.addAll(reviewerIds);

        Map<Long, Event> eventMap = eventService.listByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        Map<Long, SportItem> sportItemMap = sportItemService.listByIds(sportItemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, s -> s));
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        return registrations.stream().map(registration -> {
            RegistrationVO vo = new RegistrationVO();
            BeanUtils.copyProperties(registration, vo);

            Event event = eventMap.get(registration.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }

            SportItem sportItem = sportItemMap.get(registration.getSportItemId());
            if (sportItem != null) {
                vo.setSportItemName(sportItem.getName());
            }

            SysUser user = userMap.get(registration.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setStudentNo(user.getStudentNo());
                vo.setCollege(user.getCollege());
                vo.setPhone(user.getPhone());
            }

            if (registration.getReviewerId() != null) {
                SysUser reviewer = userMap.get(registration.getReviewerId());
                if (reviewer != null) {
                    vo.setReviewerName(reviewer.getRealName());
                }
            }

            vo.setStatusName(getStatusName(registration.getStatus()));
            return vo;
        }).collect(Collectors.toList());
    }

    public static String getStatusName(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已拒绝";
            case 4 -> "已取消";
            default -> String.valueOf(status);
        };
    }
}
