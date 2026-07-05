package com.jc.rag.admin.module.rag.service;

import com.jc.rag.admin.module.rag.model.param.RagPromptQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagPromptSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagPromptVO;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;

import java.util.List;
import java.util.Map;

/**
 * Prompt Service。
 * @author JC
 */
public interface RagPromptService {

    PageResponse<RagPromptVO> pageList(RagPromptQueryParam query);

    List<RagPromptVO> listAll();

    RagPromptVO getById(Long id);

    Long create(RagPromptSaveParam param, Long userId);

    /**
     * 更新 Prompt
     * @return true=成功 false=失败
     */
    boolean update(Long id, RagPromptSaveParam param);

    /**
     * 删除 Prompt
     * @return true=成功 false=失败
     */
    boolean delete(Long id);

    /** 兼容旧前端 */
    PageResponse<Map<String, Object>> pageListAsMap(String keyword, String scope, PageRequest pageRequest);
}
