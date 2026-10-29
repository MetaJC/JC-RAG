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


package com.jc.rag.admin.module.auth.service;

import com.jc.rag.admin.module.auth.model.param.ChangePasswordParam;
import com.jc.rag.admin.module.auth.model.param.LoginParam;
import com.jc.rag.admin.module.auth.model.vo.AuthUserVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AuthService 接口
 *
 * @author JC
 */
public interface AuthService {

    /**
     * 登录并创建 Session。
     */
    AuthUserVO login(LoginParam request, HttpServletRequest httpServletRequest);

    /**
     * 获取当前登录用户信息。
     */
    AuthUserVO currentUser();

    /**
     * 退出登录并销毁 Session。
     */
    BaseOpResultVO logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 修改当前用户密码。
     */
    BaseOpResultVO changePassword(ChangePasswordParam request);
}
