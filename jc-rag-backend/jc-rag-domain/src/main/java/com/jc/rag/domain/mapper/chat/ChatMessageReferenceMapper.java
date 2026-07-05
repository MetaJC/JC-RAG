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
