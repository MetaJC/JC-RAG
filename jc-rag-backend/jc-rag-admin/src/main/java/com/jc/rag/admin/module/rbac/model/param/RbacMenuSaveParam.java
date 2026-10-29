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


package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 菜单保存 Param
 * <p>permType 限定为菜单族: CATALOG (目录/容器) | MENU (菜单/叶子)
 * <p>其它类型 (BUTTON/API) 走 SysPermission 接口
 *
 * @author JC
 */
@Data
public class RbacMenuSaveParam {

    private Long parentId;

    /** 目录 CATALOG / 菜单 MENU，默认 MENU */
    private String permType;

    @NotBlank(message = "菜单编码不能为空")
    private String code;

    @NotBlank(message = "菜单标题不能为空")
    private String title;

    private String path;

    private String icon;

    private Integer sort;

    private String component;

    private Integer visible;
    private Integer isExternal;
    private Integer keepAlive;
    private String query;
    private Integer status;
}
