package com.jc.rag.domain.mapper.chat;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.chat.ChatMessageEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息 Mapper。
 * <p>对应 chat_message 表, 基础 CRUD 走 MyBatis-Plus BaseMapper, 业务查询走 XML。
 *
 * @author JC
 */
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {

    /**
     * 按 sessionId 升序列出全部消息 (含 message_index 排序).
     * @param sessionId 会话 ID
     * @return 消息字段 Map 列表 (key 为字段名)
     */
    List<Map<String, Object>> listBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 取会话内最大 message_index, 无消息时返回 0.
     * @param sessionId 会话 ID
     * @return 当前最大序号, 无消息返回 0
     */
    int getMaxIndex(@Param("sessionId") Long sessionId);

    /**
     * 物理删除指定时间之前创建的 chat_message (L0 retention 后台任务用).
     * <p>不可逆; 建议分批执行避免长事务, 由 Service 层控制批次大小.
     * @param cutoff 时间阈值 (含, 小于等于此时间的消息被删除)
     * @return 影响行数
     */
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
