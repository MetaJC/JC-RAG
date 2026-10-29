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


package com.jc.rag.admin.module.rbac.department.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentAssignUsersParam;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentCompanyNameParam;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentMoveParam;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentQueryParam;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentSaveParam;
import com.jc.rag.admin.module.rbac.department.model.vo.RbacDepartmentCascadeResult;
import com.jc.rag.admin.module.rbac.department.model.vo.RbacDepartmentVO;
import com.jc.rag.admin.module.rbac.department.service.RbacDepartmentService;
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
 * 部门管理 Controller (RBAC 组织维度)。
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/department")
@RequiredArgsConstructor
public class RbacDepartmentController {

    private final RbacDepartmentService rbacDepartmentService;
    private final com.jc.rag.admin.module.rbac.service.RbacUserService rbacUserService;

    /** 分页列表 */
    @GetMapping
    public R<PageResponse<RbacDepartmentVO>> pageList(RbacDepartmentQueryParam query) {
        return R.ok(rbacDepartmentService.pageList(query));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public R<RbacDepartmentVO> detail(@PathVariable Long id) {
        return R.ok(rbacDepartmentService.getDetail(id));
    }

    /** 部门树 */
    @GetMapping("/tree")
    public R<List<RbacDepartmentVO>> tree() {
        return R.ok(rbacDepartmentService.listTree());
    }

    /** 创建 */
    @PostMapping
    @OperationLog(module = "RBAC", action = "CREATE_DEPARTMENT", description = "创建部门")
    public R<Long> create(@RequestBody @Valid RbacDepartmentSaveParam param) {
        return R.ok(rbacDepartmentService.create(param));
    }

    /** 更新 */
    @PutMapping("/{id}")
    @OperationLog(module = "RBAC", action = "UPDATE_DEPARTMENT", description = "更新部门")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RbacDepartmentSaveParam param) {
        return R.ok(rbacDepartmentService.update(id, param));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    @OperationLog(module = "RBAC", action = "DELETE_DEPARTMENT", description = "删除部门")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(rbacDepartmentService.delete(id));
    }

    /** 切换状态 */
    @PutMapping("/{id}/status/{status}")
    @OperationLog(module = "RBAC", action = "UPDATE_DEPARTMENT_STATUS", description = "切换部门状态")
    public R<Boolean> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        return R.ok(rbacDepartmentService.updateStatus(id, status));
    }

    /**
     * 级联切换状态: 父级启停时, 子部门 (含孙级) 同步受影响.
     * <p>用于部门组织维度保持状态一致.
     */
    @PutMapping("/{id}/status/{status}/cascade")
    @OperationLog(module = "RBAC", action = "UPDATE_DEPARTMENT_STATUS_CASCADE", description = "级联切换部门状态")
    public R<RbacDepartmentCascadeResult> updateStatusCascade(@PathVariable Long id, @PathVariable Integer status) {
        return R.ok(rbacDepartmentService.updateStatusCascade(id, status));
    }

    /** 拖拽调整结构 (改变父级 / 排序) */
    @PutMapping("/{id}/move")
    @OperationLog(module = "RBAC", action = "MOVE_DEPARTMENT", description = "拖拽调整部门结构")
    public R<Boolean> move(@PathVariable Long id, @RequestBody @Valid RbacDepartmentMoveParam param) {
        return R.ok(rbacDepartmentService.move(id, param.getTargetId(), param.getPosition()));
    }

    /**
     * 查询公司名称 (顶级部门).
     * <p>部门管理页表格上方单独展示用, 不在树内显示根节点.
     * <p>返回 id + name, 找不到顶级部门时 data=null (前端兜底展示 "—").
     */
    @GetMapping("/root")
    public R<RbacDepartmentVO> root() {
        return R.ok(rbacDepartmentService.getRootDepartment());
    }

    /**
     * 修改公司名称 (顶级部门 name).
     * <p>仅 super_admin / developer 可调用 (Service 层已校验).
     */
    @PutMapping("/company-name")
    @OperationLog(module = "RBAC", action = "UPDATE_COMPANY_NAME", description = "修改公司名称")
    public R<Boolean> updateCompanyName(@RequestBody @Valid RbacDepartmentCompanyNameParam param) {
        return R.ok(rbacDepartmentService.updateCompanyName(param.getName()));
    }

    /**
     * 查询某部门 (含后代) 关联的用户 ID 列表.
     * <p>用于部门管理 - 分配用户弹窗回显.
     */
    @GetMapping("/{id}/users")
    public R<List<Long>> listAssignedUserIds(@PathVariable Long id) {
        return R.ok(rbacUserService.findUserIdsByDepartmentId(id));
    }

    /**
     * 覆盖式为该部门 (含后代) 分配用户.
     * <p>语义: userIds 即该部门树范围内最终绑定的用户列表, 覆盖现有绑定.
     * <p>developer / super_admin 跳过, 不参与分配.
     */
    @PutMapping("/{id}/users")
    @OperationLog(module = "RBAC", action = "ASSIGN_DEPARTMENT_USERS", description = "为部门分配用户")
    public R<Boolean> assignUsers(@PathVariable Long id, @RequestBody @Valid RbacDepartmentAssignUsersParam param) {
        return R.ok(rbacUserService.assignUsersToDepartment(id, param.getUserIds()));
    }
}
