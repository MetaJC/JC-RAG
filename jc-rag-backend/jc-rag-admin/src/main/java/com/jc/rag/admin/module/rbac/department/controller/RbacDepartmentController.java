package com.jc.rag.admin.module.rbac.department.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
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
}
