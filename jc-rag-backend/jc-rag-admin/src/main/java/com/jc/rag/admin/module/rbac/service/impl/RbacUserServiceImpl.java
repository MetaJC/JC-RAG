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
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserRoleMapper;
import com.jc.rag.domain.mapper.rbac.RbacRolePermissionMapper;
import com.jc.rag.common.model.base.BaseServiceImpl;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import com.jc.rag.domain.entity.rbac.RbacUserRoleEntity;
import com.jc.rag.domain.entity.rbac.RbacRolePermissionEntity;
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
    private final RbacRolePermissionMapper rolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final RbacDefaultsProperties rbacDefaults;

    public RbacUserServiceImpl(AbstractRbacService rbac,
                           RbacUserRoleMapper userRoleMapper,
                           RbacRolePermissionMapper rolePermissionMapper,
                           PasswordEncoder passwordEncoder,
                           RbacDefaultsProperties rbacDefaults) {
        this.rbac = rbac;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
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
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = baseMapper.pageUsers(page, like, excludeDeveloper);
        page.setTotal(baseMapper.countUsers(like, excludeDeveloper));
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
        u.setEmail(param.getEmail());
        u.setUserType(param.getUserType());
        u.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        String pwd = param.getPassword();
        if (pwd == null || pwd.isEmpty()) pwd = rbacDefaults.getDefaultPassword();
        u.setPasswordHash(passwordEncoder.encode(pwd));
        save(u);
        if (param.getRoleIds() != null) {
            // P0 越权修复: 操作者分配的每个 role 已绑定的 perm 必须 ⊆ 操作者自身权限
            assertRolePermissionsWithinOperatorScope(param.getRoleIds());
            replaceUserRoles(u.getId(), param.getRoleIds());
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
        if (param.getEmail() != null) u.setEmail(param.getEmail());
        rbac.rejectReservedUserTypeChange(u.getUserType(), param.getUserType(), RbacConstants.OpType.UPDATE_USER);
        if (param.getUserType() != null) u.setUserType(param.getUserType());
        if (param.getStatus() != null) u.setStatus(param.getStatus());
        boolean ok = updateById(u);
        if (param.getRoleIds() != null) {
            // P0 越权修复: 分配的每个 role 已绑定的 perm 必须 ⊆ 操作者自身权限
            assertRolePermissionsWithinOperatorScope(param.getRoleIds());
            replaceUserRoles(u.getId(), param.getRoleIds());
        }
        return ok;
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

    /** Map → RbacUserVO 转换（适配 SqlMap userWithRoles resultMap） */
    private RbacUserVO toUserVO(Map<String, Object> row) {
        RbacUserVO v = new RbacUserVO();
        v.setId(toLong(row.get("id")));
        v.setUsername((String) row.get("username"));
        v.setNickname((String) row.get("nickname"));
        v.setPhone((String) row.get("phone"));
        v.setEmail((String) row.get("email"));
        v.setStatus(toInt(row.get("status")));
        v.setStatusText(rbac.statusText(v.getStatus()));
        v.setUserType((String) row.get("userType"));
        v.setUserTypeText(rbac.userTypeText(v.getUserType()));
        v.setCreatedAt(toLocalDateTime(row.get("createdAt")));
        v.setLastLoginTime(toLocalDateTime(row.get("lastLoginTime")));
        v.setLastLoginIp((String) row.get("lastLoginIp"));
        v.setRoleIds(splitIds(row.get("roleIds")));
        v.setRoleNames(splitNames(row.get("roleNames")));
        return v;
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
