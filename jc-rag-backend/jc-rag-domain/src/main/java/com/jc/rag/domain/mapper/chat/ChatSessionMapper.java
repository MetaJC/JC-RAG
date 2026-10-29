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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.chat.ChatSessionEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天会话 Mapper。
 * <p>对应 chat_session 表, 提供分页查询与按 user 过滤; 状态维护 (touch/close) 与所有权校验 (countOwned/selectByOwner) 走 XML。
 *
 * @author JC
 */
public interface ChatSessionMapper extends BaseMapper<ChatSessionEntity> {

    /** 按 userId + 关键词统计会话数 */
    long countSessions(@Param("userId") Long userId, @Param("keyword") String keyword);

    /** 按 userId + 关键词分页查询会话 (含知识库名称) */
    IPage<Map<String, Object>> pageSessions(IPage<Map<String, Object>> page,
                                            @Param("userId") Long userId,
                                            @Param("keyword") String keyword);

    /** 全量导出 (无分页) */
    List<Map<String, Object>> listSessions(@Param("userId") Long userId,
                                           @Param("keyword") String keyword);

    /**
     * 校验 session 是否属于指定 user 且状态为进行中.
     * @param userId    归属用户 ID
     * @param sessionId 会话 ID
     * @return 命中行数 (0 或 1)
     */
    int countOwned(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

    /**
     * 按 userId + sessionId 取会话详情 (供 RAG 请求组装 kb_id / model_mode / model_name).
     * @param userId    归属用户 ID
     * @param sessionId 会话 ID
     * @return 会话实体, 不存在或无权访问返回 null
     */
    ChatSessionEntity selectByOwner(@Param("userId") Long userId, @Param("sessionId") Long sessionId);

    /**
     * 刷新会话 updated_at.
     * @param sessionId 会话 ID
     * @return 影响行数
     */
    int touch(@Param("sessionId") Long sessionId);

    /**
     * 关闭会话 (status=0).
     * @param sessionId 会话 ID
     * @return 影响行数
     */
    int close(@Param("sessionId") Long sessionId);

    /**
     * 单条详情 (LEFT JOIN kb_knowledge_base, 返回 Map 供 Service 组装 VO).
     * @param id 会话 ID
     * @return 含 kbName 的会话字段 Map
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 更新标题 + updated_at.
     */
    int updateTitle(@Param("sessionId") Long sessionId, @Param("title") String title);

    /**
     * 更新置顶状态 + updated_at.
     */
    int updatePinned(@Param("sessionId") Long sessionId, @Param("isPinned") Integer isPinned);

    /**
     * 条件更新标题: 仅当 title 为空/默认值时才覆盖 (异步生成标题用).
     */
    int updateTitleIfBlank(@Param("sessionId") Long sessionId, @Param("title") String title);

    /**
     * 物理删除指定时间之前创建的 chat_session (L0 retention 后台任务用).
     * <p>注意: 调用方应先删 chat_message, 再删本表 (依赖关系).
     * <p>本方法不做级联, 由 Service 层编排顺序.
     * @param cutoff 时间阈值 (含)
     * @return 影响行数
     */
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
