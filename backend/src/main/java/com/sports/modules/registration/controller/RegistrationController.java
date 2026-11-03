package com.sports.modules.registration.controller;

import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.modules.registration.dto.RegistrationDTO;
import com.sports.modules.registration.dto.ReviewDTO;
import com.sports.modules.registration.service.RegistrationService;
import com.sports.modules.registration.vo.RegistrationVO;
import com.sports.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 报名 Controller
 *
 * @author sports
 */
@Tag(name = "报名管理", description = "报名的增删改查与审核")
@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @Operation(summary = "学生报名")
    @PostMapping
    public Result<Long> register(@Valid @RequestBody RegistrationDTO dto) {
        return Result.success("报名成功，请等待审核", registrationService.register(dto));
    }

    @Operation(summary = "取消报名")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelRegistration(@PathVariable Long id,
                                           @RequestParam(required = false) String reason) {
        registrationService.cancelRegistration(id, reason);
        return Result.success();
    }

    @Operation(summary = "审核报名")
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> reviewRegistration(@PathVariable Long id, @RequestBody ReviewDTO reviewDTO) {
        registrationService.reviewRegistration(id, reviewDTO.getStatus(), reviewDTO.getRemark());
        return Result.success();
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> approveRegistration(@PathVariable Long id) {
        registrationService.reviewRegistration(id, 2, null);
        return Result.success();
    }

    @Operation(summary = "审核拒绝")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> rejectRegistration(@PathVariable Long id, @RequestBody ReviewDTO reviewDTO) {
        registrationService.reviewRegistration(id, 3, reviewDTO.getRemark());
        return Result.success();
    }

    @Operation(summary = "批量审核")
    @PostMapping("/batch-review")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> batchReview(@RequestBody ReviewDTO reviewDTO) {
        return Result.success(registrationService.batchReview(reviewDTO.getIds(), reviewDTO.getStatus(), reviewDTO.getRemark()));
    }

    @Operation(summary = "分页查询报名列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<PageResult<RegistrationVO>> pageRegistrations(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long sportItemId,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(
                registrationService.pageRegistrations(current, size, eventId, sportItemId, status)));
    }

    @Operation(summary = "获取我的报名列表")
    @GetMapping("/my")
    public Result<PageResult<RegistrationVO>> getMyRegistrations(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(
                registrationService.pageMyRegistrations(current, size, eventId, status)));
    }

    @Operation(summary = "查询已通过审核的参赛人员")
    @GetMapping("/participants")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<RegistrationVO>> listParticipants(@RequestParam(required = false) Long eventId,
                                                         @RequestParam(required = false) Long sportItemId) {
        return Result.success(registrationService.listParticipants(eventId, sportItemId));
    }

    @Operation(summary = "检查是否已报名")
    @GetMapping("/check")
    public Result<Boolean> checkRegistration(@RequestParam Long eventId, @RequestParam Long sportItemId) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(registrationService.hasRegistered(eventId, sportItemId, userId));
    }
}
