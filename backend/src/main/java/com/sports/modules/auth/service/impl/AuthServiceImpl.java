package com.sports.modules.auth.service.impl;

import com.sports.common.exception.BusinessException;
import com.sports.common.result.ResultCode;
import com.sports.modules.auth.dto.ChangePasswordDTO;
import com.sports.modules.auth.dto.LoginDTO;
import com.sports.modules.auth.dto.RegisterDTO;
import com.sports.modules.auth.dto.UpdateProfileDTO;
import com.sports.modules.auth.service.AuthService;
import com.sports.modules.auth.vo.LoginVO;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.JwtUtils;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 认证 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = sysUserService.getByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginVO vo = buildUserVO(user);
        vo.setToken(token);
        vo.setTokenType("Bearer");
        vo.setExpiresIn(expiration / 1000);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_NOT_MATCH);
        }
        if (sysUserService.getByUsername(registerDTO.getUsername()) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
        if (StringUtils.hasText(registerDTO.getStudentNo())
                && sysUserService.count(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStudentNo, registerDTO.getStudentNo())) > 0) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS, "该学号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setRole("STUDENT");
        user.setStudentNo(registerDTO.getStudentNo());
        user.setCollege(registerDTO.getCollege());
        user.setClassName(registerDTO.getClassName());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());

        String genderStr = registerDTO.getGender();
        Integer gender = "M".equals(genderStr) || "1".equals(genderStr) ? 1
                : ("F".equals(genderStr) || "2".equals(genderStr) ? 2 : 0);
        user.setGender(gender);
        user.setStatus(1);

        sysUserService.save(user);
        return user.getId();
    }

    @Override
    public LoginVO getCurrentUser() {
        SysUser user = sysUserService.getById(SecurityUtils.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return buildUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO updateProfile(UpdateProfileDTO dto) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        SysUser update = new SysUser();
        update.setId(userId);
        update.setRealName(dto.getRealName());
        update.setStudentNo(dto.getStudentNo());
        update.setPhone(dto.getPhone());
        update.setEmail(dto.getEmail());
        update.setCollege(dto.getDepartment());
        update.setAvatar(dto.getAvatar());
        if (dto.getGender() != null) {
            update.setGender(dto.getGender());
        }
        sysUserService.updateById(update);

        return buildUserVO(sysUserService.getById(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        SysUser user = sysUserService.getById(SecurityUtils.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新密码不能与原密码相同");
        }

        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserService.updateById(update);
    }

    private LoginVO buildUserVO(SysUser user) {
        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .roleName(getRoleName(user.getRole()))
                .avatar(user.getAvatar())
                .studentNo(user.getStudentNo())
                .className(user.getClassName())
                .department(user.getCollege())
                .phone(user.getPhone())
                .email(user.getEmail())
                .gender(user.getGender())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .build();
    }

    private String getRoleName(String role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            default -> role;
        };
    }
}
