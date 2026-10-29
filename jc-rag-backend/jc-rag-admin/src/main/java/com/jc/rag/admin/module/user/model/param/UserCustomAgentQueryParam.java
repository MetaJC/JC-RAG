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


package com.jc.rag.admin.module.user.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin 端用户智能体分页查询参数.
 * <p>支持: 智能体名/内容/用户名/昵称 模糊 + 状态过滤 + 时间范围 + 精确 userId.
 * @author JC
 */
@Data
public class UserCustomAgentQueryParam extends PageQueryParam {
    /** 关键词: 命中 username / nickname / agent_name / content */
    private String keyword;
    /** 1=启用 0=禁用 null=全部 */
    private Integer status;
    /** 精确 userId 过滤 */
    private Long userId;
    /** 创建时间下界 (含) */
    private LocalDateTime dateStart;
    /** 创建时间上界 (含) */
    private LocalDateTime dateEnd;
}
