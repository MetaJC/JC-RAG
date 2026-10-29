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


package com.jc.rag.domain.mapper.chat;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.chat.ChatSessionSummaryEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 会话摘要 Mapper。
 * <p>对应 chat_session_summary 表, 基础 CRUD 走 MyBatis-Plus BaseMapper, 业务查询走 XML。
 *
 * @author JC
 */
public interface ChatSessionSummaryMapper extends BaseMapper<ChatSessionSummaryEntity> {

    /**
     * 取本会话最新一条有效摘要 (status=1), 用于注入 LLM 调用.
     * @param sessionId 会话 ID
     * @return 最新有效摘要; 无则返回 null
     */
    ChatSessionSummaryEntity selectLatestActive(@Param("sessionId") Long sessionId);

    /**
     * 把本会话所有 status=1 的摘要置为 status=0 (失效).
     * <p>同会话触发新一轮摘要前调用, 避免旧摘要与新摘要冲突.
     * @param sessionId 会话 ID
     * @return 影响行数
     */
    int invalidateAllActive(@Param("sessionId") Long sessionId);

    /**
     * 物理删除指定时间之前创建的 chat_session_summary (L0 retention 后台任务用).
     * <p>建议先于 chat_message 物理删除 (摘要独立于消息, 可先行清理).
     * @param cutoff 时间阈值 (含)
     * @return 影响行数
     */
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
