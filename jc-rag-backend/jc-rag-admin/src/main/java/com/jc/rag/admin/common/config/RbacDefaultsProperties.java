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


package com.jc.rag.admin.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RBAC 默认配置项 (可覆盖硬编码)
 * application.yml: app.rbac.*
 * @author JC
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.rbac")
public class RbacDefaultsProperties {

    /** 默认密码 (新建用户时未填写则用此) */
    private String defaultPassword = "123456";

    /** 密码最短长度 */
    private int passwordMinLength = 6;

    /** 开发者保留账号 (跳过一切鉴权，不可被业务用户看见) */
    private String developerUsername = "developer";

    /** 超级账号 (可被授权编辑，但默认拥有所有权限) */
    private String superAdminUsername = "admin";

    /** 超级角色编码 (新建菜单/按钮/接口时自动绑定此角色) */
    private String superAdminRoleCode = "super_admin";

    /** 保留账号类型 (不允许变更) */
    private List<String> reservedUserTypes = List.of("DEVELOPER");

    /** 日志脱敏字段 */
    private List<String> sensitiveFields = List.of("password", "newPassword", "oldPassword", "oldPwd", "pwd");

    /** developer 是否跳过所有权限检查 (true=放行) */
    private boolean developerSkipAuth = true;
}
