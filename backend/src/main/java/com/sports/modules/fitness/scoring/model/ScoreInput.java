package com.sports.modules.fitness.scoring.model;

import java.util.List;

/**
 * 评分入参：性别 + 评价组别 + 各项目原始成绩。
 */
public class ScoreInput {

    private Gender gender;
    private GroupType groupType;
    private List<RawScore> rawScores;

    public ScoreInput() {
    }

    public ScoreInput(Gender gender, GroupType groupType, List<RawScore> rawScores) {
        this.gender = gender;
        this.groupType = groupType;
        this.rawScores = rawScores;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public GroupType getGroupType() {
        return groupType;
    }

    public void setGroupType(GroupType groupType) {
        this.groupType = groupType;
    }

    public List<RawScore> getRawScores() {
        return rawScores;
    }

    public void setRawScores(List<RawScore> rawScores) {
        this.rawScores = rawScores;
    }
}
