package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacPermissionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 权限 Mapper。
 * <p>同时服务于 Menu/Permission 两个 Service。
 *
 * @author JC
 */
public interface RbacPermissionMapper extends BaseMapper<RbacPermissionEntity> {

    long countPermissions(@Param("permType") String permType,
                          @Param("keyword") String keyword);

    IPage<Map<String, Object>> pagePermissions(IPage<Map<String, Object>> page,
                                               @Param("permType") String permType,
                                               @Param("keyword") String keyword);

    List<Map<String, Object>> listPermissions(@Param("permType") String permType,
                                              @Param("keyword") String keyword);

    /**
     * 查指定用户拥有的全部 permCode（去重）。
     * <p>链路：user → user_role → role → role_permission → permission。
     * <p>用于登录时把 permCode 注入 DbUserDetails.authorities，
     * 运行时由 {@code PermissionInterceptor} 做 hasAuthority 校验。
     *
     * @param userId 用户 ID
     * @return permCode 集合（按 perm_code 字典序）
     */
    List<String> findPermCodesByUserId(@Param("userId") Long userId);

    /**
     * 查指定用户拥有的全部 perm_id (去重).
     * <p>链路: user → user_role → role → role_permission → permission.
     * <p>用于: 角色/用户管理时的"操作者权限 ⊇ 目标权限"校验 (AbstractRbacService.assertCanAssignPermissions).
     *
     * @param userId 用户 ID
     * @return perm_id 集合
     */
    List<Long> findPermissionIdsByUserId(@Param("userId") Long userId);
}
