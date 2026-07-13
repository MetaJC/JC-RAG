package com.jc.rag.admin.module.rbac.department.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 部门分页查询 Param。
 * <p>分页字段 pageNum / pageSize 由父类提供。
 *
 * @author JC
 */
@Data
public class RbacDepartmentQueryParam extends PageQueryParam {

    /**
     * 关键字 (name / code 模糊匹配)
     */
    private String keyword;

    /**
     * 状态 (0 禁用 / 1 启用)
     */
    private Integer status;

    /**
     * 父部门 id (0 或 null = 顶级)
     */
    private Long parentId;
}
