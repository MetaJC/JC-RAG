/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.admin.module.auth.controller;

import com.jc.rag.admin.module.auth.model.vo.AuthUserVO;
import com.jc.rag.admin.module.auth.model.param.ChangePasswordParam;
import com.jc.rag.admin.module.auth.model.param.LoginParam;
import com.jc.rag.admin.module.auth.service.AuthService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与会话接口。
 * <p>说明：当前采用 Spring Security Session 方案，前端通过 cookie 携带会话。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录 (创建 Session) */
    @PostMapping("/login")
    public R<AuthUserVO> login(
            @Valid @RequestBody LoginParam request,
            HttpServletRequest httpServletRequest
    ) {
        return R.ok(authService.login(request, httpServletRequest));
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/me")
    public R<AuthUserVO> currentUser() {
        return R.ok(authService.currentUser());
    }

    /** 退出登录 (注销 Session) */
    @PostMapping("/logout")
    public R<BaseOpResultVO> logout(HttpServletRequest request, HttpServletResponse response) {
        return R.ok(authService.logout(request, response));
    }

    /** 修改密码 (当前登录用户) */
    @PostMapping("/change-password")
    public R<BaseOpResultVO> changePassword(@Valid @RequestBody ChangePasswordParam request) {
        return R.ok(authService.changePassword(request));
    }
}
