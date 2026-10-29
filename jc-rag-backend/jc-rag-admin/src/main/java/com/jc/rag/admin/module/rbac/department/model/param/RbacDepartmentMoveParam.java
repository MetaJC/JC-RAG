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


package com.jc.rag.admin.module.rbac.department.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 部门拖拽结构变更 Param。
 * <p>position:
 * <ul>
 *   <li>before - 变成 target 的同级, 排在 target 之前</li>
 *   <li>after  - 变成 target 的同级, 排在 target 之后</li>
 *   <li>inner  - 变成 target 的子级, 排在同级最后</li>
 * </ul>
 *
 * @author JC
 */
@Data
public class RbacDepartmentMoveParam {

    /**
     * 目标部门 id (拖拽释放位置所在的行 id)
     */
    @NotNull
    private Long targetId;

    /**
     * 释放位置: before / after / inner
     */
    @NotBlank
    private String position;
}
