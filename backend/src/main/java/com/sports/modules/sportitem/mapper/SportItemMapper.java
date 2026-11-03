package com.sports.modules.sportitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.sportitem.entity.SportItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 运动项目 Mapper
 *
 * @author sports
 */
@Mapper
public interface SportItemMapper extends BaseMapper<SportItem> {

    /**
     * 原子增减报名人数。
     * 增加时在 SQL 内判断名额上限，返回 0 表示名额已满，避免并发超卖。
     *
     * @param id    项目ID
     * @param delta 增量（正数增加，负数减少）
     * @return 受影响行数
     */
    @Update("""
            UPDATE sport_item
            SET current_participants = current_participants + #{delta}
            WHERE id = #{id}
              AND is_deleted = 0
              AND (#{delta} < 0
                   OR max_participants IS NULL
                   OR max_participants = 0
                   OR current_participants < max_participants)
            """)
    int incrementParticipants(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 按实际有效报名数（已通过）重算项目的已报名人数
     *
     * @param id 项目ID
     * @return 受影响行数
     */
    @Update("""
            UPDATE sport_item si
            SET si.current_participants = (
                SELECT COUNT(*) FROM registration r
                WHERE r.sport_item_id = si.id AND r.status = 2 AND r.is_deleted = 0
            )
            WHERE si.id = #{id} AND si.is_deleted = 0
            """)
    int recalculateParticipants(@Param("id") Long id);
}
