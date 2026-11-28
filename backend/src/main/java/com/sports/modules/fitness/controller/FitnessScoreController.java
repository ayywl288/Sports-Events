package com.sports.modules.fitness.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sports.common.result.Result;
import com.sports.modules.fitness.dto.FitnessScoreRequest;
import com.sports.modules.fitness.dto.FitnessScoreStatusRequest;
import com.sports.modules.fitness.service.FitnessScoreService;
import com.sports.modules.fitness.vo.FitnessScoreVO;
import com.sports.modules.fitness.vo.FitnessStudentVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 国标体测评分接口。
 * <p>
 * 与小组已有的 /api/fitness（基础测量与聚类）并存，互不影响。
 */
@Validated
@RestController
@RequestMapping("/api/fitness/scores")
@RequiredArgsConstructor
public class FitnessScoreController {

    private final FitnessScoreService scoreService;

    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<FitnessScoreVO> preview(@Valid @RequestBody FitnessScoreRequest request) {
        return Result.success(scoreService.preview(request));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> create(@Valid @RequestBody FitnessScoreRequest request) {
        return Result.success(scoreService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable @Positive Long id,
                               @Valid @RequestBody FitnessScoreRequest request) {
        scoreService.update(id, request);
        return Result.success();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<FitnessScoreVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) int current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate testDate) {
        return Result.success(scoreService.page(current, size, userId, testDate));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<FitnessScoreVO>> myScores() {
        return Result.success(scoreService.myScores());
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<FitnessStudentVO>> students(
            @RequestParam(defaultValue = "") @Size(max = 80) String q) {
        return Result.success(scoreService.studentOptions(q));
    }

    @GetMapping("/{id}")
    public Result<FitnessScoreVO> detail(@PathVariable @Positive Long id) {
        return Result.success(scoreService.detail(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<FitnessScoreVO> updateStatus(@PathVariable @Positive Long id,
                                               @Valid @RequestBody FitnessScoreStatusRequest request) {
        return Result.success(scoreService.updateStatus(id, request.getStatus(), request.getOperator()));
    }
}
