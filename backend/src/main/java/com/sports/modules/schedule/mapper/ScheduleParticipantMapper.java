package com.sports.modules.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.schedule.entity.ScheduleParticipant;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 赛程参赛人员 Mapper
 *
 * @author sports
 */
@Mapper
public interface ScheduleParticipantMapper extends BaseMapper<ScheduleParticipant> {

    /**
     * 物理删除某场赛程的选手分配，便于保存时整体替换
     */
    @Delete("DELETE FROM schedule_participant WHERE schedule_id = #{scheduleId}")
    int deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
