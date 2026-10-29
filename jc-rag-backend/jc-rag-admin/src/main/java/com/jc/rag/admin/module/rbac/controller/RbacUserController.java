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
import com.jc.rag.admin.module.rbac.model.param.RbacUserAssignRoleParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserResetPasswordParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserSaveParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserStatusParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacUserVO;
import com.jc.rag.admin.module.rbac.service.RbacUserService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/user")
@RequiredArgsConstructor
public class RbacUserController {

    private final RbacUserService rbacUserService;

    @GetMapping
    public R<PageResponse<RbacUserVO>> list(RbacUserQueryParam query) {
        return R.ok(rbacUserService.pageListUsers(query));
    }

    @PostMapping
    @OperationLog(module = "RBAC", action = "CREATE_USER", description = "创建用户")
    public R<Long> create(@RequestBody @Valid RbacUserSaveParam param) {
        return R.ok(rbacUserService.createUser(param));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "RBAC", action = "UPDATE_USER", description = "更新用户")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RbacUserSaveParam param) {
        return R.ok(rbacUserService.updateUser(id, param));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "RBAC", action = "DELETE_USER", description = "删除用户")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(rbacUserService.deleteUser(id));
    }

    @PostMapping("/{id}/reset-password")
    @OperationLog(module = "RBAC", action = "RESET_PASSWORD", description = "重置密码")
    public R<Boolean> resetPassword(@PathVariable Long id, @RequestBody @Valid RbacUserResetPasswordParam param) {
        return R.ok(rbacUserService.resetPassword(id, param.getPassword()));
    }

    @PutMapping("/{id}/status")
    @OperationLog(module = "RBAC", action = "UPDATE_USER_STATUS", description = "切换用户状态")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestBody @Valid RbacUserStatusParam param) {
        return R.ok(rbacUserService.updateStatus(id, param.getStatus()));
    }

    /** 覆盖式为用户分配角色（仅更新角色，不触发用户基本信息校验） */
    @PutMapping("/{id}/roles")
    @OperationLog(module = "RBAC", action = "ASSIGN_USER_ROLES", description = "为用户分配角色")
    public R<Boolean> assignRoles(@PathVariable Long id, @RequestBody @Valid RbacUserAssignRoleParam param) {
        return R.ok(rbacUserService.replaceUserRoles(id, param.getRoleIds()));
    }

    /**
     * 单用户详情 (只读), 用于"部门管理→部门树内点用户 nickname"弹窗.
     * <p>不返回 passwordHash, 含性别 / 部门名 / 登录时间等展示字段.
     */
    @GetMapping("/{id}/detail")
    public R<RbacUserVO> detail(@PathVariable Long id) {
        return R.ok(rbacUserService.getUserDetail(id));
    }

    /**
     * 性别字典 (0=未知 1=男 2=女), 前端下拉 / 列表展示用.
     */
    @GetMapping("/gender-options")
    public R<java.util.List<java.util.Map<String, Object>>> genderOptions() {
        return R.ok(java.util.List.of(
                java.util.Map.of("value", 0, "label", "未知"),
                java.util.Map.of("value", 1, "label", "男"),
                java.util.Map.of("value", 2, "label", "女")
        ));
    }

    /**
     * 单级部门直接关联用户 (不递归, 不含子部门用户).
     * <p>用于"部门管理→显示部门用户"开关展开时拉取, 已排除 developer / super_admin.
     */
    @GetMapping("/list-by-department/{deptId}")
    public R<java.util.List<java.util.Map<String, Object>>> listByDepartment(@PathVariable Long deptId) {
        return R.ok(rbacUserService.listUsersByDepartmentId(deptId));
    }
}
