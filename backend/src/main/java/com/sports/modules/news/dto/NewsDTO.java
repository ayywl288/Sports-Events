package com.sports.modules.news.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 公告 DTO
 *
 * @author sports
 */
@Data
public class NewsDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200个字符")
    private String title;

    /**
     * 公告类型（字典 news_type：NOTICE/RESULT/RULE/OTHER）
     */
    @NotBlank(message = "公告类型不能为空")
    private String type;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 255, message = "封面地址过长")
    private String coverImage;

    private Long eventId;

    /**
     * 是否置顶（0-否 1-是）
     */
    private Integer isTop;
}
