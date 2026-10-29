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


package com.jc.rag.user.module.chat.model.vo;

import lombok.Data;

import java.util.List;

/**
 * C 端知识库选择配置 VO。
 * <p>三种模式:
 * <ul>
 *   <li>FIXED: 超管设置了默认 KB, 前端不展示下拉, options 仅有默认项</li>
 *   <li>SELECTABLE: 允许客户端从公开 KB 中自行选择, options 列出全部 clientSelectable=1 的 PUBLIC KB</li>
 *   <li>EMPTY: 没有任何可用 KB</li>
 * </ul>
 *
 * @author JC
 */
@Data
public class KbSelectConfigVO {

    /** FIXED / SELECTABLE / EMPTY */
    private String mode;

    /** 当前默认 KB (FIXED 时为该 KB, SELECTABLE 时为 null) */
    private KbOptionVO defaultKb;

    /** 可选项 (FIXED 时为 [defaultKb], SELECTABLE 时为多选, EMPTY 为空) */
    private List<KbOptionVO> options;
}
