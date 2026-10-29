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
import com.jc.rag.admin.common.constants.RbacConstants;
import com.jc.rag.admin.module.rbac.model.param.RbacUserQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacUserVO;
import com.jc.rag.admin.module.rbac.service.AbstractRbacService;
import com.jc.rag.admin.module.rbac.service.RbacUserService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.domain.mapper.rbac.RbacDepartmentMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserDepartmentMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserRoleMapper;
import com.jc.rag.domain.mapper.rbac.RbacRolePermissionMapper;
import com.jc.rag.common.model.base.BaseServiceImpl;
import com.jc.rag.domain.entity.rbac.RbacDepartmentEntity;
import com.jc.rag.domain.entity.rbac.RbacUserDepartmentEntity;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import com.jc.rag.domain.entity.rbac.RbacUserPositionEntity;
import com.jc.rag.domain.entity.rbac.RbacUserRoleEntity;
import com.jc.rag.domain.entity.rbac.RbacRolePermissionEntity;
import com.jc.rag.domain.mapper.rbac.RbacPositionMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserPositionMapper;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理实现。
 * <p>继承通用 BaseServiceImpl 获得 MP 全部 CRUD；本类只写用户管理特有业务。
 * <p>RBAC 辅助方法通过组合（注入 AbstractRbacService）使用，而非继承，
 * 为后续把 Role/Permission 等模块也迁到 BaseServiceImpl 铺路。
 *
 * @author JC
 */
@Slf4j
@Service
public class RbacUserServiceImpl extends BaseServiceImpl<RbacUserMapper, RbacUserEntity> implements RbacUserService {

    private final AbstractRbacService rbac;
    private final RbacUserRoleMapper userRoleMapper;
    private final RbacUserDepartmentMapper userDepartmentMapper;
    private final RbacUserPositionMapper userPositionMapper;
    private final RbacRolePermissionMapper rolePermissionMapper;
    private final RbacDepartmentMapper rbacDepartmentMapper;
    private final RbacPositionMapper rbacPositionMapper;
    private final PasswordEncoder passwordEncoder;
    private final RbacDefaultsProperties rbacDefaults;

    public RbacUserServiceImpl(AbstractRbacService rbac,
                           RbacUserRoleMapper userRoleMapper,
                           RbacUserDepartmentMapper userDepartmentMapper,
                           RbacUserPositionMapper userPositionMapper,
                           RbacRolePermissionMapper rolePermissionMapper,
                           RbacDepartmentMapper rbacDepartmentMapper,
                           RbacPositionMapper rbacPositionMapper,
                           PasswordEncoder passwordEncoder,
                           RbacDefaultsProperties rbacDefaults) {
        this.rbac = rbac;
        this.userRoleMapper = userRoleMapper;
        this.userDepartmentMapper = userDepartmentMapper;
        this.userPositionMapper = userPositionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.rbacDepartmentMapper = rbacDepartmentMapper;
        this.rbacPositionMapper = rbacPositionMapper;
        this.passwordEncoder = passwordEncoder;
        this.rbacDefaults = rbacDefaults;
    }

    @Override
    public long countUsers() {
        return this.count();
    }

    @Override
    public PageResponse<RbacUserVO> pageListUsers(RbacUserQueryParam query) {
        String like = buildLike(query.getKeyword());
        boolean excludeDeveloper = !rbac.isDeveloper();
        // P0 越权修复: 非超管/developer 隐藏 super_admin 用户
        boolean hideSuper = rbac.shouldHideSuperAdmin();
        // 部门过滤: deptId > 0 时, 递归展开到所有后代部门 ID
        Long deptId = query.getDeptId();
        List<Long> descendantDeptIds = null;
        if (deptId != null && deptId > 0) {
            descendantDeptIds = userDepartmentMapper.selectDescendantDepartmentIds(deptId);
            if (descendantDeptIds == null || descendantDeptIds.isEmpty()) {
                // 选中的部门不存在, 返回空列表
                return PageResponse.of(List.of(), 0L, query.getPageNum(), query.getPageSize());
            }
        }
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = baseMapper.pageUsers(page, like, excludeDeveloper, deptId, descendantDeptIds);
        page.setTotal(baseMapper.countUsers(like, excludeDeveloper, deptId, descendantDeptIds));
        List<RbacUserVO> all = new ArrayList<>(page.getRecords().size());
        for (Map<String, Object> row : page.getRecords()) {
            all.add(toUserVO(row));
        }
        if (!hideSuper) {
            return PageResponse.of(all, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
        }
        String superUsername = rbacDefaults.getSuperAdminUsername();
        List<RbacUserVO> filtered = all.stream()
                .filter(v -> v.getUsername() == null || !superUsername.equalsIgnoreCase(v.getUsername()))
                .collect(Collectors.toList());
        return PageResponse.of(filtered, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    @Transactional
    public Long createUser(RbacUserSaveParam param) {
        rbac.rejectReservedMutation(param.getUsername(), RbacConstants.OpType.CREATE_USER);
        RbacUserEntity u = new RbacUserEntity();
        u.setUsername(param.getUsername());
        u.setNickname(param.getNickname());
        u.setRealName(param.getRealName() == null ? "" : param.getRealName().trim());
        u.setEmail(param.getEmail());
        u.setUserType(param.getUserType());
        u.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        u.setDepartmentId(param.getDepartmentId());
        u.setGender(normalizeGender(param.getGender()));
        String pwd = param.getPassword();
        if (pwd == null || pwd.isEmpty()) pwd = rbacDefaults.getDefaultPassword();
        u.setPasswordHash(passwordEncoder.encode(pwd));
        save(u);
        if (param.getDepartmentId() != null) {
            // 同步写入 rbac_user_department 关联表, 主部门标记
            replaceUserPrimaryDepartment(u.getId(), param.getDepartmentId());
        }
        if (param.getRoleIds() != null) {
            // P0 越权修复: 操作者分配的每个 role 已绑定的 perm 必须 ⊆ 操作者自身权限
            assertRolePermissionsWithinOperatorScope(param.getRoleIds());
            replaceUserRoles(u.getId(), param.getRoleIds());
        }
        if (param.getPositionIds() != null) {
            replaceUserPositions(u.getId(), param.getPositionIds());
        }
        return u.getId();
    }

    @Override
    @Transactional
    public boolean updateUser(Long id, RbacUserSaveParam param) {
        RbacUserEntity u = getById(id);
        if (u == null) throw new ResourceNotFoundException("rbac.user.notfound", id);
        rbac.rejectReservedMutation(u.getUsername(), RbacConstants.OpType.UPDATE_USER);
        // P0 越权修复: 禁止更新 super_admin 用户
        rbac.assertNotOperatingSuperAdminUser(id, "更新");
        if (param.getNickname() != null) u.setNickname(param.getNickname());
        if (param.getRealName() != null) u.setRealName(param.getRealName().trim());
        if (param.getEmail() != null) u.setEmail(param.getEmail());
        rbac.rejectReservedUserTypeChange(u.getUserType(), param.getUserType(), RbacConstants.OpType.UPDATE_USER);
        if (param.getUserType() != null) u.setUserType(param.getUserType());
        if (param.getStatus() != null) u.setStatus(param.getStatus());
        // 部门变更: 非 null 时同步主部门 (不传视为保持不变; 传 null 在 controller 决定)
        if (param.getDepartmentId() != null) {
            u.setDepartmentId(param.getDepartmentId());
            replaceUserPrimaryDepartment(id, param.getDepartmentId());
        }
        if (param.getGender() != null) u.setGender(normalizeGender(param.getGender()));
        boolean ok = updateById(u);
        if (param.getRoleIds() != null) {
            // P0 越权修复: 分配的每个 role 已绑定的 perm 必须 ⊆ 操作者自身权限
            assertRolePermissionsWithinOperatorScope(param.getRoleIds());
            replaceUserRoles(u.getId(), param.getRoleIds());
        }
        if (param.getPositionIds() != null) {
            replaceUserPositions(id, param.getPositionIds());
        }
        return ok;
    }

    /** 性别字段归一化: 非法值视为 0 (未知) */
    private static Integer normalizeGender(Integer v) {
        if (v == null) return 0;
        if (v == 1 || v == 2) return v;
        return 0;
    }

    /** 性别枚举 → 文本 (0=未知 1=男 2=女) */
    public static String genderText(Integer v) {
        if (v == null) return "未知";
        return switch (v) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    /**
     * 替换用户主部门关联 (只清主部门标记, 不删所有).
     * <p>当前阶段仅支持主部门 (单一), 后续如需多部门兼职, 走 replaceUserDepartments 批量.
     */
    private void replaceUserPrimaryDepartment(Long userId, Long newDeptId) {
        // 清空旧的主部门
        userDepartmentMapper.delete(new QueryWrapper<RbacUserDepartmentEntity>().eq("user_id", userId));
        if (newDeptId == null) return;
        RbacUserDepartmentEntity ud = new RbacUserDepartmentEntity();
        ud.setUserId(userId);
        ud.setDepartmentId(newDeptId);
        ud.setIsPrimary(1);
        userDepartmentMapper.insert(ud);
    }

    @Override
    @Transactional
    public boolean deleteUser(Long id) {
        RbacUserEntity u = getById(id);
        if (u == null) throw new ResourceNotFoundException("rbac.user.notfound", id);
        rbac.rejectReservedMutation(u.getUsername(), RbacConstants.OpType.DELETE_USER);
        // P0 越权修复: 禁止删除 super_admin 用户
        rbac.assertNotOperatingSuperAdminUser(id, "删除");
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        RbacUserEntity u = getById(id);
        if (u == null) throw new ResourceNotFoundException("rbac.user.notfound", id);
        rbac.rejectReservedMutation(u.getUsername(), RbacConstants.OpType.UPDATE_USER);
        // P0 越权修复: 禁止修改 super_admin 用户状态
        rbac.assertNotOperatingSuperAdminUser(id, "修改状态");
        u.setStatus(status);
        return updateById(u);
    }

    @Override
    @Transactional
    public boolean resetPassword(Long id, String newPassword) {
        RbacUserEntity u = getById(id);
        if (u == null) throw new ResourceNotFoundException("rbac.user.notfound", id);
        rbac.rejectReservedMutation(u.getUsername(), RbacConstants.OpType.RESET_PASSWORD);
        // P0 越权修复: 禁止重置 super_admin 密码
        rbac.assertNotOperatingSuperAdminUser(id, "重置密码");
        if (newPassword == null || newPassword.isEmpty()) {
            newPassword = rbacDefaults.getDefaultPassword();
        }
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        return updateById(u);
    }

    @Override
    @Transactional
    public boolean replaceUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new QueryWrapper<RbacUserRoleEntity>().eq("user_id", userId));
        if (roleIds == null || roleIds.isEmpty()) return true;
        for (Long rid : roleIds) {
            RbacUserRoleEntity ur = new RbacUserRoleEntity();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            userRoleMapper.insert(ur);
        }
        return true;
    }

    @Override
    public List<Long> findRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectList(new QueryWrapper<RbacUserRoleEntity>().eq("user_id", userId))
                .stream().map(RbacUserRoleEntity::getRoleId).toList();
    }

    @Override
    public List<Long> findUserIdsByRoleId(Long roleId) {
        return userRoleMapper.selectUserIdsByRoleId(roleId);
    }

    @Override
    @Transactional
    public boolean assignUsersToRole(Long roleId, List<Long> userIds) {
        // P0 越权修复: 禁止给 super_admin 角色分配用户 (防冒充超管)
        rbac.assertNotOperatingSuperAdminRole(roleId, "分配用户");
        // P0 越权修复: 操作者必须能管理目标 role (等价于"目标 role 的 perm ⊆ 操作者 perm")
        rbac.assertCanAssignPermissions(rbac.currentUserId(), findRolePermissionIds(roleId));
        userRoleMapper.deleteByRoleId(roleId);
        if (userIds == null || userIds.isEmpty()) return true;
        List<RbacUserRoleEntity> list = new ArrayList<>(userIds.size());
        for (Long uid : userIds) {
            RbacUserRoleEntity ur = new RbacUserRoleEntity();
            ur.setRoleId(roleId);
            ur.setUserId(uid);
            list.add(ur);
        }
        userRoleMapper.insertBatch(list);
        return true;
    }

    @Override
    public List<Long> findUserIdsByDepartmentId(Long departmentId) {
        if (departmentId == null) return List.of();
        return userDepartmentMapper.selectUserIdsByDepartmentIdRecursive(departmentId);
    }

    @Override
    public List<java.util.Map<String, Object>> listUsersByDepartmentId(Long departmentId) {
        if (departmentId == null) return List.of();
        // 单级直接关联 (不递归, 不含子部门用户), 用于"部门管理→显示部门用户"开关
        return baseMapper.listUsersByDepartmentIdDirectly(departmentId);
    }

    @Override
    public RbacUserVO getUserDetail(Long userId) {
        Map<String, Object> row = baseMapper.selectUserDetailById(userId);
        if (row == null || row.isEmpty()) {
            throw new ResourceNotFoundException("rbac.user.notfound", userId);
        }
        RbacUserVO v = toUserVO(row);
        v.setCreatedAt(toLocalDateTime(getStr(row, "createdAt", "created_at")));
        v.setGenderText(genderText(v.getGender()));
        return v;
    }

    @Override
    @Transactional
    public boolean assignUsersToDepartment(Long departmentId, List<Long> userIds) {
        if (departmentId == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "部门 id 不能为空");
        }
        // 部门存在性校验
        RbacDepartmentEntity dept = rbacDepartmentMapper.selectById(departmentId);
        if (dept == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", departmentId);
        }
        // 计算部门树 (含自身 + 所有后代) 的 id 集合
        List<Long> scopeIds = userDepartmentMapper.selectDescendantDepartmentIds(departmentId);
        if (scopeIds == null || scopeIds.isEmpty()) return true;

        // 1) 覆盖式: 先清空 scope 内所有用户对 scope 内所有部门的旧关联
        //    (不限于 userIds; 否则取消勾选不生效)
        userDepartmentMapper.delete(
                new QueryWrapper<RbacUserDepartmentEntity>().in("department_id", scopeIds));

        // 2) userIds 为空 = 仅清空, 无需重建
        if (userIds == null || userIds.isEmpty()) {
            return true;
        }

        // 3) 按 userIds 重建关联, 全部挂在 target 部门上, is_primary=0 (不抢用户主部门)
        List<RbacUserDepartmentEntity> list = new ArrayList<>(userIds.size());
        for (Long uid : userIds) {
            if (uid == null) continue;
            // 校验用户存在性 + 跳过系统保留账号
            RbacUserEntity u = getById(uid);
            if (u == null) continue;
            String uname = u.getUsername();
            if ("developer".equalsIgnoreCase(uname) || "super_admin".equalsIgnoreCase(uname)) {
                // 系统保留账号不允许通过部门分配加入; 跳过
                continue;
            }
            RbacUserDepartmentEntity ud = new RbacUserDepartmentEntity();
            ud.setUserId(uid);
            ud.setDepartmentId(departmentId);
            ud.setIsPrimary(0);
            list.add(ud);
        }
        if (!list.isEmpty()) {
            userDepartmentMapper.insertBatch(list);
        }
        return true;
    }

    @Override
    public List<Long> findUserIdsByPositionId(Long positionId) {
        if (positionId == null) return List.of();
        return userPositionMapper.selectUserIdsByPositionId(positionId);
    }

    @Override
    @Transactional
    public boolean assignUsersToPosition(Long positionId, List<Long> userIds) {
        if (positionId == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "岗位 id 不能为空");
        }
        // 岗位存在性校验
        if (rbacPositionMapper.selectById(positionId) == null) {
            throw new ResourceNotFoundException("rbac.position.notfound", positionId);
        }
        // 1) 覆盖式: 先清空该岗位所有旧关联
        userPositionMapper.deleteByPositionId(positionId);
        // 2) 空集合 = 仅清空
        if (userIds == null || userIds.isEmpty()) {
            return true;
        }
        // 3) 按 userIds 重建关联 (is_primary=0, 由具体业务按需变更)
        List<RbacUserPositionEntity> list = new ArrayList<>(userIds.size());
        for (Long uid : userIds) {
            if (uid == null) continue;
            RbacUserEntity u = getById(uid);
            if (u == null) continue;
            String uname = u.getUsername();
            if ("developer".equalsIgnoreCase(uname) || "super_admin".equalsIgnoreCase(uname)) {
                continue;
            }
            RbacUserPositionEntity up = new RbacUserPositionEntity();
            up.setUserId(uid);
            up.setPositionId(positionId);
            up.setIsPrimary(0);
            list.add(up);
        }
        if (!list.isEmpty()) {
            userPositionMapper.insertBatch(list);
        }
        return true;
    }

    @Override
    public List<Long> findPositionIdsByUserId(Long userId) {
        if (userId == null) return List.of();
        return userPositionMapper.selectPositionIdsByUserId(userId);
    }

    @Override
    @Transactional
    public boolean replaceUserPositions(Long userId, List<Long> positionIds) {
        if (userId == null) return false;
        userPositionMapper.deleteByUserId(userId);
        if (positionIds == null || positionIds.isEmpty()) return true;
        List<RbacUserPositionEntity> list = new ArrayList<>(positionIds.size());
        for (Long pid : positionIds) {
            if (pid == null) continue;
            // 校验岗位存在
            if (rbacPositionMapper.selectById(pid) == null) continue;
            RbacUserPositionEntity up = new RbacUserPositionEntity();
            up.setUserId(userId);
            up.setPositionId(pid);
            up.setIsPrimary(0);
            list.add(up);
        }
        if (!list.isEmpty()) {
            userPositionMapper.insertBatch(list);
        }
        return true;
    }

    /**
     * 给当前操作者一批 roleId, 校验这些 role 已绑定的 perm 必须全部在操作者自身 perm 范围内.
     * <p>等价于: 对每个 roleId, 累加其 permissionIds, 再走 {@link AbstractRbacService#assertCanAssignPermissions}.
     * <p>规则: developer / super_admin 跳过.
     */
    private void assertRolePermissionsWithinOperatorScope(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return;
        if (rbac.isDeveloper() || rbac.isSuperAdmin()) return;
        java.util.Set<Long> all = new java.util.HashSet<>();
        for (Long rid : roleIds) {
            if (rid == null) continue;
            all.addAll(findRolePermissionIds(rid));
        }
        if (all.isEmpty()) return;
        rbac.assertCanAssignPermissions(rbac.currentUserId(), new java.util.ArrayList<>(all));
    }

    /** 查单个 role 的全部 permissionId (去重) */
    private List<Long> findRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectList(
                new QueryWrapper<RbacRolePermissionEntity>().eq("role_id", roleId))
                .stream().map(RbacRolePermissionEntity::getPermissionId).collect(Collectors.toList());
    }

    /** 关键字 %xxx% 包装，null/空直接返回 null */
    private String buildLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) return null;
        return "%" + keyword + "%";
    }

    /**
     * Map → RbacUserVO 转换。
     * <p>key 与 SQL 别名一致 (下划线), 由 pageUsers / listUsers 直接返回 LinkedHashMap 提供.
     * <p>附 fallback: 若 SQL 别名未来又变成驼峰, 仍能兼容 (history-proof).
     */
    private RbacUserVO toUserVO(Map<String, Object> row) {
        RbacUserVO v = new RbacUserVO();
        v.setId(toLong(getStr(row, "id", "id")));
        v.setUsername(getStr(row, "username", "username"));
        v.setNickname(getStr(row, "nickname", "nickname"));
        v.setRealName(getStr(row, "realName", "real_name"));
        v.setPhone(getStr(row, "phone", "phone"));
        v.setEmail(getStr(row, "email", "email"));
        v.setStatus(toInt(getStr(row, "status", "status")));
        v.setStatusText(rbac.statusText(v.getStatus()));
        v.setUserType(getStr(row, "userType", "user_type"));
        v.setUserTypeText(rbac.userTypeText(v.getUserType()));
        v.setGender(toInt(getStr(row, "gender", "gender")));
        v.setGenderText(genderText(v.getGender()));
        v.setDepartmentId(toLong(getStr(row, "departmentId", "department_id")));
        v.setDepartmentName(getStr(row, "departmentName", "department_name"));
        v.setCreatedAt(toLocalDateTime(getStr(row, "createdAt", "created_at")));
        v.setLastLoginTime(toLocalDateTime(getStr(row, "lastLoginTime", "last_login_time")));
        v.setLastLoginIp(getStr(row, "lastLoginIp", "last_login_ip"));
        v.setRoleIds(splitIds(getStr(row, "roleIds", "role_ids")));
        v.setRoleNames(splitNames(getStr(row, "roleNames", "role_names")));
        v.setPositionIds(splitIds(getStr(row, "positionIds", "position_ids")));
        v.setPositionNames(splitNames(getStr(row, "positionNames", "position_names")));
        return v;
    }

    /**
     * Map 取值 (兼容驼峰与下划线 key).
     * <p>先按驼峰 key 取, 为 null 时 fallback 到下划线 key.
     */
    private static String getStr(Map<String, Object> row, String camel, String snake) {
        Object v = row.get(camel);
        if (v == null) v = row.get(snake);
        return v == null ? null : v.toString();
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

    private static java.time.LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof java.time.LocalDateTime t) return t;
        if (o instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    /** GROUP_CONCAT 字符串 "1,2,3" → List&lt;Long&gt; */
    private static List<Long> splitIds(Object o) {
        if (o == null) return List.of();
        String s = o.toString();
        if (s.isEmpty()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty())
                .map(Long::valueOf).collect(Collectors.toList());
    }

    /** GROUP_CONCAT 字符串 "a,b,c" → List&lt;String&gt; */
    private static List<String> splitNames(Object o) {
        if (o == null) return List.of();
        String s = o.toString();
        if (s.isEmpty()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }
}
