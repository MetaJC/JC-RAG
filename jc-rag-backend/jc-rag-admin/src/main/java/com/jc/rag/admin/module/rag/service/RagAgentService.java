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


package com.jc.rag.admin.module.rag.service;

import com.jc.rag.admin.module.rag.model.param.RagAgentQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagAgentSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagAgentVO;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;

import java.util.List;
import java.util.Map;

/**
 * Agent Service。
 * @author JC
 */
public interface RagAgentService {

    PageResponse<RagAgentVO> pageList(RagAgentQueryParam query);

    List<RagAgentVO> listAll();

    RagAgentVO getById(Long id);

    Long create(RagAgentSaveParam param, Long userId);

    /**
     * 更新 Agent
     * @return true=成功 false=失败
     */
    boolean update(Long id, RagAgentSaveParam param);

    /**
     * 发布 Agent
     * @return true=成功 false=失败
     */
    boolean publish(Long id);

    /**
     * 下线 Agent
     * @return true=成功 false=失败
     */
    boolean unpublish(Long id);

    /**
     * 删除 Agent
     * @return true=成功 false=失败
     */
    boolean delete(Long id);

    /** 兼容旧前端：直接返回 Map 列表 (statusText/skillIds 等已在 Service 拼好) */
    PageResponse<Map<String, Object>> pageListAsMap(String keyword, String status, PageRequest pageRequest);

    /**
     * 设置系统默认 Agent (全局唯一, 覆盖已有).
     * <p>校验: Agent 必须存在且 status=1 (已发布).
     * @return true=成功
     */
    boolean setDefault(Long agentId);

    /**
     * 取消默认 Agent (清空 default_agent_id 配置).
     */
    boolean unsetDefault();

    /**
     * 查当前默认 Agent ID (无默认返回 null).
     */
    Long getDefaultId();
}
