package com.jc.rag.admin.module.sys.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 审计日志查询 Param (RAG 调用 / 系统异常共用)。
 * <p>分页字段 {@code page/size} 由父类 {@link PageQueryParam} 提供, 业务过滤字段仅作扩展。
 * @author JC
 */
@Data
public class AuditLogQueryParam extends PageQueryParam {

    private String keyword;

    private String modelName;

    private String userName;

    private String moduleName;
}
