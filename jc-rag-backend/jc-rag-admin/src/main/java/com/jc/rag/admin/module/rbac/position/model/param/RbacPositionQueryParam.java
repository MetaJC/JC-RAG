package com.jc.rag.admin.module.rbac.position.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 岗位分页查询 Param。
 * <p>分页字段 pageNum / pageSize 由父类提供。
 *
 * @author JC
 */
@Data
public class RbacPositionQueryParam extends PageQueryParam {

    /**
     * 关键字 (name / code 模糊匹配)
     */
    private String keyword;

    /**
     * 状态 (0 禁用 / 1 启用)
     */
    private Integer status;

    /**
     * 所属部门 id
     */
    private Long departmentId;

    /**
     * 岗位级别 (1 初级 / 2 中级 / 3 高级)
     */
    private Integer level;
}
