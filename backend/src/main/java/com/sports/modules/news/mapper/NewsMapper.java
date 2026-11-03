package com.sports.modules.news.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.news.entity.News;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 新闻/公告 Mapper
 *
 * @author sports
 */
@Mapper
public interface NewsMapper extends BaseMapper<News> {

    /**
     * 浏览量原子自增，避免并发读-改-写丢失
     *
     * @param id 公告ID
     * @return 受影响行数
     */
    @Update("UPDATE news SET view_count = view_count + 1 WHERE id = #{id} AND is_deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}
