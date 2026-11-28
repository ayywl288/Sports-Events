package com.sports.modules.fitness.scoring.data;

import com.sports.modules.fitness.scoring.model.Gender;
import com.sports.modules.fitness.scoring.model.GroupType;
import com.sports.modules.fitness.scoring.model.ScoringItem;

import java.util.List;

/**
 * 评分标准数据源。示例工程使用内存实现；接入实际项目时替换为
 * MyBatis-Plus / JPA 从 scoring_standard_version / scoring_item / scoring_grade 表读取即可。
 */
public interface ScoringDataProvider {

    /**
     * 查询某性别、某评价组别下某个项目的评分定义。
     *
     * @return 找不到时返回 null
     */
    ScoringItem findItem(Gender gender, GroupType groupType, String itemCode);

    /**
     * 查询某性别、某评价组别下的全部项目（含 BMI、肺活量、50米跑、坐位体前屈、
     * 立定跳远、引体向上/仰卧起坐、1000米/800米跑）。
     */
    List<ScoringItem> findItems(Gender gender, GroupType groupType);
}
