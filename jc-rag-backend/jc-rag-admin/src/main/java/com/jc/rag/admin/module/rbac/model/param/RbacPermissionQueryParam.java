package com.jc.rag.admin.module.rbac.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 权限分页查询 Param。
 * <p>分页字段 {@code pageNum/pageSize} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class RbacPermissionQueryParam extends PageQueryParam {

    private String keyword;

    private String permType;
}
