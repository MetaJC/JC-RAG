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

import lombok.Data;

/**
 * 菜单拖拽移动参数 (前端 el-tree @node-drop 调用)
 * <p>parentId=0 / null 表示移到根目录
 *
 * @author JC
 */
@Data
public class RbacMenuMoveParam {

    /** 新父级 id; 0 或 null 表示移到根 */
    private Long parentId;

    /** 拖入目标位置后的 sort (调用方按 drop position 计算, 可选) */
    private Integer sort;
}
