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


package com.jc.rag.user.module.chat.service;

import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.user.module.chat.model.param.UserChatSessionQueryParam;
import com.jc.rag.user.module.chat.model.vo.AskResponseVO;
import com.jc.rag.user.module.chat.model.vo.ChatMessageVO;
import com.jc.rag.user.module.chat.model.vo.ChatSessionVO;
import com.jc.rag.user.module.chat.model.vo.KbOptionVO;
import com.jc.rag.user.module.chat.model.vo.KbSelectConfigVO;
import com.jc.rag.user.module.chat.model.vo.ModelInfoVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * C 端问答中心 Service 接口。
 * <p>所有方法返回严格类型的 VO, 禁止使用 {@code Map<String,Object>}。
 * <p>SSE 流式接口因协议特殊, 仍直接返回 {@link SseEmitter} (Controller 层不包装 R)。
 *
 * @author JC
 */
public interface UserChatService {

    List<ChatSessionVO> listSessions(Long userId);

    PageResponse<ChatSessionVO> listSessions(Long userId, UserChatSessionQueryParam query);

    ChatSessionVO getSession(Long id);

    @Transactional
    ChatSessionVO createSession(Long userId, String title, Long kbId);

    @Transactional
    ChatSessionVO renameSession(Long userId, Long sessionId, String sessionTitle);

    @Transactional
    ChatSessionVO pinSession(Long userId, Long sessionId, boolean pinned);

    List<ChatMessageVO> listMessages(Long userId, Long sessionId);

    @Transactional
    AskResponseVO ask(Long userId, Long sessionId, String question, Long agentId, Long customAgentId);

    /**
     * SSE 流式接口: attachments 仍保留 Map 形态, 因为是从外部 JSON 直接透传, 不做 DTO 化.
     * <p>customAgentId 与 agentId 互斥: 同时存在时优先 customAgentId (因为 customAgent 是 user 层, 不冲突).
     * <p>注意: 这里的 customAgentId 不会传给 Python 用来加载系统 prompt, 而是查 user_custom_agent 表取 content 作为 user 层后缀.
     */
    SseEmitter askStream(Long userId, Long sessionId, String question, boolean deepThinking,
                         Long agentId, Long customAgentId, Long overrideKbId,
                         List<Map<String, Object>> attachments);

    @Transactional
    BaseOpResultVO deleteSession(Long userId, Long sessionId);

    List<KbOptionVO> listDefaultKnowledgeBases();

    /** C 端知识库选择配置: 含 mode + options */
    KbSelectConfigVO getKbSelectConfig();

    ModelInfoVO getModelInfo(Long userId);
}
