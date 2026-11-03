package com.sports.modules.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 报名审核 DTO
 *
 * @author sports
 */
@Data
public class ReviewDTO {

    /**
     * 审核结果（2-通过 3-拒绝）
     */
    private Integer status;

    /**
     * 审核意见 / 拒绝原因
     */
    @Size(max = 255, message = "审核意见最多255个字符")
    private String remark;

    /**
     * 批量审核的报名ID
     */
    private java.util.List<Long> ids;
}
