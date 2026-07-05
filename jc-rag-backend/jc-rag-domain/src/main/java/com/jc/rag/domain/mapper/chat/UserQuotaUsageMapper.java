package com.jc.rag.domain.mapper.chat;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.chat.UserQuotaUsageEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户当日用量 Mapper。
 *
 * @author JC
 */
public interface UserQuotaUsageMapper extends BaseMapper<UserQuotaUsageEntity> {

    /** 查询某用户当日用量 */
    List<Map<String, Object>> findByUserAndDate(@Param("userId") Long userId,
                                                @Param("usageDate") LocalDate usageDate);

    /** 自增 1 次 + 累加 token (ON DUPLICATE KEY UPDATE) */
    int upsertIncrement(@Param("userId") Long userId,
                        @Param("usageDate") LocalDate usageDate,
                        @Param("tokensUsed") Integer tokensUsed);
}
