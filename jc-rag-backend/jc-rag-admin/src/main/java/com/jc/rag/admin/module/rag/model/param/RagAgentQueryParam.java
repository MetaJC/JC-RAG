package com.jc.rag.admin.module.rag.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * Agent 分页查询 Param。
 * <p>分页字段 {@code pageNum/pageSize} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class RagAgentQueryParam extends PageQueryParam {

    private String keyword;

    private Integer status;

    /** 智能体类型过滤: 1=系统智能体, 0=内建用户智能体, null=全部 */
    private Integer isSystem;
}
