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


package com.jc.rag.user.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.domain.entity.rag.RagAgentEntity;
import com.jc.rag.domain.mapper.rag.RagAgentMapper;
import com.jc.rag.common.web.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端 Agent 列表 Controller。
 * <p>仅返回已发布 (status=1) 且 isSystem=0 的预置用户智能体 (后台创建, 所有 C 端用户可见可选)。
 * <p>系统智能体 (isSystem=1) 对 C 端完全不可见, 由后台设置默认后, 后端在用户提问时强制使用, 前端无感知。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/user/agents")
public class UserAgentController {

    private final RagAgentMapper agentMapper;

    public UserAgentController(RagAgentMapper m) {
        this.agentMapper = m;
    }

    @GetMapping
    public R<List<RagAgentEntity>> listPublished() {
        return R.ok(agentMapper.selectList(
            new QueryWrapper<RagAgentEntity>()
                .eq("status", 1)
                .eq("is_system", 0)
                .orderByDesc("id")));
    }
}
