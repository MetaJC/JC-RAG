package com.jc.rag.domain.mapper.knowledge;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbUserAuthEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 知识库-用户授权 Mapper。
 * @author JC
 */
public interface KbUserAuthMapper extends BaseMapper<KbUserAuthEntity> {

    /**
     * 按 kb 查全部授权
     */
    List<KbUserAuthEntity> listByKbId(@Param("kbId") Long kbId);

    /**
     * 按 kb + user 查单条
     */
    KbUserAuthEntity selectByKbAndUser(@Param("kbId") Long kbId, @Param("userId") Long userId);

    /**
     * 按 kb 删除全部授权 (全量替换用)
     */
    int deleteByKbId(@Param("kbId") Long kbId);

    /**
     * 批量 upsert (用于全量替换)
     */
    int batchUpsert(@Param("list") Collection<KbUserAuthEntity> list);

    /**
     * 查当前用户的授权
     */
    KbUserAuthEntity selectByKbAndUserId(@Param("kbId") Long kbId, @Param("userId") Long userId);

    /**
     * 按 kb + userIds 批量查 (IN 查询)
     */
    List<KbUserAuthEntity> listByKbAndUsers(@Param("kbId") Long kbId, @Param("userIds") List<Long> userIds);
}
