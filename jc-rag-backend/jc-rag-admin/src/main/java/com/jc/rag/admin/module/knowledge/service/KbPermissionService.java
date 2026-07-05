package com.jc.rag.admin.module.knowledge.service;

import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import com.jc.rag.admin.common.enums.UserTypeEnum;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.domain.entity.knowledge.KbKnowledgeBaseEntity;
import com.jc.rag.domain.entity.knowledge.KbRoleAuthEntity;
import com.jc.rag.domain.entity.knowledge.KbUserAuthEntity;
import com.jc.rag.domain.enums.knowledge.KbPermLevelEnum;
import com.jc.rag.domain.enums.knowledge.VisibilityScopeEnum;
import com.jc.rag.domain.mapper.knowledge.KbKnowledgeBaseMapper;
import com.jc.rag.domain.mapper.knowledge.KbRoleAuthMapper;
import com.jc.rag.domain.mapper.knowledge.KbUserAuthMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库权限服务。
 * <p>集中处理: 解析当前用户对某知识库的最大权限级别 / 可见性范围变更校验 / 列表过滤 / 客户端选择开关校验。
 * <p>规则总览:
 * <ul>
 *   <li>developer 用户跳过一切鉴权, 视为 MANAGE</li>
 *   <li>超管 (用户名 = superAdminUsername) 视为 MANAGE, 但仍走鉴权流程</li>
 *   <li>非 ADMIN (NORMAL) 用户不可对管理端 KB 执行任何操作</li>
 *   <li>PRIVATE: 仅所有者具备 MANAGE, 其他人 NONE</li>
 *   <li>INTERNAL: 所有人可 VIEW, 操作权限取 (角色授权 max 用户授权)</li>
 *   <li>PUBLIC: 所有人可 VIEW, 操作权限取 (角色授权 max 用户授权)</li>
 * </ul>
 *
 * @author JC
 */
@Service
@RequiredArgsConstructor
public class KbPermissionService {

    private final RbacDefaultsProperties rbacDefaults;
    private final KbKnowledgeBaseMapper kbKnowledgeBaseMapper;
    private final KbRoleAuthMapper kbRoleAuthMapper;
    private final KbUserAuthMapper kbUserAuthMapper;
    private final RbacUserMapper rbacUserMapper;

    /** 当前用户对指定 KB 的最大权限级别 */
    public KbPermLevelEnum resolveMyPermLevel(KbKnowledgeBaseEntity kb, DbUserDetails user, List<Long> roleIds) {
        if (kb == null || user == null) {
            return KbPermLevelEnum.NONE;
        }
        if (isDeveloper(user)) {
            return KbPermLevelEnum.MANAGE;
        }
        if (isSuperAdmin(user)) {
            return KbPermLevelEnum.MANAGE;
        }
        if (!isAdminUser(user)) {
            return KbPermLevelEnum.NONE;
        }
        VisibilityScopeEnum scope = VisibilityScopeEnum.parse(kb.getVisibilityScope());
        if (scope == VisibilityScopeEnum.PRIVATE) {
            if (kb.getOwnerUserId() != null && kb.getOwnerUserId().equals(user.getUserId())) {
                return KbPermLevelEnum.MANAGE;
            }
            return KbPermLevelEnum.NONE;
        }
        // INTERNAL / PUBLIC: 基础 VIEW + 角色/用户授权 max
        KbPermLevelEnum base = KbPermLevelEnum.VIEW;
        KbPermLevelEnum fromRole = resolveRolePermLevel(kb.getId(), roleIds);
        KbPermLevelEnum fromUser = resolveUserPermLevel(kb.getId(), user.getUserId());
        return KbPermLevelEnum.max(base, KbPermLevelEnum.max(fromRole, fromUser));
    }

    /** 角色侧授权级别: 取该用户所属全部 role 的 max */
    private KbPermLevelEnum resolveRolePermLevel(Long kbId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return KbPermLevelEnum.NONE;
        }
        List<KbRoleAuthEntity> rows = kbRoleAuthMapper.listByKbAndRoles(kbId, roleIds);
        return rows.stream()
                .map(r -> KbPermLevelEnum.ofCode(r.getPermLevel()))
                .reduce(KbPermLevelEnum.NONE, KbPermLevelEnum::max);
    }

    /** 用户侧授权级别 */
    private KbPermLevelEnum resolveUserPermLevel(Long kbId, Long userId) {
        KbUserAuthEntity row = kbUserAuthMapper.selectByKbAndUserId(kbId, userId);
        return row == null ? KbPermLevelEnum.NONE : KbPermLevelEnum.ofCode(row.getPermLevel());
    }

    /** 校验当前用户对指定 KB 至少具备给定级别 */
    public void assertPerm(KbKnowledgeBaseEntity kb, DbUserDetails user, List<Long> roleIds, KbPermLevelEnum need) {
        KbPermLevelEnum actual = resolveMyPermLevel(kb, user, roleIds);
        if (!actual.covers(need)) {
            throw new AssertException(ResultCode.FORBIDDEN,
                    "无权限操作知识库, 需要 " + need.getText() + " 及以上权限");
        }
    }

    /**
     * 可见性范围升级校验。
     * <p>规则: PRIVATE -> 需 MANAGE 且是所有者; INTERNAL -> 需 MANAGE; PUBLIC -> 需 MANAGE (超管/developer 无限制)。
     * <p>降级或同级不做限制, 沿用 assertPerm(MANAGE) 即可, 这里提供兼容方法。
     */
    public void assertCanChangeVisibility(KbKnowledgeBaseEntity kb, DbUserDetails user, List<Long> roleIds, String newScope) {
        if (isDeveloper(user) || isSuperAdmin(user)) {
            return;
        }
        VisibilityScopeEnum target = VisibilityScopeEnum.parse(newScope);
        if (target == null) {
            throw new AssertException(ResultCode.PARAM_INVALID, "可见性范围非法");
        }
        assertPerm(kb, user, roleIds, KbPermLevelEnum.MANAGE);
        // PRIVATE 仅所有者可设
        if (target == VisibilityScopeEnum.PRIVATE) {
            if (kb.getOwnerUserId() == null || !kb.getOwnerUserId().equals(user.getUserId())) {
                throw new AssertException(ResultCode.FORBIDDEN, "仅知识库所有者可将其设为私有");
            }
        }
    }

    /** clientSelectable 仅超管/developer 可调整 */
    public void assertCanChangeClientSelectable(DbUserDetails user) {
        if (isDeveloper(user) || isSuperAdmin(user)) {
            return;
        }
        throw new AssertException(ResultCode.FORBIDDEN, "仅超级管理员可设置客户端可见选项");
    }

    /** 列表过滤: 过滤掉当前用户完全无权的 KB, 并填充 myPermLevel */
    public List<KbKnowledgeBaseEntity> filterVisible(List<KbKnowledgeBaseEntity> list, DbUserDetails user, List<Long> roleIds) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        if (isDeveloper(user) || isSuperAdmin(user) || isAdminUser(user)) {
            return list;
        }
        // 非 ADMIN: 一律不展示 (虽然登录时已拦截, 此处二次保护)
        return List.of();
    }

    /**
     * 批量解析权限级别, 一次 DB 查询 -> Map<kbId, level>.
     * <p>用于列表页回填 myPermLevel, 减少 N+1.
     */
    public java.util.Map<Long, KbPermLevelEnum> batchResolvePermLevel(Collection<KbKnowledgeBaseEntity> kbs, DbUserDetails user, List<Long> roleIds) {
        java.util.Map<Long, KbPermLevelEnum> result = new java.util.HashMap<>();
        if (kbs == null || kbs.isEmpty()) {
            return result;
        }
        if (isDeveloper(user) || isSuperAdmin(user)) {
            for (KbKnowledgeBaseEntity kb : kbs) {
                result.put(kb.getId(), KbPermLevelEnum.MANAGE);
            }
            return result;
        }
        if (!isAdminUser(user)) {
            for (KbKnowledgeBaseEntity kb : kbs) {
                result.put(kb.getId(), KbPermLevelEnum.NONE);
            }
            return result;
        }
        Set<Long> kbIds = kbs.stream().map(KbKnowledgeBaseEntity::getId).collect(Collectors.toSet());
        // 一次查角色授权
        java.util.Map<Long, KbPermLevelEnum> roleMap = new java.util.HashMap<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long kbId : kbIds) {
                KbPermLevelEnum lv = resolveRolePermLevel(kbId, roleIds);
                roleMap.put(kbId, lv);
            }
        }
        // 一次查用户授权
        java.util.Map<Long, KbPermLevelEnum> userMap = new java.util.HashMap<>();
        for (Long kbId : kbIds) {
            KbPermLevelEnum lv = resolveUserPermLevel(kbId, user.getUserId());
            userMap.put(kbId, lv);
        }
        for (KbKnowledgeBaseEntity kb : kbs) {
            VisibilityScopeEnum scope = VisibilityScopeEnum.parse(kb.getVisibilityScope());
            if (scope == VisibilityScopeEnum.PRIVATE) {
                if (kb.getOwnerUserId() != null && kb.getOwnerUserId().equals(user.getUserId())) {
                    result.put(kb.getId(), KbPermLevelEnum.MANAGE);
                } else {
                    result.put(kb.getId(), KbPermLevelEnum.NONE);
                }
                continue;
            }
            KbPermLevelEnum lv = KbPermLevelEnum.max(KbPermLevelEnum.VIEW,
                    KbPermLevelEnum.max(roleMap.getOrDefault(kb.getId(), KbPermLevelEnum.NONE),
                            userMap.getOrDefault(kb.getId(), KbPermLevelEnum.NONE)));
            result.put(kb.getId(), lv);
        }
        return result;
    }

    /** 加载当前用户绑定的 roleIds */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<Long> loadCurrentUserRoleIds(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Long> ids = rbacUserMapper.selectRoleIdsByUserId(userId);
        return ids == null ? new ArrayList<>() : ids;
    }

    /** 加载知识库 (status=1) */
    public KbKnowledgeBaseEntity loadActiveKb(Long kbId) {
        KbKnowledgeBaseEntity kb = kbKnowledgeBaseMapper.selectActiveById(kbId);
        if (kb == null) {
            throw new ResourceNotFoundException("知识库不存在或已禁用: " + kbId);
        }
        return kb;
    }

    public boolean isDeveloper(DbUserDetails user) {
        if (user == null) return false;
        return rbacDefaults.getDeveloperUsername().equalsIgnoreCase(user.getUsername());
    }

    public boolean isSuperAdmin(DbUserDetails user) {
        if (user == null) return false;
        return rbacDefaults.getSuperAdminUsername().equalsIgnoreCase(user.getUsername());
    }

    public boolean isAdminUser(DbUserDetails user) {
        if (user == null) return false;
        return UserTypeEnum.ADMIN.name().equalsIgnoreCase(user.getUserType());
    }
}
