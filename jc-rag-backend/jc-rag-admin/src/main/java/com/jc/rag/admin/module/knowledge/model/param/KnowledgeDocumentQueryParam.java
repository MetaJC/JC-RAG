package com.jc.rag.admin.module.knowledge.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 知识库文档分页查询 Param。
 * <p>分页字段 {@code pageNum/pageSize} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class KnowledgeDocumentQueryParam extends PageQueryParam {

    private String keyword;
    private String parseStatus;

    /** 全量拉取工厂 (Service 内部使用)。 */
    public static KnowledgeDocumentQueryParam all() {
        KnowledgeDocumentQueryParam p = new KnowledgeDocumentQueryParam();
        p.setPageSize(Integer.MAX_VALUE);
        return p;
    }
}
