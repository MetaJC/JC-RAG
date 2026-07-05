package com.jc.rag.admin.module.knowledge.exception;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;

/**
 * 知识库任务冲突异常 (409)
 * <p>文档正在解析/重建索引中, 拒绝重复触发。
 * @author JC
 */
public class KnowledgeTaskConflictException extends BizException {

    private static final long serialVersionUID = 1L;

    public KnowledgeTaskConflictException(String messageKey) {
        super(ResultCode.CONFLICT, messageKey);
    }
}
