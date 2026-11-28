package com.sports.modules.fitness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sports.modules.fitness.entity.FitnessScoreRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 国标体测评分记录 Mapper。
 */
@Mapper
public interface FitnessScoreMapper extends BaseMapper<FitnessScoreRecord> {
}
