package com.sports.modules.auth.controller;

import com.sports.common.result.Result;
import com.sports.modules.auth.dto.ChangePasswordDTO;
import com.sports.modules.auth.dto.LoginDTO;
import com.sports.modules.auth.dto.RegisterDTO;
import com.sports.modules.auth.dto.UpdateProfileDTO;
import com.sports.modules.auth.service.AuthService;
import com.sports.modules.auth.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证 Controller
 *
 * @author sports
 */
@Tag(name = "认证管理", description = "登录、注册、用户信息、资料与密码修改")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterDTO registerDTO) {
        return Result.success("注册成功", authService.register(registerDTO));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<LoginVO> getCurrentUser() {
        return Result.success(authService.getCurrentUser());
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<LoginVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success("保存成功", authService.updateProfile(dto));
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success("密码修改成功", null);
    }
}
