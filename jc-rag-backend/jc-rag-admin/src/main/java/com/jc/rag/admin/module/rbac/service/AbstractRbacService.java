/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.admin.module.rbac.service;

import com.jc.rag.admin.common.enums.PermissionTypeEnum;
import com.jc.rag.admin.common.enums.StatusEnum;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import com.jc.rag.admin.common.enums.UserTypeEnum;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.exception.DeveloperGuardException;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.admin.common.util.MapConvertUtil;
import com.jc.rag.domain.mapper.rbac.RbacPermissionMapper;
import com.jc.rag.domain.mapper.rbac.RbacRoleMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.domain.entity.rbac.RbacRoleEntity;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RBAC 业务支撑组件。
 * <p>提供当前登录用户、保留账号校验、枚举文本解析等业务级辅助方法。
 * <p>仅作为 Spring Bean 注入使用；业务 ServiceImpl 通过组合方式（构造器注入）使用本类。
 *
 * @author JC
 */
@Component
@RequiredArgsConstructor
public class AbstractRbacService {

    protected final RbacDefaultsProperties rbacDefaults;
    private final RbacPermissionMapper sysPermissionMapper;
    private final RbacRoleMapper sysRoleMapper;
    private final RbacUserMapper sysUserMapper;

    /** 获取当前登录用户 */
    public DbUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof DbUserDetails u)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return u;
    }

    /** 当前操作人 ID */
    public Long currentUserId() {
        return currentUser().getUserId();
    }

    /** 当前操作人用户名 */
    public String currentUsername() {
        return currentUser().getUsername();
    }

    /** 判断用户名是否为保留账号 (默认 developer) */
    public boolean isReservedUsername(String username) {
        return rbacDefaults.getDeveloperUsername().equalsIgnoreCase(username);
    }

    /** 判断用户类型是否为保留类型 */
    public boolean isReservedUserType(String userType) {
        if (userType == null) return false;
        return rbacDefaults.getReservedUserTypes().stream()
                .anyMatch(t -> t.equalsIgnoreCase(userType));
    }

    /** 拒绝任何对保留账号的修改 */
    public void rejectReservedMutation(String targetUsername, String action) {
        if (isReservedUsername(targetUsername)) {
            throw new DeveloperGuardException(
                rbacDefaults.getDeveloperUsername() + " 账号为系统保留账号, 不允许" + action);
        }
    }

    /** 拒绝任何对保留账号类型的修改 */
    public void rejectReservedUserTypeChange(String currentUserType, String newUserType, String action) {
        if (isReservedUserType(currentUserType) && !isReservedUserType(newUserType)) {
            throw new DeveloperGuardException(
                "保留账号类型 " + currentUserType + " 不允许变更为 " + newUserType + " (" + action + ")");
        }
    }

    /** 解析字符串 id 列表 (前端 "1,2,3" -> List<Long>) */
    public List<Long> parseIds(String csv) {
        return MapConvertUtil.parseIdList(csv);
    }

    /** 复制 Map 中指定 key */
    public void copyFields(Map<String, Object> src, Map<String, Object> dst, String... keys) {
        MapConvertUtil.copyProp(src, dst, keys);
    }

    /** 状态 0/1 -> 中文 */
    public String statusText(Object code) {
        if (code == null) return "";
        try {
            return StatusEnum.textOf(
                code instanceof Number n ? n.intValue() : Integer.parseInt(code.toString()));
        } catch (Exception e) {
            return String.valueOf(code);
        }
    }

    /** 用户类型 -> 中文 */
    public String userTypeText(Object code) {
        if (code == null) return "";
        return UserTypeEnum.of(code.toString()).getDescription();
    }

    /** 权限类型 -> 中文 */
    public String permissionTypeText(Object code) {
        if (code == null) return "";
        return PermissionTypeEnum.of(code.toString()).getDescription();
    }

    /** 当前用户是否为 developer (跳过鉴权) */
    public boolean isDeveloper() {
        try {
            return rbacDefaults.getDeveloperUsername().equalsIgnoreCase(currentUsername());
        } catch (Exception e) {
            return false;
        }
    }

    /** 当前用户是否为超级管理员 (默认拥有所有权限, 但仍走鉴权流程) */
    public boolean isSuperAdmin() {
        try {
            return rbacDefaults.getSuperAdminUsername().equalsIgnoreCase(currentUsername());
        } catch (Exception e) {
            return false;
        }
    }

    /** 当前用户是否对指定 id 执行了自操作 (删自己/改自己权限) - 阻止自残 */
    public void assertNotSelfOperation(Long targetUserId, String action) {
        if (targetUserId != null && targetUserId.equals(currentUserId())) {
            throw new com.jc.rag.common.exception.BizException(
                com.jc.rag.common.enums.ResultCode.FORBIDDEN, "不允许对当前登录用户执行" + action);
        }
    }

    /** 按用户 id 查全部 permCode (委托 RbacPermissionMapper，去重) */
    public List<String> listUserPermCodes(Long userId) {
        if (userId == null) return List.of();
        return sysPermissionMapper.findPermCodesByUserId(userId);
    }

    /**
     * 按用户 id 查全部 perm_id (去重). 用于"操作者权限 ⊇ 目标权限" 校验.
     * <p>对应 RbacPermissionMapper.findPermissionIdsByUserId.
     *
     * @param userId 用户 ID
     * @return perm_id 集合
     */
    public List<Long> listUserPermissionIds(Long userId) {
        if (userId == null) return List.of();
        return sysPermissionMapper.findPermissionIdsByUserId(userId);
    }

    /**
     * 校验操作者能否分配/编辑给定的 perm 集合 (P0 越权修复点 1).
     * <p>规则: 给定 perm 集合 ⊆ 操作者拥有的 perm 集合, 否则抛 BizException(FORBIDDEN).
     * <p>豁免:
     * <ul>
     *   <li>developer 跳过 (项目规约, 跳过所有权限校验)</li>
     *   <li>super_admin 用户跳过 (超管可分配任何 perm, 跟现有 isAdmin=1 角色绑定全量 perm 行为对齐)</li>
     * </ul>
     * <p>使用场景:
     * <ul>
     *   <li>创建/更新角色时校验 permissionIds</li>
     *   <li>删除角色时校验目标角色已绑定的 perm (防止越权破坏数据)</li>
     *   <li>给用户分配角色时校验每个目标角色绑定的 perm</li>
     * </ul>
     *
     * @param operatorId    操作者用户 ID
     * @param targetPermIds 目标 perm_id 集合 (创建/更新角色时是 param.permissionIds, 删除/分配时是目标角色已绑定)
     */
    public void assertCanAssignPermissions(Long operatorId, Collection<Long> targetPermIds) {
        if (targetPermIds == null || targetPermIds.isEmpty()) return;
        if (isDeveloper()) return;
        if (isSuperAdmin()) return;
        Set<Long> operatorPerms = new HashSet<>(listUserPermissionIds(operatorId));
        List<Long> exceed = new ArrayList<>();
        for (Long pid : targetPermIds) {
            if (pid == null) continue;
            if (!operatorPerms.contains(pid)) exceed.add(pid);
        }
        if (!exceed.isEmpty()) {
            throw new BizException(ResultCode.FORBIDDEN,
                    "您没有权限分配/编辑菜单权限 ID: " + exceed + " (超出自身权限范围)");
        }
    }

    /**
     * 校验操作者能否管理目标角色 (P0 越权修复点 2).
     * <p>规则: 目标角色已绑定的 perm 集合 ⊆ 操作者拥有的 perm 集合.
     * <p>实现: 等价于 assertCanAssignPermissions (复用).
     *
     * @param operatorId    操作者用户 ID
     * @param targetPermIds 目标角色已绑定的 perm_id 集合 (空集合表示无任何权限, 总能管理)
     */
    public void assertCanEditRole(Long operatorId, List<Long> targetPermIds) {
        assertCanAssignPermissions(operatorId, targetPermIds);
    }

    /**
     * 校验不能操作 super_admin 角色 (P0 越权修复点 1 - 角色层).
     * <p>规则: 任何人对 super_admin 角色做变更/删除 都被拒绝, developer 跳过.
     * <p>理由: super_admin 角色代表"全权限", 改动会破坏全平台, 必须锁死.
     *
     * @param targetRoleId 目标角色 ID
     * @param action       操作类型 (更新/删除/分配权限等, 用于报错文案)
     */
    public void assertNotOperatingSuperAdminRole(Long targetRoleId, String action) {
        if (targetRoleId == null) return;
        if (isDeveloper()) return;
        String superCode = rbacDefaults.getSuperAdminRoleCode();
        if (superCode == null) return;
        RbacRoleEntity entity = sysRoleMapper.selectById(targetRoleId);
        if (entity != null && superCode.equalsIgnoreCase(entity.getRoleCode())) {
            throw new BizException(ResultCode.FORBIDDEN,
                    "超级管理员角色 (" + superCode + ") 是系统保留角色, 不允许" + action);
        }
    }

    /**
     * 校验不能操作 super_admin 用户 (P0 越权修复点 1 - 用户层).
     * <p>规则: 任何人对 super_admin 用户做变更/删除/重置密码 都被拒绝, developer 跳过.
     * <p>理由: super_admin 用户代表"系统最高权限账号", 改动会锁死全平台, 必须保留给 developer.
     *
     * @param targetUserId 目标用户 ID
     * @param action       操作类型
     */
    public void assertNotOperatingSuperAdminUser(Long targetUserId, String action) {
        if (targetUserId == null) return;
        if (isDeveloper()) return;
        String superUsername = rbacDefaults.getSuperAdminUsername();
        if (superUsername == null) return;
        String targetUsername = sysUserMapper.selectUsernameById(targetUserId);
        if (targetUsername != null && superUsername.equalsIgnoreCase(targetUsername)) {
            throw new BizException(ResultCode.FORBIDDEN,
                    "超级管理员账号 (" + superUsername + ") 是系统保留账号, 不允许" + action);
        }
    }

    /**
     * 校验不能创建 roleCode=super_admin 的新角色 (防绕过).
     * <p>理由: 即使现有 super_admin 角色被锁死, 攻击者可以创建同 code 的新角色来冒充.
     *
     * @param newRoleCode 待创建角色的 roleCode
     */
    public void assertNotCreatingSuperAdminRole(String newRoleCode) {
        if (newRoleCode == null) return;
        if (isDeveloper()) return;
        String superCode = rbacDefaults.getSuperAdminRoleCode();
        if (superCode != null && superCode.equalsIgnoreCase(newRoleCode)) {
            throw new BizException(ResultCode.FORBIDDEN,
                    "roleCode=" + superCode + " 是系统保留, 不允许创建同名角色");
        }
    }

    /**
     * 判断当前用户是否应该隐藏 super_admin 角色 (列表过滤用).
     * <p>规则: developer + super_admin 用户可见, 其他用户隐藏.
     */
    public boolean shouldHideSuperAdmin() {
        if (isDeveloper()) return false;
        if (isSuperAdmin()) return false;
        return true;
    }

    /**
     * 校验当前用户是否有权操作顶级部门 (parentId=0 或 自身是顶级).
     * <p>规则: 仅 developer 与 super_admin 可操作顶级部门, 其他用户拒绝.
     * <p>用途: 部门树的根节点是组织最顶层, 误改会破坏整个架构, 需限制为系统级账号.
     */
    public void assertCanEditRootDepartment() {
        if (isDeveloper()) return;
        if (isSuperAdmin()) return;
        throw new BizException(ResultCode.FORBIDDEN,
                "顶级部门仅 developer / super_admin 可操作");
    }
}
