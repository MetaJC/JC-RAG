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


package com.jc.rag.common.constant;

/**
 * sys_config 表配置键常量 (集中定义, 避免 admin/user 两端字符串散落).
 * <p>键命名约定: {分组}.{配置项}, 与 config_group 列对应.
 *
 * @author JC
 */
public final class SysConfigKeys {

    private SysConfigKeys() {
    }

    // ===== 站点基础信息 (config_group = SITE) =====

    /** 系统名称 */
    public static final String SITE_SYSTEM_NAME = "site.system_name";

    /** 系统 Logo URL */
    public static final String SITE_LOGO_URL = "site.logo_url";

    /** 版权信息 */
    public static final String SITE_COPYRIGHT = "site.copyright";

    // ===== 账号与安全 (config_group = AUTH) =====

    /** 是否开放用户注册 (1=开放, 0=关闭; 关闭后注册接口与短信自动注册均不可用) */
    public static final String AUTH_REGISTER_OPEN = "auth.register.open";

    /** 登录是否必须勾选用户协议 (1=必须勾选, 0=隐藏协议复选框且不限制) */
    public static final String AUTH_LOGIN_AGREE_REQUIRED = "auth.login.agree_required";
}
