package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacUserRoleEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-角色关联 Mapper。
 *
 * @author JC
 */
public interface RbacUserRoleMapper extends BaseMapper<RbacUserRoleEntity> {

    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 按角色 ID 查绑定该角色的用户 ID 列表 */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    int deleteByUserId(@Param("userId") Long userId);

    /** 按角色 ID 解除全部用户绑定 */
    int deleteByRoleId(@Param("roleId") Long roleId);

    int insertBatch(@Param("list") List<RbacUserRoleEntity> list);
}
