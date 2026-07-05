package com.jc.rag.admin.module.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import com.jc.rag.admin.common.enums.UserTypeEnum;
import com.jc.rag.admin.module.knowledge.model.param.KbAuthSaveParam;
import com.jc.rag.admin.module.knowledge.model.vo.KbAuthDetailVO;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.domain.entity.knowledge.KbKnowledgeBaseEntity;
import com.jc.rag.domain.entity.knowledge.KbRoleAuthEntity;
import com.jc.rag.domain.entity.knowledge.KbUserAuthEntity;
import com.jc.rag.domain.entity.rbac.RbacRoleEntity;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import com.jc.rag.domain.enums.knowledge.KbPermLevelEnum;
import com.jc.rag.domain.mapper.knowledge.KbRoleAuthMapper;
import com.jc.rag.domain.mapper.knowledge.KbUserAuthMapper;
import com.jc.rag.domain.mapper.rbac.RbacRoleMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserRoleMapper;
import com.jc.rag.domain.entity.rbac.RbacUserRoleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识库授权管理服务。
 * <p>提供: 授权详情查询 / 全量替换保存。
 * <p>需当前用户对知识库具备 MANAGE 权限。
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbAuthService {

    private final KbPermissionService kbPermissionService;
    private final KbRoleAuthMapper kbRoleAuthMapper;
    private final KbUserAuthMapper kbUserAuthMapper;
    private final RbacRoleMapper rbacRoleMapper;
    private final RbacUserMapper rbacUserMapper;
    private final RbacUserRoleMapper rbacUserRoleMapper;
    private final RbacDefaultsProperties rbacDefaults;
    private final com.jc.rag.admin.module.sys.service.OperationLogService operationLogService;

    /** 查询授权详情: 全量角色 + 全量 ADMIN 用户 + 当前选中状态 */
    public KbAuthDetailVO getAuthDetail(Long kbId) {
        DbUserDetails user = currentUser();
        KbKnowledgeBaseEntity kb = kbPermissionService.loadActiveKb(kbId);
        kbPermissionService.assertPerm(kb, user, kbPermissionService.loadCurrentUserRoleIds(user.getUserId()),
                KbPermLevelEnum.MANAGE);

        KbAuthDetailVO vo = new KbAuthDetailVO();
        vo.setKbId(kb.getId());
        vo.setKbName(kb.getKbName());
        vo.setVisibilityScope(kb.getVisibilityScope());
        vo.setClientSelectable(kb.getClientSelectable());

        // 全量管理端角色 (isAdmin=1), 排除普通用户/客户端用户/访客等
        List<RbacRoleEntity> roles = rbacRoleMapper.selectList(
                new QueryWrapper<RbacRoleEntity>()
                        .eq("is_admin", 1)
                        .orderByAsc("id"));
        List<KbRoleAuthEntity> authList = kbRoleAuthMapper.listByKbId(kbId);
        log.info("[KB-AUTH] detail kbId={} rawAuthSize={} rows={}", kbId, authList.size(),
                authList.stream().map(e -> "role=" + e.getRoleId() + ":perm=" + e.getPermLevel()).toList());
        Map<Long, Integer> rolePermMap = authList.stream()
                .filter(e -> e.getPermLevel() != null)
                .collect(Collectors.toMap(KbRoleAuthEntity::getRoleId, KbRoleAuthEntity::getPermLevel, (a, b) -> a));
        for (RbacRoleEntity r : roles) {
            // null = 未授权 (未保存或被取消); 非 null = 已保存的实际级别
            Integer level = rolePermMap.get(r.getId());
            vo.getRoleAuths().add(KbAuthDetailVO.RoleAuthItem.of(
                    r.getId(), r.getRoleCode(), r.getRoleName(), level));
        }

        // 全量 ADMIN 用户 (排除 developer 已登录态以更直观, 但仍展示以便 audit)
        List<RbacUserEntity> users = rbacUserMapper.selectList(
                new QueryWrapper<RbacUserEntity>()
                        .eq("deleted", 0)
                        .eq("user_type", UserTypeEnum.ADMIN.name())
                        .orderByAsc("id"));
        Map<Long, Integer> userPermMap = kbUserAuthMapper.listByKbId(kbId).stream()
                .collect(Collectors.toMap(KbUserAuthEntity::getUserId, KbUserAuthEntity::getPermLevel, (a, b) -> a));
        // 用户的角色集合 (用于左侧联动)
        Map<Long, List<Long>> userRoleMap = new HashMap<>();
        for (RbacUserEntity u : users) {
            userRoleMap.put(u.getId(), new ArrayList<>());
        }
        if (!users.isEmpty()) {
            List<RbacUserRoleEntity> relations = rbacUserRoleMapper.selectList(
                    new QueryWrapper<RbacUserRoleEntity>().in("user_id", userRoleMap.keySet()));
            for (RbacUserRoleEntity r : relations) {
                List<Long> list = userRoleMap.get(r.getUserId());
                if (list != null) list.add(r.getRoleId());
            }
        }
        for (RbacUserEntity u : users) {
            // null = 未直接授权; 非 null = 已保存的实际级别
            Integer level = userPermMap.get(u.getId());
            vo.getUserAuths().add(KbAuthDetailVO.UserAuthItem.of(
                    u.getId(), u.getUsername(), u.getNickname(), level,
                    userRoleMap.getOrDefault(u.getId(), Collections.emptyList())));
        }
        return vo;
    }

    /**
     * 全量替换保存: 先删除 KB 下的全部角色/用户授权, 再批量 upsert.
     */
    @Transactional
    public void saveAuth(Long kbId, KbAuthSaveParam param) {
        DbUserDetails user = currentUser();
        KbKnowledgeBaseEntity kb = kbPermissionService.loadActiveKb(kbId);
        kbPermissionService.assertPerm(kb, user, kbPermissionService.loadCurrentUserRoleIds(user.getUserId()),
                KbPermLevelEnum.MANAGE);
        if (param == null) {
            throw new AssertException(ResultCode.PARAM_INVALID, "参数不能为空");
        }
        LocalDateTime now = LocalDateTime.now();

        // 角色授权
        kbRoleAuthMapper.deleteByKbId(kbId);
        if (param.getRoleAuths() != null) {
            List<KbRoleAuthEntity> rows = new ArrayList<>();
            for (KbAuthSaveParam.KbRoleAuthItem item : param.getRoleAuths()) {
                if (item == null || item.getRoleId() == null) continue;
                int level = item.getPermLevel() == null ? 0 : item.getPermLevel();
                if (level < 0 || level > 3) {
                    throw new AssertException(ResultCode.PARAM_INVALID, "角色权限级别非法: " + level);
                }
                // level=0 也保存: 用于明确表达"已取消/未授权", 与"从未保存"区分开
                KbRoleAuthEntity e = new KbRoleAuthEntity();
                e.setKbId(kbId);
                e.setRoleId(item.getRoleId());
                e.setPermLevel(level);
                e.setCreatedAt(now);
                e.setUpdatedAt(now);
                rows.add(e);
            }
            if (!rows.isEmpty()) {
                kbRoleAuthMapper.batchUpsert(rows);
            }
        }

        // 用户授权
        kbUserAuthMapper.deleteByKbId(kbId);
        if (param.getUserAuths() != null) {
            List<KbUserAuthEntity> rows = new ArrayList<>();
            Set<Long> adminUserIds = rbacUserMapper.selectList(
                    new QueryWrapper<RbacUserEntity>()
                            .eq("deleted", 0)
                            .eq("user_type", UserTypeEnum.ADMIN.name()))
                    .stream().map(RbacUserEntity::getId).collect(Collectors.toSet());
            for (KbAuthSaveParam.KbUserAuthItem item : param.getUserAuths()) {
                if (item == null || item.getUserId() == null) continue;
                if (!adminUserIds.contains(item.getUserId())) {
                    throw new AssertException(ResultCode.PARAM_INVALID, "用户非管理用户, 不可授权: " + item.getUserId());
                }
                int level = item.getPermLevel() == null ? 0 : item.getPermLevel();
                if (level < 0 || level > 3) {
                    throw new AssertException(ResultCode.PARAM_INVALID, "用户权限级别非法: " + level);
                }
                if (level == 0) continue; // 未授权/已取消
                KbUserAuthEntity e = new KbUserAuthEntity();
                e.setKbId(kbId);
                e.setUserId(item.getUserId());
                e.setPermLevel(level);
                e.setCreatedAt(now);
                e.setUpdatedAt(now);
                rows.add(e);
            }
            if (!rows.isEmpty()) {
                kbUserAuthMapper.batchUpsert(rows);
            }
        }

        operationLogService.log("知识库管理", "UPDATE_KB_AUTH",
                "更新知识库授权: kbId=" + kbId + ", roles=" + (param.getRoleAuths() == null ? 0 : param.getRoleAuths().size())
                        + ", users=" + (param.getUserAuths() == null ? 0 : param.getUserAuths().size()),
                "SUCCESS");
    }

    private DbUserDetails currentUser() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof DbUserDetails u)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return u;
    }
}
