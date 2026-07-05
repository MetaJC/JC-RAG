package com.jc.rag.domain.mapper.chat;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.chat.UserDialogueEvaluationEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话评估记录 Mapper.
 * <p>对应 user_dialogue_evaluation 表.</p>
 *
 * @author JC
 */
public interface UserDialogueEvaluationMapper extends BaseMapper<UserDialogueEvaluationEntity> {

    /**
     * 按 userId 查询全部评估记录, 倒序.
     */
    List<UserDialogueEvaluationEntity> selectByUserId(@Param("userId") Long userId,
                                                       @Param("limit") Integer limit);

    /**
     * 统计用户在指定时间窗口内的评估次数 (用于每日配额).
     */
    int countByUserIdAndWindow(@Param("userId") Long userId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    /**
     * 管理端: 全部评估记录 (最新在前, 支持分页).
     */
    List<UserDialogueEvaluationEntity> selectAllPaged(@Param("offset") int offset,
                                                      @Param("limit") int limit);
}
