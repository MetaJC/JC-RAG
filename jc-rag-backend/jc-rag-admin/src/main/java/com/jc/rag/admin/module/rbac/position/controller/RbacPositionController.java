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


package com.jc.rag.admin.module.rbac.position.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.rbac.model.param.RbacPositionAssignUsersParam;
import com.jc.rag.admin.module.rbac.position.model.param.RbacPositionQueryParam;
import com.jc.rag.admin.module.rbac.position.model.param.RbacPositionSaveParam;
import com.jc.rag.admin.module.rbac.position.model.vo.RbacPositionVO;
import com.jc.rag.admin.module.rbac.position.service.RbacPositionService;
import com.jc.rag.admin.module.rbac.service.RbacUserService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 岗位管理 Controller (RBAC 组织维度)。
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/position")
@RequiredArgsConstructor
public class RbacPositionController {

    private final RbacPositionService rbacPositionService;
    private final RbacUserService rbacUserService;

    /** 分页列表 */
    @GetMapping
    public R<PageResponse<RbacPositionVO>> pageList(RbacPositionQueryParam query) {
        return R.ok(rbacPositionService.pageList(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public R<RbacPositionVO> detail(@PathVariable Long id) {
        return R.ok(rbacPositionService.getDetail(id));
    }

    /** 全量启用岗位 (用于下拉框) */
    @GetMapping("/active")
    public R<List<RbacPositionVO>> listActive() {
        return R.ok(rbacPositionService.listActive());
    }

    /** 创建 */
    @PostMapping
    @OperationLog(module = "RBAC", action = "CREATE_POSITION", description = "创建岗位")
    public R<Long> create(@RequestBody @Valid RbacPositionSaveParam param) {
        return R.ok(rbacPositionService.create(param));
    }

    /** 更新 */
    @PutMapping("/{id}")
    @OperationLog(module = "RBAC", action = "UPDATE_POSITION", description = "更新岗位")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RbacPositionSaveParam param) {
        return R.ok(rbacPositionService.update(id, param));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @OperationLog(module = "RBAC", action = "DELETE_POSITION", description = "删除岗位")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(rbacPositionService.delete(id));
    }

    /** 切换状态 */
    @PutMapping("/{id}/status/{status}")
    @OperationLog(module = "RBAC", action = "UPDATE_POSITION_STATUS", description = "切换岗位状态")
    public R<Boolean> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        return R.ok(rbacPositionService.updateStatus(id, status));
    }

    /**
     * 查询某岗位关联的用户 ID 列表.
     * <p>用于岗位管理 - 分配用户弹窗回显.
     */
    @GetMapping("/{id}/users")
    public R<List<Long>> listAssignedUserIds(@PathVariable Long id) {
        return R.ok(rbacUserService.findUserIdsByPositionId(id));
    }

    /**
     * 覆盖式为该岗位分配用户.
     * <p>语义: userIds 即该岗位最终绑定的用户列表, 覆盖现有绑定.
     * <p>developer / super_admin 跳过, 不参与分配.
     */
    @PutMapping("/{id}/users")
    @OperationLog(module = "RBAC", action = "ASSIGN_POSITION_USERS", description = "为岗位分配用户")
    public R<Boolean> assignUsers(@PathVariable Long id, @RequestBody @Valid RbacPositionAssignUsersParam param) {
        return R.ok(rbacUserService.assignUsersToPosition(id, param.getUserIds()));
    }
}
