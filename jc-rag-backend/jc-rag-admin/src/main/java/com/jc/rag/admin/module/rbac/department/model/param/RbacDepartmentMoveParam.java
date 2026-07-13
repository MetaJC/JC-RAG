package com.jc.rag.admin.module.rbac.department.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 部门拖拽结构变更 Param。
 * <p>position:
 * <ul>
 *   <li>before - 变成 target 的同级, 排在 target 之前</li>
 *   <li>after  - 变成 target 的同级, 排在 target 之后</li>
 *   <li>inner  - 变成 target 的子级, 排在同级最后</li>
 * </ul>
 *
 * @author JC
 */
@Data
public class RbacDepartmentMoveParam {

    /**
     * 目标部门 id (拖拽释放位置所在的行 id)
     */
    @NotNull
    private Long targetId;

    /**
     * 释放位置: before / after / inner
     */
    @NotBlank
    private String position;
}
