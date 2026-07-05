package com.jc.rag.admin.module.rbac.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.rbac.model.param.RbacPermissionQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacPermissionSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacPermissionVO;
import com.jc.rag.admin.module.rbac.service.RbacPermissionService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 权限管理 Controller
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/permission")
@RequiredArgsConstructor
public class RbacPermissionController {

    private final RbacPermissionService rbacPermissionService;

    @GetMapping
    public R<PageResponse<RbacPermissionVO>> list(RbacPermissionQueryParam query) {
        return R.ok(rbacPermissionService.pageListPermissions(query));
    }

    @PostMapping
    @OperationLog(module = "RBAC", action = "CREATE_PERMISSION", description = "创建权限")
    public R<Long> create(@RequestBody @Valid RbacPermissionSaveParam param) {
        return R.ok(rbacPermissionService.createPermission(param));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "RBAC", action = "UPDATE_PERMISSION", description = "更新权限")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RbacPermissionSaveParam param) {
        return R.ok(rbacPermissionService.updatePermission(id, param));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "RBAC", action = "DELETE_PERMISSION", description = "删除权限")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(rbacPermissionService.deletePermission(id));
    }
}
