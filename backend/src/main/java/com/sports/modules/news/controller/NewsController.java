package com.sports.modules.news.controller;

import com.sports.common.result.PageResult;
import com.sports.common.result.Result;
import com.sports.modules.news.dto.NewsDTO;
import com.sports.modules.news.service.NewsService;
import com.sports.modules.news.vo.NewsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 新闻/公告 Controller
 *
 * @author sports
 */
@Tag(name = "新闻公告管理", description = "新闻公告的增删改查与发布")
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "分页查询公告")
    @GetMapping
    public Result<PageResult<NewsVO>> pageNews(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(newsService.pageNewsVO(current, size, title, type, status)));
    }

    @Operation(summary = "获取公告详情")
    @GetMapping("/{id}")
    public Result<NewsVO> getById(@PathVariable Long id) {
        return Result.success(newsService.getNewsDetail(id));
    }

    @Operation(summary = "创建公告")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Long> create(@Valid @RequestBody NewsDTO dto) {
        return Result.success("创建成功", newsService.createNews(dto));
    }

    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NewsDTO dto) {
        newsService.updateNews(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable Long id) {
        newsService.deleteNews(id);
        return Result.success();
    }

    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> publish(@PathVariable Long id) {
        newsService.publish(id);
        return Result.success();
    }

    @Operation(summary = "下架公告")
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> offline(@PathVariable Long id) {
        newsService.offline(id);
        return Result.success();
    }
}
