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


package com.jc.rag.user.module.auth.model.vo;

import lombok.Data;

/**
 * 站点配置 VO (用户端登录页读取).
 * <p>来源 sys_config 表, 管理端"系统设置"页维护; 登录页据此控制
 * 注册入口显隐、协议勾选强制性与页面品牌信息展示.
 *
 * @author JC
 */
@Data
public class SiteConfigVO {

    /** 系统名称 */
    private String systemName;

    /** 系统 Logo URL */
    private String logoUrl;

    /** 版权信息 */
    private String copyright;

    /** 是否开放用户注册 */
    private Boolean registerOpen;

    /** 登录是否必须勾选用户协议 */
    private Boolean loginAgreeRequired;
}
