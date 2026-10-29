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


package com.jc.rag.user.module.user.controller;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.user.module.user.model.param.ChangePasswordParam;
import com.jc.rag.user.module.user.model.param.UserProfileUpdateParam;
import com.jc.rag.user.module.user.model.vo.UserProfileVO;
import com.jc.rag.user.module.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * C 端用户个人信息 Controller。
 * <p>路径: /api/user/profile
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public R<UserProfileVO> getProfile(Authentication auth) {
        return R.ok(userProfileService.getProfile(currentUserId(auth)));
    }

    @PutMapping
    public R<BaseOpResultVO> updateProfile(@Valid @RequestBody UserProfileUpdateParam param, Authentication auth) {
        return R.ok(userProfileService.updateProfile(currentUserId(auth), param));
    }

    @PostMapping("/change-password")
    public R<BaseOpResultVO> changePassword(@Valid @RequestBody ChangePasswordParam param, Authentication auth) {
        return R.ok(userProfileService.changePassword(currentUserId(auth), param));
    }

    private static Long currentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CoreUserDetails cu)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return cu.getUserId();
    }
}
