package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacRoleEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 角色 Mapper。
 *
 * @author JC
 */
public interface RbacRoleMapper extends BaseMapper<RbacRoleEntity> {

    long countRoles(@Param("keyword") String keyword);

    /**
     * 分页查询角色 (含权限聚合)。
     * <p>调用方需关闭 searchCount, 用 {@link #countRoles} 手动设置 total。
     */
    IPage<Map<String, Object>> pageRoles(IPage<Map<String, Object>> page,
                                         @Param("keyword") String keyword);

    /** 全量导出 (无分页)。 */
    List<Map<String, Object>> listRoles(@Param("keyword") String keyword);

    Long selectIdByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 统计用户拥有的 is_admin=1 且启用的角色数量。
     * <p>登录管理端准入校验: 至少 1 个, developer 用户绕过此检查。
     */
    long countAdminRoleByUserId(@Param("userId") Long userId);
}
