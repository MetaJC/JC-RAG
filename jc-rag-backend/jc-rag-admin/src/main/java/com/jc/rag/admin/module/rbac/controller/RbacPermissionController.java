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
