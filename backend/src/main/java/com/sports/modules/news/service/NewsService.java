package com.sports.modules.news.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.news.dto.NewsDTO;
import com.sports.modules.news.entity.News;
import com.sports.modules.news.vo.NewsVO;

/**
 * 新闻/公告 Service
 * 状态说明：0-草稿 1-已发布 2-已下架
 *
 * @author sports
 */
public interface NewsService extends IService<News> {

    /**
     * 分页查询公告（带VO转换，非管理员只能看到已发布）
     */
    IPage<NewsVO> pageNewsVO(Integer current, Integer size, String title, String type, Integer status);

    /**
     * 获取公告详情（含浏览量自增）
     */
    NewsVO getNewsDetail(Long id);

    /**
     * 创建公告
     */
    Long createNews(NewsDTO dto);

    /**
     * 更新公告
     */
    void updateNews(Long id, NewsDTO dto);

    /**
     * 删除公告
     */
    void deleteNews(Long id);

    /**
     * 发布公告
     */
    void publish(Long id);

    /**
     * 下架公告
     */
    void offline(Long id);
}
