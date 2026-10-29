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


package com.jc.rag.admin.module.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import com.jc.rag.admin.common.enums.PermissionTypeEnum;
import com.jc.rag.admin.module.rbac.model.param.RbacMenuSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacMenuVO;
import com.jc.rag.admin.module.rbac.service.AbstractRbacService;
import com.jc.rag.admin.module.rbac.service.RbacMenuService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.domain.mapper.rbac.RbacPermissionMapper;
import com.jc.rag.domain.mapper.rbac.RbacRoleMapper;
import com.jc.rag.domain.mapper.rbac.RbacRolePermissionMapper;
import com.jc.rag.common.model.base.BaseServiceImpl;
import com.jc.rag.domain.entity.rbac.RbacPermissionEntity;
import com.jc.rag.domain.entity.rbac.RbacRolePermissionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单管理实现。
 * <p>实体为 {@link RbacPermissionEntity}，通过 permType=MENU 过滤。
 * <p>节点字段: id / parentId / title / code / path / icon / sort / component / visible / status。
 * <p>拼树规则: parentId=0L 或 null 视为根，按 sort 升序。
 *
 * @author JC
 */
@Slf4j
@Service
public class RbacMenuServiceImpl extends BaseServiceImpl<RbacPermissionMapper, RbacPermissionEntity>
        implements RbacMenuService {

    private final AbstractRbacService rbac;
    private final RbacRoleMapper sysRoleMapper;
    private final RbacRolePermissionMapper sysRolePermissionMapper;
    private final RbacDefaultsProperties rbacDefaults;

    public RbacMenuServiceImpl(AbstractRbacService rbac,
                           RbacRoleMapper sysRoleMapper,
                           RbacRolePermissionMapper sysRolePermissionMapper,
                           RbacDefaultsProperties rbacDefaults) {
        this.rbac = rbac;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.rbacDefaults = rbacDefaults;
    }

    @Override
    public Long currentUserId() {
        return rbac.currentUserId();
    }

    @Override
    public List<RbacMenuVO> listMenuTree() {
        // P0 越权修复: 菜单管理 tab 加载的菜单树按当前用户权限过滤
        // (developer / super_admin 仍可看全量, 普通系统管理员只看自己拥有 perm 的菜单)
        return listMenuTreeByUserId(rbac.currentUserId());
    }

    @Override
    public List<RbacMenuVO> listMenus() {
        return toMenuVOs(listActiveMenus());
    }

    @Override
    @Transactional
    public Long createMenu(RbacMenuSaveParam param) {
        RbacPermissionEntity p = new RbacPermissionEntity();
        p.setParentId(param.getParentId() == null ? 0L : param.getParentId());
        p.setPermCode(param.getCode());
        p.setPermName(param.getTitle());
        p.setPath(param.getPath());
        p.setIcon(param.getIcon());
        p.setSort(param.getSort() == null ? 0 : param.getSort());
        p.setComponent(param.getComponent());
        p.setVisible(param.getVisible() == null ? 1 : param.getVisible());
        p.setIsExternal(param.getIsExternal() == null ? 0 : param.getIsExternal());
        p.setKeepAlive(param.getKeepAlive() == null ? 0 : param.getKeepAlive());
        p.setQuery(param.getQuery());
        p.setPermType("MENU");
        p.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        save(p);
        // 新建菜单自动绑定 super_admin 角色 (保证超级可见，走全量鉴权不绕过)
        autoBindSuperAdmin(p.getId());
        return p.getId();
    }

    @Override
    @Transactional
    public boolean updateMenu(Long id, RbacMenuSaveParam param) {
        RbacPermissionEntity p = getById(id);
        if (p == null) {
            throw new BizException(ResultCode.NOT_FOUND, "菜单不存在: id=" + id);
        }
        // P0 越权修复: 非超管/developer 不允许操作自己没权限的菜单
        assertMenuWithinOperatorScope(id, "更新");
        if (!isMenuOrCatalog(p.getPermType())) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "该权限非菜单/目录类型, 不允许通过菜单接口修改: id=" + id);
        }
        if (param.getParentId() != null) {
            // 禁止将父级设为自身或自身的后代
            if (param.getParentId().equals(id)) {
                throw new BizException(ResultCode.PARAM_INVALID, "父级菜单不能为自身");
            }
            if (isDescendant(id, param.getParentId())) {
                throw new BizException(ResultCode.PARAM_INVALID, "父级菜单不能为自身的后代");
            }
            p.setParentId(param.getParentId());
        }
        if (param.getCode() != null) p.setPermCode(param.getCode());
        if (param.getTitle() != null) p.setPermName(param.getTitle());
        if (param.getPath() != null) p.setPath(param.getPath());
        if (param.getIcon() != null) p.setIcon(param.getIcon());
        if (param.getSort() != null) p.setSort(param.getSort());
        if (param.getComponent() != null) p.setComponent(param.getComponent());
        if (param.getVisible() != null) p.setVisible(param.getVisible());
        if (param.getStatus() != null) p.setStatus(param.getStatus());
        return updateById(p);
    }

    @Override
    @Transactional
    public boolean deleteMenu(Long id) {
        RbacPermissionEntity p = getById(id);
        if (p == null) {
            throw new BizException(ResultCode.NOT_FOUND, "菜单不存在: id=" + id);
        }
        if (!isMenuOrCatalog(p.getPermType())) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "该权限非菜单/目录类型, 不允许通过菜单接口删除: id=" + id);
        }
        // 有子菜单则拒绝
        long childCount = count(new QueryWrapper<RbacPermissionEntity>().eq("parent_id", id));
        if (childCount > 0) {
            throw new BizException(ResultCode.CONFLICT,
                    "菜单存在子节点, 请先删除子菜单: id=" + id);
        }
        // 级联清理角色-权限关联 (避免 sys_role_permission 悬空引用)
        sysRolePermissionMapper.delete(
            new QueryWrapper<RbacRolePermissionEntity>().eq("permission_id", id));
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean moveMenu(Long id, Long newParentId, Integer sort) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "菜单 id 不能为空");
        }
        RbacPermissionEntity p = getById(id);
        if (p == null) {
            throw new BizException(ResultCode.NOT_FOUND, "菜单不存在: id=" + id);
        }
        // P0 越权修复: 非超管/developer 不允许移动自己没权限的菜单
        assertMenuWithinOperatorScope(id, "移动");
        if (!isMenuOrCatalog(p.getPermType())) {
            throw new BizException(ResultCode.PARAM_INVALID,
                    "该权限非菜单/目录类型, 不允许移动: id=" + id);
        }
        long targetParent = (newParentId == null || newParentId == 0L) ? 0L : newParentId;
        // 防回环: 不能把节点挂到自身或自身的后代下
        if (targetParent == id) {
            throw new BizException(ResultCode.PARAM_INVALID, "父级菜单不能为自身");
        }
        if (targetParent != 0L && isDescendant(id, targetParent)) {
            throw new BizException(ResultCode.PARAM_INVALID, "父级菜单不能为自身的后代");
        }
        // 校验目标父级存在 + 类型合法
        if (targetParent != 0L) {
            RbacPermissionEntity parent = getById(targetParent);
            if (parent == null) {
                throw new BizException(ResultCode.NOT_FOUND, "目标父级菜单不存在: id=" + targetParent);
            }
            if (!"MENU".equalsIgnoreCase(parent.getPermType()) && !"CATALOG".equalsIgnoreCase(parent.getPermType())) {
                throw new BizException(ResultCode.PARAM_INVALID,
                        "目标父级不是菜单/目录类型, 不能作为父级: id=" + targetParent);
            }
        }
        p.setParentId(targetParent);
        if (sort != null) {
            p.setSort(sort);
        }
        boolean ok = updateById(p);
        if (!ok) {
            log.error("菜单移动失败: updateById 返回 false, id={}, newParent={}, sort={}", id, targetParent, sort);
            throw new BizException(ResultCode.FAIL, "菜单移动失败: id=" + id);
        }
        log.info("菜单移动成功: id={}, newParent={}, sort={}", id, targetParent, sort);
        return ok;
    }

    @Override
    public List<RbacMenuVO> listMenuTreeByUserId(Long userId) {
        if (userId == null) return List.of();
        // 用户 → 角色 → 权限 (filter 菜单族 CATALOG+MENU + status=1)
        List<String> permCodes = rbac.listUserPermCodes(userId);
        if (permCodes.isEmpty()) return List.of();
        List<RbacPermissionEntity> entities = this.list(
                new QueryWrapper<RbacPermissionEntity>()
                        .in("perm_type", "MENU", "CATALOG")
                        .eq("status", 1)
                        .in("perm_code", permCodes)
        );
        return buildTree(toMenuVOs(entities));
    }

    // ==================== 私有 ====================

    /** 新建菜单/接口时自动绑定 super_admin 角色 (幂等) */
    private void autoBindSuperAdmin(Long permissionId) {
        if (permissionId == null) return;
        String roleCode = rbacDefaults.getSuperAdminRoleCode();
        if (roleCode == null || roleCode.isBlank()) return;
        Long roleId = sysRoleMapper.selectIdByRoleCode(roleCode);
        if (roleId == null) {
            log.warn("super_admin 角色不存在 (roleCode={}), 跳过自动绑定 permissionId={}", roleCode, permissionId);
            return;
        }
        // 幂等: 已存在则跳过
        Long exists = sysRolePermissionMapper.selectCount(
            new QueryWrapper<RbacRolePermissionEntity>()
                .eq("role_id", roleId)
                .eq("permission_id", permissionId));
        if (exists != null && exists > 0) return;
        RbacRolePermissionEntity binding = new RbacRolePermissionEntity();
        binding.setRoleId(roleId);
        binding.setPermissionId(permissionId);
        binding.setDataScope(1);
        sysRolePermissionMapper.insert(binding);
    }

    /**
     * 校验当前操作者能否管理指定菜单 (P0 越权修复点 3 - 菜单层).
     * <p>规则: 目标菜单 perm_id 必须在操作者 perm 范围内, 否则抛 BizException(FORBIDDEN).
     * <p>developer / super_admin 跳过.
     *
     * @param menuId 菜单 perm_id
     * @param action 操作类型 (更新/删除/移动, 用于报错文案)
     */
    private void assertMenuWithinOperatorScope(Long menuId, String action) {
        if (menuId == null) return;
        if (rbac.isDeveloper() || rbac.isSuperAdmin()) return;
        java.util.List<Long> opPerms = rbac.listUserPermissionIds(rbac.currentUserId());
        if (!opPerms.contains(menuId)) {
            throw new BizException(ResultCode.FORBIDDEN,
                    "您没有权限" + action + "该菜单: id=" + menuId + " (不在自身权限范围内)");
        }
    }

    /** 取所有启用的菜单 (含目录) */
    private List<RbacPermissionEntity> listActiveMenus() {
        return this.list(
                new QueryWrapper<RbacPermissionEntity>()
                        .in("perm_type", "MENU", "CATALOG")
                        .orderByAsc("sort", "id")
        );
    }

    /** 实体 → VO */
    private List<RbacMenuVO> toMenuVOs(List<RbacPermissionEntity> all) {
        List<RbacMenuVO> result = new ArrayList<>(all.size());
        for (RbacPermissionEntity p : all) {
            RbacMenuVO v = new RbacMenuVO();
            v.setId(p.getId());
            v.setParentId(p.getParentId() == null ? 0L : p.getParentId());
            v.setCode(p.getPermCode());
            v.setTitle(p.getPermName());
            v.setPath(p.getPath());
            v.setIcon(p.getIcon());
            v.setSort(p.getSort() == null ? 0 : p.getSort());
            v.setComponent(p.getComponent());
            v.setVisible(p.getVisible() == null ? 1 : p.getVisible());
            v.setIsExternal(p.getIsExternal() == null ? 0 : p.getIsExternal());
            v.setKeepAlive(p.getKeepAlive() == null ? 0 : p.getKeepAlive());
            v.setQuery(p.getQuery());
            v.setStatus(p.getStatus());
            v.setStatusText(rbac.statusText(p.getStatus()));
            v.setChildren(new ArrayList<>());
            result.add(v);
        }
        return result;
    }

    /** 拼树: parentId=0/根 视为一级 */
    private List<RbacMenuVO> buildTree(List<RbacMenuVO> all) {
        Map<Long, List<RbacMenuVO>> byParent = all.stream()
                .collect(Collectors.groupingBy(RbacMenuVO::getParentId));
        for (RbacMenuVO node : all) {
            List<RbacMenuVO> children = byParent.get(node.getId());
            if (children != null) {
                children.sort(Comparator.comparing(RbacMenuVO::getSort,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                node.setChildren(children);
            }
        }
        List<RbacMenuVO> roots = byParent.getOrDefault(0L, new ArrayList<>());
        roots.sort(Comparator.comparing(RbacMenuVO::getSort,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return roots;
    }

    /** 判断 targetId 是否为 sourceId 的后代 (防回环) */
    private boolean isDescendant(Long sourceId, Long targetId) {
        if (targetId == null || targetId == 0L) return false;
        Long cursor = targetId;
        int guard = 0;
        while (cursor != null && cursor != 0L && guard++ < 32) {
            if (sourceId.equals(cursor)) return true;
            RbacPermissionEntity p = getById(cursor);
            if (p == null) return false;
            cursor = p.getParentId();
        }
        return false;
    }

    /** 是否为菜单或目录类型 (这两个类型都允许通过菜单接口增删改) */
    private boolean isMenuOrCatalog(String permType) {
        return "MENU".equalsIgnoreCase(permType) || "CATALOG".equalsIgnoreCase(permType);
    }
}
