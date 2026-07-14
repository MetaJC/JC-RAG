package com.jc.rag.admin.module.rbac.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 用户分页查询 Param。
 * <p>分页字段 {@code pageNum/pageSize} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class RbacUserQueryParam extends PageQueryParam {

    private String keyword;

    /**
     * 部门 ID 过滤.
     * <p>0 或 null = 不按部门过滤 (全公司);
     * <p>非 0 = 过滤该部门及其所有后代部门的用户.
     */
    private Long deptId;
}
