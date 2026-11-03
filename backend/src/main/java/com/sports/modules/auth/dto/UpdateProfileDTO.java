package com.sports.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料修改 DTO
 *
 * @author sports
 */
@Data
public class UpdateProfileDTO {

    @Size(max = 50, message = "姓名最多50个字符")
    private String realName;

    @Size(max = 50, message = "学号最多50个字符")
    private String studentNo;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 100, message = "院系最多100个字符")
    private String department;

    /**
     * 性别（0-未知 1-男 2-女）
     */
    private Integer gender;

    private String avatar;
}
