package com.sports.modules.fitness.dto;

import com.sports.modules.fitness.scoring.model.RecordStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 成绩审核请求：教师/管理员把记录标记为有效或无效。
 */
@Data
public class FitnessScoreStatusRequest {

    @NotNull(message = "请选择审核状态")
    private RecordStatus status;

    /** 不传时使用当前登录账号。 */
    private String operator;
}
