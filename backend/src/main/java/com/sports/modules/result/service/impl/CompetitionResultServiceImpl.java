package com.sports.modules.result.service.impl;

import com.alibaba.excel.EasyExcel;
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
import com.sports.modules.result.dto.ResultDTO;
import com.sports.modules.result.dto.ResultExcelRow;
import com.sports.modules.result.dto.ResultUpdateDTO;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.mapper.CompetitionResultMapper;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.result.vo.CompetitionResultVO;
import com.sports.modules.schedule.entity.Schedule;
import com.sports.modules.schedule.service.ScheduleService;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 比赛成绩 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class CompetitionResultServiceImpl extends ServiceImpl<CompetitionResultMapper, CompetitionResult>
        implements CompetitionResultService {

    /** 名次对应积分：第1名9分、第2名7分、第3名6分…第8名1分 */
    private static final int[] POINTS_TABLE = {9, 7, 6, 5, 4, 3, 2, 1};

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final SysUserService sysUserService;
    private final RegistrationService registrationService;
    private final ScheduleService scheduleService;

    @Override
    public IPage<CompetitionResultVO> pageResultsVO(Integer current, Integer size, Long eventId, Long sportItemId, Long userId) {
        // 学生只能查看自己的成绩
        if (SecurityUtils.isStudent()) {
            userId = SecurityUtils.getUserId();
        }

        Page<CompetitionResult> page = new Page<>(current, size);
        LambdaQueryWrapper<CompetitionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, CompetitionResult::getEventId, eventId)
                .eq(sportItemId != null, CompetitionResult::getSportItemId, sportItemId)
                .eq(userId != null, CompetitionResult::getUserId, userId)
                // NULL 名次排在最后，避免未排名成绩抢占首页
                .last("ORDER BY (ranking IS NULL) ASC, ranking ASC, create_time DESC");

        return convertToVOPage(this.page(page, wrapper));
    }

    @Override
    public List<CompetitionResultVO> getRankingByEventAndItem(Long eventId, Long sportItemId) {
        LambdaQueryWrapper<CompetitionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompetitionResult::getEventId, eventId)
                .eq(CompetitionResult::getSportItemId, sportItemId)
                .last("ORDER BY (ranking IS NULL) ASC, ranking ASC, score ASC");
        return convertToVOList(this.list(wrapper));
    }

    @Override
    public IPage<CompetitionResultVO> pageMyResults(Integer current, Integer size, Long eventId) {
        return pageResultsVO(current, size, eventId, null, SecurityUtils.getUserId());
    }

    @Override
    public Map<String, Object> getMyStatistics() {
        Long userId = SecurityUtils.getUserId();
        List<CompetitionResult> list = this.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getUserId, userId));

        long distinctEvents = list.stream()
                .map(CompetitionResult::getEventId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("totalResults", list.size());
        data.put("totalEvents", distinctEvents);
        data.put("goldMedals", list.stream().filter(r -> Objects.equals(r.getRanking(), 1)).count());
        data.put("silverMedals", list.stream().filter(r -> Objects.equals(r.getRanking(), 2)).count());
        data.put("bronzeMedals", list.stream().filter(r -> Objects.equals(r.getRanking(), 3)).count());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveResult(ResultDTO dto) {
        Registration registration = registrationService.getById(dto.getRegistrationId());
        if (registration == null) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND);
        }
        if (!Objects.equals(registration.getStatus(), 2)) {
            throw new BusinessException(ResultCode.REGISTRATION_NOT_ALLOWED, "该报名尚未通过审核，不能录入成绩");
        }

        Event event = eventService.getById(registration.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (event.getStatus() == null || event.getStatus() == 1 || event.getStatus() == 5) {
            throw new BusinessException("赛事未发布或已取消，不能录入成绩");
        }

        SportItem item = sportItemService.getById(registration.getSportItemId());
        if (item == null || !registration.getEventId().equals(item.getEventId())) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }

        long exists = this.count(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getRegistrationId, dto.getRegistrationId()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.RESULT_ALREADY_EXISTS);
        }

        if (dto.getScore() == null && !StringUtils.hasText(dto.getScoreText())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "成绩值与成绩文本至少填写一项");
        }

        if (dto.getScheduleId() != null) {
            Schedule schedule = scheduleService.getById(dto.getScheduleId());
            if (schedule == null
                    || !registration.getEventId().equals(schedule.getEventId())
                    || !registration.getSportItemId().equals(schedule.getSportItemId())) {
                throw new BusinessException(ResultCode.SCHEDULE_NOT_FOUND, "赛程不属于该赛事项目");
            }
        }

        CompetitionResult result = new CompetitionResult();
        result.setEventId(registration.getEventId());
        result.setSportItemId(registration.getSportItemId());
        result.setRegistrationId(registration.getId());
        result.setUserId(registration.getUserId());
        result.setScheduleId(dto.getScheduleId());
        result.setScore(dto.getScore());
        result.setScoreText(dto.getScoreText());
        result.setRanking(dto.getRanking());
        result.setPoints(dto.getPoints());
        result.setRemark(dto.getRemark());
        result.setRecordBy(SecurityUtils.getUserId());
        result.setRecordTime(LocalDateTime.now());

        this.save(result);

        // 名次/积分未手工指定时，按项目排序规则自动计算
        if (dto.getRanking() == null) {
            recalculateRanking(result.getEventId(), result.getSportItemId());
        }

        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResult(Long id, ResultUpdateDTO dto) {
        CompetitionResult result = this.getById(id);
        if (result == null) {
            throw new BusinessException(ResultCode.RESULT_NOT_FOUND);
        }
        if (dto.getScore() == null && dto.getScoreText() == null && dto.getRanking() == null
                && dto.getPoints() == null && dto.getRemark() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "没有需要修改的内容");
        }

        CompetitionResult update = new CompetitionResult();
        update.setId(id);
        update.setScore(dto.getScore());
        update.setScoreText(dto.getScoreText());
        update.setRanking(dto.getRanking());
        update.setPoints(dto.getPoints());
        update.setRemark(dto.getRemark());
        this.updateById(update);

        if (dto.getRanking() == null) {
            recalculateRanking(result.getEventId(), result.getSportItemId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResult(Long id) {
        CompetitionResult result = this.getById(id);
        if (result == null) {
            throw new BusinessException(ResultCode.RESULT_NOT_FOUND);
        }
        this.removeById(id);
        recalculateRanking(result.getEventId(), result.getSportItemId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchSaveResults(List<ResultDTO> list) {
        if (CollectionUtils.isEmpty(list)) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请提供要录入的成绩");
        }
        int success = 0;
        List<Map<String, Object>> failed = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ResultDTO dto = list.get(i);
            try {
                saveResult(dto);
                success++;
            } catch (BusinessException e) {
                Map<String, Object> item = new HashMap<>();
                item.put("row", i + 1);
                item.put("registrationId", dto.getRegistrationId());
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
    public List<ResultExcelRow> listForExport(Long eventId, Long sportItemId) {
        List<CompetitionResult> results = this.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(eventId != null, CompetitionResult::getEventId, eventId)
                .eq(sportItemId != null, CompetitionResult::getSportItemId, sportItemId)
                .last("ORDER BY sport_item_id ASC, (ranking IS NULL) ASC, ranking ASC"));

        if (results.isEmpty()) {
            return List.of();
        }

        Set<Long> itemIds = results.stream().map(CompetitionResult::getSportItemId).collect(Collectors.toSet());
        Set<Long> userIds = results.stream().map(CompetitionResult::getUserId).collect(Collectors.toSet());
        Map<Long, String> itemNameMap = sportItemService.listByIds(itemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, SportItem::getName));
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        return results.stream().map(result -> {
            ResultExcelRow row = new ResultExcelRow();
            SysUser user = userMap.get(result.getUserId());
            if (user != null) {
                row.setStudentNo(user.getStudentNo());
                row.setRealName(user.getRealName());
            }
            row.setSportItemName(itemNameMap.get(result.getSportItemId()));
            row.setScore(result.getScore());
            row.setScoreText(result.getScoreText());
            row.setRanking(result.getRanking());
            row.setPoints(result.getPoints());
            row.setRemark(result.getRemark());
            return row;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importResults(MultipartFile file, Long eventId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND, "请选择要导入的文件");
        }
        if (eventId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "请选择赛事");
        }
        Event event = eventService.getById(eventId);
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }

        List<ResultExcelRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(ResultExcelRow.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_ERROR, "文件解析失败，请使用系统提供的模板");
        }

        if (CollectionUtils.isEmpty(rows)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件内容为空");
        }

        // 预加载该赛事下的项目与已通过报名，避免逐行查库
        Map<String, Long> itemNameMap = sportItemService.list(new LambdaQueryWrapper<SportItem>()
                        .eq(SportItem::getEventId, eventId))
                .stream()
                .collect(Collectors.toMap(SportItem::getName, SportItem::getId, (a, b) -> a));

        List<Registration> approved = registrationService.list(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getEventId, eventId)
                .eq(Registration::getStatus, 2));
        Map<Long, SysUser> userMap = approved.isEmpty() ? Collections.emptyMap()
                : sysUserService.listByIds(approved.stream().map(Registration::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        int success = 0;
        List<Map<String, Object>> failed = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            ResultExcelRow row = rows.get(i);
            int rowNo = i + 2; // 表格含表头
            try {
                if (!StringUtils.hasText(row.getStudentNo())) {
                    throw new BusinessException(ResultCode.PARAM_MISSING, "学号为空");
                }
                if (!StringUtils.hasText(row.getSportItemName())) {
                    throw new BusinessException(ResultCode.PARAM_MISSING, "运动项目为空");
                }
                Long itemId = itemNameMap.get(row.getSportItemName().trim());
                if (itemId == null) {
                    throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND,
                            "赛事下不存在项目：" + row.getSportItemName());
                }
                Registration target = approved.stream()
                        .filter(r -> r.getSportItemId().equals(itemId))
                        .filter(r -> {
                            SysUser u = userMap.get(r.getUserId());
                            return u != null && row.getStudentNo().trim().equals(u.getStudentNo());
                        })
                        .findFirst()
                        .orElse(null);
                if (target == null) {
                    throw new BusinessException(ResultCode.REGISTRATION_NOT_FOUND,
                            "未找到该学号在对应项目下已通过审核的报名");
                }

                ResultDTO dto = new ResultDTO();
                dto.setRegistrationId(target.getId());
                dto.setScore(row.getScore());
                dto.setScoreText(row.getScoreText());
                dto.setRanking(row.getRanking());
                dto.setPoints(row.getPoints());
                dto.setRemark(row.getRemark());
                saveResult(dto);
                success++;
            } catch (BusinessException e) {
                Map<String, Object> item = new HashMap<>();
                item.put("row", rowNo);
                item.put("studentNo", row.getStudentNo());
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
    @Transactional(rollbackFor = Exception.class)
    public void recalculateRanking(Long eventId, Long sportItemId) {
        if (eventId == null || sportItemId == null) {
            return;
        }
        SportItem item = sportItemService.getById(sportItemId);
        boolean ascending = item != null && "ASC".equalsIgnoreCase(item.getSortRule());

        List<CompetitionResult> results = this.list(new LambdaQueryWrapper<CompetitionResult>()
                .eq(CompetitionResult::getEventId, eventId)
                .eq(CompetitionResult::getSportItemId, sportItemId)
                .isNotNull(CompetitionResult::getScore));

        if (results.isEmpty()) {
            return;
        }

        Comparator<CompetitionResult> comparator = Comparator.comparing(CompetitionResult::getScore);
        results.sort(ascending ? comparator : comparator.reversed());

        BigDecimal prevScore = null;
        int prevRank = 0;
        List<CompetitionResult> updates = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            CompetitionResult current = results.get(i);
            int rank;
            if (prevScore != null && prevScore.compareTo(current.getScore()) == 0) {
                // 并列名次：成绩相同取同名次（如 1、1、3）
                rank = prevRank;
            } else {
                rank = i + 1;
            }
            prevScore = current.getScore();
            prevRank = rank;

            CompetitionResult update = new CompetitionResult();
            update.setId(current.getId());
            update.setRanking(rank);
            update.setPoints(pointsOf(rank));
            updates.add(update);
        }

        this.updateBatchById(updates);
    }

    private int pointsOf(int rank) {
        return rank >= 1 && rank <= POINTS_TABLE.length ? POINTS_TABLE[rank - 1] : 0;
    }

    private IPage<CompetitionResultVO> convertToVOPage(IPage<CompetitionResult> page) {
        List<CompetitionResultVO> voList = convertToVOList(page.getRecords());
        Page<CompetitionResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 批量转换为 VO（避免 N+1 查询）
     */
    private List<CompetitionResultVO> convertToVOList(List<CompetitionResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = results.stream().map(CompetitionResult::getEventId).collect(Collectors.toSet());
        Set<Long> sportItemIds = results.stream().map(CompetitionResult::getSportItemId).collect(Collectors.toSet());
        Set<Long> userIds = results.stream().map(CompetitionResult::getUserId).collect(Collectors.toSet());
        Set<Long> recordByIds = results.stream()
                .map(CompetitionResult::getRecordBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        userIds.addAll(recordByIds);

        Map<Long, Event> eventMap = eventService.listByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        Map<Long, SportItem> sportItemMap = sportItemService.listByIds(sportItemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, s -> s));
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        return results.stream().map(result -> {
            CompetitionResultVO vo = new CompetitionResultVO();
            BeanUtils.copyProperties(result, vo);

            Event event = eventMap.get(result.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }

            SportItem sportItem = sportItemMap.get(result.getSportItemId());
            if (sportItem != null) {
                vo.setSportItemName(sportItem.getName());
                vo.setUnit(sportItem.getScoreUnit());
            }

            SysUser user = userMap.get(result.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setStudentNo(user.getStudentNo());
                vo.setCollege(user.getCollege());
            }

            if (result.getRecordBy() != null) {
                SysUser recorder = userMap.get(result.getRecordBy());
                if (recorder != null) {
                    vo.setRecorderName(recorder.getRealName());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }
}
