package com.sports.modules.sportitem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.common.result.ResultCode;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.registration.entity.Registration;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * 运动项目 Controller
 *
 * @author sports
 */
@Tag(name = "运动项目管理", description = "运动项目的增删改查")
@RestController
@RequestMapping("/api/sport-items")
@RequiredArgsConstructor
public class SportItemController {

    private final SportItemService sportItemService;
    private final EventService eventService;
    private final RegistrationService registrationService;

    @Operation(summary = "分页查询运动项目")
    @GetMapping
    public Result<PageResult<SportItem>> pageItems(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(sportItemService.pageItems(current, size, eventId, type, status)));
    }

    @Operation(summary = "根据赛事ID获取项目列表")
    @GetMapping("/event/{eventId}")
    public Result<List<SportItem>> getByEventId(@PathVariable Long eventId) {
        return Result.success(sportItemService.getByEventId(eventId));
    }

    @Operation(summary = "获取项目详情")
    @GetMapping("/{id}")
    public Result<SportItem> getById(@PathVariable Long id) {
        SportItem item = sportItemService.getById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        return Result.success(item);
    }

    @Operation(summary = "创建运动项目")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@RequestBody SportItem sportItem) {
        validate(sportItem);
        sportItem.setId(null);
        sportItem.setStatus(sportItem.getStatus() == null ? 1 : sportItem.getStatus());
        sportItem.setCurrentParticipants(0);
        sportItemService.save(sportItem);
        return Result.success("创建成功", sportItem.getId());
    }

    @Operation(summary = "更新运动项目")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SportItem sportItem) {
        SportItem existing = sportItemService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        validate(sportItem);

        // 不允许把人数上限改到低于已通过人数
        long approved = registrationService.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getSportItemId, id)
                .eq(Registration::getStatus, 2));
        if (sportItem.getMaxParticipants() != null && sportItem.getMaxParticipants() > 0
                && sportItem.getMaxParticipants() < approved) {
            throw new BusinessException("人数上限不能小于已通过报名人数（" + approved + "人）");
        }

        sportItem.setId(id);
        sportItem.setEventId(existing.getEventId());
        sportItem.setCurrentParticipants(existing.getCurrentParticipants());
        sportItemService.updateById(sportItem);
        return Result.success();
    }

    @Operation(summary = "删除运动项目")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        SportItem existing = sportItemService.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        long regCount = registrationService.count(new LambdaQueryWrapper<Registration>()
                .eq(Registration::getSportItemId, id));
        if (regCount > 0) {
            throw new BusinessException("该项目下已有报名记录，不能删除");
        }
        sportItemService.removeById(id);
        return Result.success();
    }

    @Operation(summary = "切换项目状态")
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        SportItem item = sportItemService.getById(id);
        if (item == null) {
            throw new BusinessException(ResultCode.SPORT_ITEM_NOT_FOUND);
        }
        SportItem update = new SportItem();
        update.setId(id);
        update.setStatus(Objects.equals(item.getStatus(), 1) ? 0 : 1);
        sportItemService.updateById(update);
        return Result.success();
    }

    private void validate(SportItem sportItem) {
        if (sportItem.getEventId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "所属赛事不能为空");
        }
        if (sportItem.getName() == null || sportItem.getName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "项目名称不能为空");
        }
        Event event = eventService.getById(sportItem.getEventId());
        if (event == null) {
            throw new BusinessException(ResultCode.EVENT_NOT_FOUND);
        }
        if (Objects.equals(event.getStatus(), 4) || Objects.equals(event.getStatus(), 5)) {
            throw new BusinessException("已结束或已取消的赛事不能维护比赛项目");
        }
        if (sportItem.getMaxParticipants() != null && sportItem.getMaxParticipants() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "人数上限不能为负数");
        }
    }
}
