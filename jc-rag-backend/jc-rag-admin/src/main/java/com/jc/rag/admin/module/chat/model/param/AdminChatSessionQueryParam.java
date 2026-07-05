package com.jc.rag.admin.module.chat.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * A 端会话分页查询 Param。
 * <p>分页字段 {@code pageNum/pageSize} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class AdminChatSessionQueryParam extends PageQueryParam {

    private String keyword;

    /** 全量拉取工厂 (Service 内部使用)。 */
    public static AdminChatSessionQueryParam all() {
        AdminChatSessionQueryParam p = new AdminChatSessionQueryParam();
        p.setPageSize(Integer.MAX_VALUE);
        return p;
    }
}
