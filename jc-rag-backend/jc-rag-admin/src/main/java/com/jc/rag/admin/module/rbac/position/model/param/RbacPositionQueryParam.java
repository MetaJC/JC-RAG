package com.jc.rag.admin.module.rbac.position.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 岗位分页查询 Param。
 * <p>分页字段 pageNum / pageSize 由父类提供。
 * <p>岗位与部门解耦: 不再按 departmentId 过滤, 用户-部门关联走 rbac_user_department。
 *
 * @author JC
 */
@Data
public class RbacPositionQueryParam extends PageQueryParam {

    /**
     * 关键字 (name / displayName 模糊匹配)
     */
    private String keyword;

    /**
     * 状态 (0 禁用 / 1 启用)
     */
    private Integer status;

    /**
     * 岗位级别 (1 初级 / 2 中级 / 3 高级)
     */
    private Integer level;
}
