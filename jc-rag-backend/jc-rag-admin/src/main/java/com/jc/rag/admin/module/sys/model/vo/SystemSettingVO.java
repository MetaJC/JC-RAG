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


package com.jc.rag.admin.module.sys.model.vo;

import lombok.Data;

/**
 * 系统设置 VO (系统设置页读取).
 * <p>存储于 sys_config 表: 基础信息 group=SITE, 账号设置 group=AUTH.
 *
 * @author JC
 */
@Data
public class SystemSettingVO {

    // ===== 基础信息 =====

    /** 系统名称 */
    private String systemName;

    /** 系统 Logo URL */
    private String logoUrl;

    /** 版权信息 */
    private String copyright;

    // ===== 账号设置 =====

    /** 是否开放用户注册 */
    private Boolean registerOpen;

    /** 登录是否必须勾选用户协议 */
    private Boolean loginAgreeRequired;
}
