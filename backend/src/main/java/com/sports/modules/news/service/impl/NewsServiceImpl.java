package com.sports.modules.news.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.common.util.HtmlSanitizer;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.news.dto.NewsDTO;
import com.sports.modules.news.entity.News;
import com.sports.modules.news.mapper.NewsMapper;
import com.sports.modules.news.service.NewsService;
import com.sports.modules.news.vo.NewsVO;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 新闻/公告 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class NewsServiceImpl extends ServiceImpl<NewsMapper, News> implements NewsService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;
    private static final int STATUS_OFFLINE = 2;

    /** 公告类型（与字典 news_type 保持一致，兼容历史数据） */
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "NOTICE", "赛事通知",
            "RESULT", "成绩公告",
            "RULE", "规则说明",
            "OTHER", "其他",
            "ANNOUNCEMENT", "公告",
            "NEWS", "新闻"
    );

    private static final Map<Integer, String> STATUS_NAMES = Map.of(
            0, "草稿",
            1, "已发布",
            2, "已下架"
    );

    private final EventService eventService;
    private final SysUserService sysUserService;

    @Override
    public IPage<NewsVO> pageNewsVO(Integer current, Integer size, String title, String type, Integer status) {
        Page<News> page = new Page<>(current, size);

        // 非管理员只能看到已发布公告，且不能通过参数绕过
        boolean admin = SecurityUtils.isAdmin();
        if (!admin) {
            status = STATUS_PUBLISHED;
        }

        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(title), News::getTitle, title)
                .eq(StringUtils.hasText(type), News::getType, type)
                .eq(status != null, News::getStatus, status)
                .orderByDesc(News::getIsTop)
                .orderByDesc(News::getPublishTime)
                .orderByDesc(News::getCreateTime);

        IPage<News> newsPage = this.page(page, wrapper);
        List<NewsVO> voList = convertToVOList(newsPage.getRecords());

        Page<NewsVO> voPage = new Page<>(newsPage.getCurrent(), newsPage.getSize(), newsPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public NewsVO getNewsDetail(Long id) {
        News news = this.getById(id);
        if (news == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (!SecurityUtils.isAdmin() && !Objects.equals(news.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在或未发布");
        }

        // 浏览量为原子自增，读取时使用 +1 后的值
        baseMapper.incrementViewCount(id);
        news.setViewCount(news.getViewCount() == null ? 1 : news.getViewCount() + 1);

        return convertToVOList(List.of(news)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createNews(NewsDTO dto) {
        News news = new News();
        news.setTitle(dto.getTitle().trim());
        news.setType(dto.getType());
        news.setContent(HtmlSanitizer.clean(dto.getContent()));
        news.setCoverImage(dto.getCoverImage());
        news.setEventId(dto.getEventId());
        news.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        news.setViewCount(0);
        news.setStatus(STATUS_DRAFT);
        news.setCreateBy(SecurityUtils.getUserId());
        this.save(news);
        return news.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNews(Long id, NewsDTO dto) {
        News existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }

        // 仅允许更新白名单字段，避免篡改状态、发布人、浏览量
        News update = new News();
        update.setId(id);
        update.setTitle(dto.getTitle().trim());
        update.setType(dto.getType());
        update.setContent(HtmlSanitizer.clean(dto.getContent()));
        update.setCoverImage(dto.getCoverImage());
        update.setEventId(dto.getEventId());
        update.setIsTop(dto.getIsTop() == null ? existing.getIsTop() : dto.getIsTop());
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNews(Long id) {
        News existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        News news = this.getById(id);
        if (news == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (!StringUtils.hasText(news.getTitle()) || !StringUtils.hasText(news.getContent())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "标题与内容不能为空，无法发布");
        }
        if (Objects.equals(news.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException("该公告已发布");
        }

        News update = new News();
        update.setId(id);
        update.setStatus(STATUS_PUBLISHED);
        update.setPublishTime(LocalDateTime.now());
        this.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        News news = this.getById(id);
        if (news == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        if (!Objects.equals(news.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException("只有已发布的公告才能下架");
        }
        News update = new News();
        update.setId(id);
        update.setStatus(STATUS_OFFLINE);
        this.updateById(update);
    }

    /**
     * 批量转换为 VO（避免 N+1 查询）
     */
    private List<NewsVO> convertToVOList(List<News> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return List.of();
        }

        Set<Long> eventIds = newsList.stream()
                .map(News::getEventId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIds = newsList.stream()
                .map(News::getCreateBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> eventNameMap = eventIds.isEmpty() ? Collections.emptyMap()
                : eventService.listByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, Event::getName));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        return newsList.stream().map(news -> {
            NewsVO vo = new NewsVO();
            BeanUtils.copyProperties(news, vo);

            String type = news.getType();
            vo.setTypeName(type == null ? "" : TYPE_NAMES.getOrDefault(type, type));

            Integer status = news.getStatus();
            vo.setStatusName(status == null ? "" : STATUS_NAMES.getOrDefault(status, String.valueOf(status)));

            if (news.getEventId() != null) {
                vo.setEventName(eventNameMap.get(news.getEventId()));
            }

            if (news.getCreateBy() != null) {
                SysUser author = userMap.get(news.getCreateBy());
                if (author != null) {
                    vo.setAuthorName(StringUtils.hasText(author.getRealName()) ? author.getRealName() : author.getUsername());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
