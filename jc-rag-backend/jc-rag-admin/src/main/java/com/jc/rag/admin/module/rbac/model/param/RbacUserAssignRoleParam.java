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

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户分配角色请求参数。
 * <p>仅传 roleIds，不触发 RbacUserSaveParam 的非空校验。
 * <p>语义为覆盖式：传入的 roleIds 即为最终绑定关系。
 *
 * @author JC
 */
@Data
public class RbacUserAssignRoleParam {

    /** 角色 ID 列表。空集合表示清空该用户全部角色。 */
    @NotNull(message = "角色列表不能为空")
    private List<Long> roleIds;
}
