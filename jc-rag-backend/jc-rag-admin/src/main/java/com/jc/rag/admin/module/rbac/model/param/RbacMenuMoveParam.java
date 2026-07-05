package com.jc.rag.admin.module.rbac.model.param;

import lombok.Data;

/**
 * 菜单拖拽移动参数 (前端 el-tree @node-drop 调用)
 * <p>parentId=0 / null 表示移到根目录
 *
 * @author JC
 */
@Data
public class RbacMenuMoveParam {

    /** 新父级 id; 0 或 null 表示移到根 */
    private Long parentId;

    /** 拖入目标位置后的 sort (调用方按 drop position 计算, 可选) */
    private Integer sort;
}
