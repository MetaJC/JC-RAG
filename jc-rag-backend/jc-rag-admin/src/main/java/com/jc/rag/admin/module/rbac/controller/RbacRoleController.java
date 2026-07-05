package com.jc.rag.admin.module.rbac.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleAssignParam;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleSaveParam;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleStatusParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacRoleVO;
import com.jc.rag.admin.module.rbac.service.RbacRoleService;
import com.jc.rag.admin.module.rbac.service.RbacUserService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理 Controller
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/role")
@RequiredArgsConstructor
public class RbacRoleController {

    private final RbacRoleService rbacRoleService;
    private final RbacUserService rbacUserService;

    @GetMapping
    public R<PageResponse<RbacRoleVO>> list(RbacRoleQueryParam query) {
        return R.ok(rbacRoleService.pageListRoles(query));
    }

    @PostMapping
    @OperationLog(module = "RBAC", action = "CREATE_ROLE", description = "创建角色")
    public R<Long> create(@RequestBody @Valid RbacRoleSaveParam param) {
        return R.ok(rbacRoleService.createRole(param));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "RBAC", action = "UPDATE_ROLE", description = "更新角色")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RbacRoleSaveParam param) {
        return R.ok(rbacRoleService.updateRole(id, param));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "RBAC", action = "DELETE_ROLE", description = "删除角色")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(rbacRoleService.deleteRole(id));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "RBAC", action = "UPDATE_ROLE_STATUS", description = "切换角色状态")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestBody @Valid RbacRoleStatusParam param) {
        return R.ok(rbacRoleService.updateStatus(id, param.getStatus()));
    }

    /** 查询该角色下已绑定的用户 ID 列表 */
    @GetMapping("/{id}/users")
    public R<List<Long>> listUsers(@PathVariable Long id) {
        return R.ok(rbacUserService.findUserIdsByRoleId(id));
    }

    /** 覆盖式为该角色分配用户 */
    @PutMapping("/{id}/users")
    @OperationLog(module = "RBAC", action = "ASSIGN_ROLE_USERS", description = "为角色分配用户")
    public R<Boolean> assignUsers(@PathVariable Long id, @RequestBody @Valid RbacRoleAssignParam param) {
        return R.ok(rbacUserService.assignUsersToRole(id, param.getUserIds()));
    }
}
