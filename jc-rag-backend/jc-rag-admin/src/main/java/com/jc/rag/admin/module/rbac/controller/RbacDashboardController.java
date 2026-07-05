package com.jc.rag.admin.module.rbac.controller;

import com.jc.rag.admin.module.rbac.model.param.RbacPermissionQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleQueryParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacDashboardVO;
import com.jc.rag.admin.module.rbac.model.vo.RbacPermissionVO;
import com.jc.rag.admin.module.rbac.model.vo.RbacRoleVO;
import com.jc.rag.admin.module.rbac.service.RbacPermissionService;
import com.jc.rag.admin.module.rbac.service.RbacRoleService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RBAC Dashboard 聚合 Controller。
 * <p>前端 RbacManageView / RbacUserView / RbacRoleView 三个页面 onMounted 都会调
 * {@code GET /api/rbac/dashboard} 一次拿全量角色 + 权限, 避免每个组件都做 size=9999 的全表查询.
 * <p>历史原因: SQL 010 把 RbacManageView 拆为 4 个独立页 (users/roles/menus/permissions),
 * 但前端 3 处 view 仍引用了旧 RBAC dashboard 接口, 后端本应一起迁移; 此处补齐缺失端点.
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/dashboard")
@RequiredArgsConstructor
public class RbacDashboardController {

    /**
     * 单次拉全表的最大条数.
     * <p>{@link com.jc.rag.common.web.page.PageQueryParam#MAX_SIZE} 默认 200; rbac 角色/权限通常 < 100 条, 200 足够.
     */
    private static final int MAX_FETCH = 200;

    private final RbacRoleService rbacRoleService;
    private final RbacPermissionService rbacPermissionService;

    @GetMapping
    public R<RbacDashboardVO> dashboard() {
        RbacDashboardVO vo = new RbacDashboardVO();
        vo.setRoles(fetchAllRoles());
        vo.setPermissions(fetchAllPermissions());
        return R.ok(vo);
    }

    /** 拉全量角色并精简字段. */
    private List<RbacDashboardVO.RoleItem> fetchAllRoles() {
        try {
            RbacRoleQueryParam q = new RbacRoleQueryParam();
            q.setPageNum(1);
            q.setPageSize(MAX_FETCH);
            PageResponse<RbacRoleVO> page = rbacRoleService.pageListRoles(q);
            if (page == null || page.getRecords() == null) {
                return Collections.emptyList();
            }
            return page.getRecords().stream().map(this::toRoleItem).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[RbacDashboard] 拉取角色失败, 返回空列表: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 拉全量权限并精简字段. */
    private List<RbacDashboardVO.PermissionItem> fetchAllPermissions() {
        try {
            RbacPermissionQueryParam q = new RbacPermissionQueryParam();
            q.setPageNum(1);
            q.setPageSize(MAX_FETCH);
            PageResponse<RbacPermissionVO> page = rbacPermissionService.pageListPermissions(q);
            if (page == null || page.getRecords() == null) {
                return Collections.emptyList();
            }
            return page.getRecords().stream().map(this::toPermItem).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[RbacDashboard] 拉取权限失败, 返回空列表: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private RbacDashboardVO.RoleItem toRoleItem(RbacRoleVO v) {
        RbacDashboardVO.RoleItem r = new RbacDashboardVO.RoleItem();
        r.setId(v.getId());
        r.setRoleCode(v.getRoleCode());
        r.setRoleName(v.getRoleName());
        r.setStatus(v.getStatus());
        return r;
    }

    private RbacDashboardVO.PermissionItem toPermItem(RbacPermissionVO v) {
        RbacDashboardVO.PermissionItem p = new RbacDashboardVO.PermissionItem();
        p.setId(v.getId());
        p.setPermCode(v.getPermCode());
        p.setPermName(v.getPermName());
        p.setPermType(v.getPermType());
        p.setParentId(v.getParentId());
        return p;
    }
}
