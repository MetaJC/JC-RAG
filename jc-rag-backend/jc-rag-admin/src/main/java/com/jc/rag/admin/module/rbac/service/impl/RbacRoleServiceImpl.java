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
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacRoleVO;
import com.jc.rag.admin.module.rbac.service.AbstractRbacService;
import com.jc.rag.admin.module.rbac.service.RbacRoleService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.exception.DataConflictException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.domain.mapper.rbac.RbacRoleMapper;
import com.jc.rag.domain.mapper.rbac.RbacRolePermissionMapper;
import com.jc.rag.common.model.base.BaseServiceImpl;
import com.jc.rag.domain.entity.rbac.RbacRoleEntity;
import com.jc.rag.domain.entity.rbac.RbacRolePermissionEntity;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色管理实现。
 * <p>继承通用 BaseServiceImpl 获得 MP 全部 CRUD；本类只写角色管理特有业务。
 * <p>多表 SQL（role_permission 关联）直接通过 {@link RbacRolePermissionMapper} 操作。
 *
 * @author JC
 */
@Slf4j
@Service
public class RbacRoleServiceImpl extends BaseServiceImpl<RbacRoleMapper, RbacRoleEntity> implements RbacRoleService {

    private final AbstractRbacService rbac;
    private final RbacRolePermissionMapper rolePermissionMapper;
    private final RbacDefaultsProperties rbacDefaults;

    public RbacRoleServiceImpl(AbstractRbacService rbac,
                               RbacRolePermissionMapper rolePermissionMapper,
                               RbacDefaultsProperties rbacDefaults) {
        this.rbac = rbac;
        this.rolePermissionMapper = rolePermissionMapper;
        this.rbacDefaults = rbacDefaults;
    }

    @Override
    public long countRoles() {
        return this.count();
    }

    @Override
    public PageResponse<RbacRoleVO> pageListRoles(RbacRoleQueryParam query) {
        String like = buildLike(query.getKeyword());
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = baseMapper.pageRoles(page, like);
        page.setTotal(baseMapper.countRoles(like));
        // P0 越权修复: 非超管/developer 隐藏 super_admin 角色
        boolean hideSuper = rbac.shouldHideSuperAdmin();
        if (!hideSuper) {
            return PageHelper.toPageResponse(page, this::toRoleVO);
        }
        // 过滤 + 重建响应
        List<RbacRoleVO> filtered = page.getRecords().stream()
                .map(this::toRoleVO)
                .filter(v -> !rbacDefaults.getSuperAdminRoleCode().equalsIgnoreCase(v.getRoleCode()))
                .collect(Collectors.toList());
        return PageResponse.of(filtered, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    @Transactional
    public Long createRole(RbacRoleSaveParam param) {
        RbacRoleEntity exists = getOne(new QueryWrapper<RbacRoleEntity>().eq("role_name", param.getRoleName()));
        if (exists != null) {
            throw new DataConflictException("rbac.role.duplicate.name", param.getRoleName());
        }
        RbacRoleEntity r = new RbacRoleEntity();
        r.setRoleName(param.getRoleName());
        r.setRoleCode(param.getRoleCode());
        r.setRoleDesc(param.getRoleDesc());
        r.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        // 是否允许登录管理端: super_admin 强制 1, 其他角色按入参 (默认 0)
        r.setIsAdmin(resolveIsAdminForWrite(param.getRoleCode(), param.getIsAdmin(), null));
        // P0 越权修复: 禁止创建 roleCode=super_admin 的角色 (防冒充)
        rbac.assertNotCreatingSuperAdminRole(param.getRoleCode());
        save(r);
        if (param.getPermissionIds() != null) {
            // P0 越权修复: 操作者分配的 perm 必须 ⊆ 操作者自身权限
            rbac.assertCanAssignPermissions(rbac.currentUserId(), param.getPermissionIds());
            replaceRolePermissions(r.getId(), param.getPermissionIds());
        }
        return r.getId();
    }

    @Override
    @Transactional
    public boolean updateRole(Long id, RbacRoleSaveParam param) {
        RbacRoleEntity r = getById(id);
        if (r == null) throw new ResourceNotFoundException("rbac.role.notfound", id);
        if (param.getRoleName() != null) r.setRoleName(param.getRoleName());
        if (param.getRoleCode() != null) r.setRoleCode(param.getRoleCode());
        if (param.getRoleDesc() != null) r.setRoleDesc(param.getRoleDesc());
        if (param.getStatus() != null) r.setStatus(param.getStatus());
        // isAdmin 字段: 走统一校验 (super_admin 强校验, developer 账号除外)
        if (param.getIsAdmin() != null || r.getIsAdmin() == null) {
            r.setIsAdmin(resolveIsAdminForWrite(r.getRoleCode(), param.getIsAdmin(), r.getIsAdmin()));
        }
        boolean ok = updateById(r);
        if (param.getPermissionIds() != null) {
            replaceRolePermissions(r.getId(), param.getPermissionIds());
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean deleteRole(Long id) {
        RbacRoleEntity r = getById(id);
        if (r == null) throw new ResourceNotFoundException("rbac.role.notfound", id);
        // P0 越权修复: 禁止删除 super_admin 角色
        rbac.assertNotOperatingSuperAdminRole(id, "删除");
        // P0 越权修复: 删角色前校验"目标角色已绑定的 perm ⊆ 操作者权限" (防止删除超出自身权限的角色)
        rbac.assertCanAssignPermissions(rbac.currentUserId(), findPermissionIdsByRoleId(id));
        // 先清关联, 再删主表 (顺序很重要: 否则外键悬挂)
        rolePermissionMapper.delete(new QueryWrapper<RbacRolePermissionEntity>().eq("role_id", id));
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        RbacRoleEntity r = getById(id);
        if (r == null) throw new ResourceNotFoundException("rbac.role.notfound", id);
        // P0 越权修复: 禁止修改 super_admin 角色状态
        rbac.assertNotOperatingSuperAdminRole(id, "修改状态");
        r.setStatus(status);
        return updateById(r);
    }

    @Override
    @Transactional
    public boolean replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new QueryWrapper<RbacRolePermissionEntity>().eq("role_id", roleId));
        if (permissionIds == null || permissionIds.isEmpty()) return true;
        for (Long pid : permissionIds) {
            RbacRolePermissionEntity rp = new RbacRolePermissionEntity();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
        return true;
    }

    @Override
    public List<Long> findPermissionIdsByRoleId(Long roleId) {
        return rolePermissionMapper.selectList(new QueryWrapper<RbacRolePermissionEntity>().eq("role_id", roleId))
                .stream().map(RbacRolePermissionEntity::getPermissionId).toList();
    }

    private String buildLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) return null;
        return "%" + keyword + "%";
    }

    private RbacRoleVO toRoleVO(Map<String, Object> row) {
        RbacRoleVO v = new RbacRoleVO();
        v.setId(toLong(row.get("id")));
        v.setRoleCode((String) row.get("roleCode"));
        v.setRoleName((String) row.get("roleName"));
        v.setRoleDesc((String) row.get("roleDesc"));
        v.setStatus(toInt(row.get("status")));
        v.setStatusText(rbac.statusText(v.getStatus()));
        v.setIsAdmin(toInt(row.get("isAdmin")));
        Object createdAt = row.get("createdAt");
        if (createdAt instanceof java.time.LocalDateTime t) v.setCreatedAt(t);
        else if (createdAt instanceof java.sql.Timestamp ts) v.setCreatedAt(ts.toLocalDateTime());
        v.setPermissionIds(splitIds(row.get("permissionIds")));
        return v;
    }

    /**
     * 计算 is_admin 字段落库值 (统一强校验入口)。
     * <p>规则:
     * <ul>
     *   <li>roleCode = super_admin: 强制 1, 拒绝任何 0 入参 (developer 账号除外)</li>
     *   <li>其他角色: 入参 null -> 取 currentValue -> 仍 null 兜底 0; 入参非空按入参</li>
     * </ul>
     */
    private Integer resolveIsAdminForWrite(String roleCode, Integer incoming, Integer currentValue) {
        boolean isSuperAdmin = roleCode != null
                && rbacDefaults.getSuperAdminRoleCode().equalsIgnoreCase(roleCode);
        if (!isSuperAdmin) {
            if (incoming != null) return incoming;
            if (currentValue != null) return currentValue;
            return 0;
        }
        // super_admin 角色: developer 账号不受限, 任何入参都接受
        if (rbac.isDeveloper()) {
            return incoming != null ? incoming : 1;
        }
        // 非 developer: 强制 1, 拒绝改为 0
        if (incoming != null && incoming == 0) {
            throw new BizException(ResultCode.FORBIDDEN,
                    "超级管理员角色 (super_admin) 不允许关闭'允许登录后台'开关");
        }
        return 1;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }

    private static List<Long> splitIds(Object o) {
        if (o == null) return List.of();
        String s = o.toString();
        if (s.isEmpty()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty())
                .map(Long::valueOf).collect(Collectors.toList());
    }
}
