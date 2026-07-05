package com.jc.rag.domain.mapper.knowledge;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbRoleAuthEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 知识库-角色授权 Mapper。
 * @author JC
 */
public interface KbRoleAuthMapper extends BaseMapper<KbRoleAuthEntity> {

    /**
     * 按 kb 查全部授权
     */
    List<KbRoleAuthEntity> listByKbId(@Param("kbId") Long kbId);

    /**
     * 按 kb + role 查单条
     */
    KbRoleAuthEntity selectByKbAndRole(@Param("kbId") Long kbId, @Param("roleId") Long roleId);

    /**
     * 按 kb 删除全部授权 (全量替换用)
     */
    int deleteByKbId(@Param("kbId") Long kbId);

    /**
     * 批量 upsert (用于全量替换)
     */
    int batchUpsert(@Param("list") Collection<KbRoleAuthEntity> list);

    /**
     * 查当前用户所有相关 role 的授权最大级别 (含 union all 或 max)
     */
    List<KbRoleAuthEntity> listByKbAndRoles(@Param("kbId") Long kbId, @Param("roleIds") List<Long> roleIds);
}
