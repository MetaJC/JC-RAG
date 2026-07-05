package com.jc.rag.domain.mapper.rbac;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacRolePermissionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-权限关联 Mapper。
 *
 * @author JC
 */
public interface RbacRolePermissionMapper extends BaseMapper<RbacRolePermissionEntity> {

    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    int deleteByRoleId(@Param("roleId") Long roleId);

    int insertBatch(@Param("list") List<RbacRolePermissionEntity> list);
}
