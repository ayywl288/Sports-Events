package com.sports.modules.result.controller;

import com.alibaba.excel.EasyExcel;
import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.modules.result.dto.ResultDTO;
import com.sports.modules.result.dto.ResultExcelRow;
import com.sports.modules.result.dto.ResultUpdateDTO;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.result.vo.CompetitionResultVO;
import com.sports.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 比赛成绩 Controller
 *
 * @author sports
 */
@Tag(name = "成绩管理", description = "成绩的查询、录入、导入导出")
@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class CompetitionResultController {

    private final CompetitionResultService competitionResultService;

    @Operation(summary = "分页查询成绩")
    @GetMapping
    public Result<PageResult<CompetitionResultVO>> pageResults(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long sportItemId,
            @RequestParam(required = false) Long userId) {
        return Result.success(PageResult.of(
                competitionResultService.pageResultsVO(current, size, eventId, sportItemId, userId)));
    }

    @Operation(summary = "获取成绩排名")
    @GetMapping("/ranking")
    public Result<List<CompetitionResultVO>> getRanking(@RequestParam Long eventId, @RequestParam Long sportItemId) {
        return Result.success(competitionResultService.getRankingByEventAndItem(eventId, sportItemId));
    }

    @Operation(summary = "获取我的成绩")
    @GetMapping("/my")
    public Result<PageResult<CompetitionResultVO>> getMyResults(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long eventId) {
        return Result.success(PageResult.of(
                competitionResultService.pageMyResults(current, size, eventId)));
    }

    @Operation(summary = "我的成绩统计")
    @GetMapping("/my/statistics")
    public Result<Map<String, Object>> getMyStatistics() {
        return Result.success(competitionResultService.getMyStatistics());
    }

    @Operation(summary = "录入成绩")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Long> saveResult(@Valid @RequestBody ResultDTO dto) {
        return Result.success("录入成功", competitionResultService.saveResult(dto));
    }

    @Operation(summary = "批量录入成绩")
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> batchSaveResults(@RequestBody List<ResultDTO> results) {
        return Result.success(competitionResultService.batchSaveResults(results));
    }

    @Operation(summary = "更新成绩")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> updateResult(@PathVariable Long id, @Valid @RequestBody ResultUpdateDTO dto) {
        competitionResultService.updateResult(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除成绩")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteResult(@PathVariable Long id) {
        competitionResultService.deleteResult(id);
        return Result.success();
    }

    @Operation(summary = "重新计算名次")
    @PostMapping("/recalculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> recalculate(@RequestParam Long eventId, @RequestParam Long sportItemId) {
        competitionResultService.recalculateRanking(eventId, sportItemId);
        return Result.success();
    }

    @Operation(summary = "导出成绩")
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public void export(@RequestParam(required = false) Long eventId,
                       @RequestParam(required = false) Long sportItemId,
                       HttpServletResponse response) throws IOException {
        List<ResultExcelRow> rows = competitionResultService.listForExport(eventId, sportItemId);
        writeExcel(response, rows, "成绩数据");
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public void template(HttpServletResponse response) throws IOException {
        writeExcel(response, List.of(), "成绩导入模板");
    }

    @Operation(summary = "导入成绩")
    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> importResults(@RequestParam("file") MultipartFile file,
                                                     @RequestParam Long eventId) {
        return Result.success("导入完成", competitionResultService.importResults(file, eventId));
    }

    private void writeExcel(HttpServletResponse response, List<ResultExcelRow> rows, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded + ".xlsx");
        EasyExcel.write(response.getOutputStream(), ResultExcelRow.class)
                .sheet("成绩")
                .doWrite(rows);
    }
}
