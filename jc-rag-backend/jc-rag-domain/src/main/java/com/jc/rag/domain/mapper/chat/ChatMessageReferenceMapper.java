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
import com.jc.rag.domain.entity.chat.ChatMessageReferenceEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息引用 Mapper。
 * <p>对应 chat_message_reference 表, 单条 INSERT 由 BaseMapper 提供, 批量走 insertBatch。
 *
 * @author JC
 */
public interface ChatMessageReferenceMapper extends BaseMapper<ChatMessageReferenceEntity> {

    /**
     * 批量插入引用, 单条失败不影响其他行。
     * @param refs 引用实体列表 (messageId 必填)
     * @return 实际写入行数
     */
    int insertBatch(@Param("list") List<ChatMessageReferenceEntity> refs);
}
