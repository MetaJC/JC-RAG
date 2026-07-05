package com.jc.rag.domain.mapper.chat;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.chat.UserMemoryProfileEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 用户跨会话画像 Mapper (L3)。
 * <p>对应 user_memory_profile 表, 基础 CRUD 走 MyBatis-Plus BaseMapper, 业务查询走 XML。
 *
 * @author JC
 */
public interface UserMemoryProfileMapper extends BaseMapper<UserMemoryProfileEntity> {

    /**
     * 按 userId 取本用户最新一条画像 (无则返回 null)。
     * @param userId 用户 ID
     * @return 画像实体; 无则返回 null
     */
    UserMemoryProfileEntity selectByUserId(@Param("userId") Long userId);
}
