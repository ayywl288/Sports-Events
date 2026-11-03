package com.sports.modules.auth.service;

import com.sports.modules.auth.dto.ChangePasswordDTO;
import com.sports.modules.auth.dto.LoginDTO;
import com.sports.modules.auth.dto.RegisterDTO;
import com.sports.modules.auth.dto.UpdateProfileDTO;
import com.sports.modules.auth.vo.LoginVO;

/**
 * 认证 Service
 *
 * @author sports
 */
public interface AuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 注册
     */
    Long register(RegisterDTO registerDTO);

    /**
     * 获取当前用户信息
     */
    LoginVO getCurrentUser();

    /**
     * 修改个人资料
     */
    LoginVO updateProfile(UpdateProfileDTO dto);

    /**
     * 修改密码
     */
    void changePassword(ChangePasswordDTO dto);
}
