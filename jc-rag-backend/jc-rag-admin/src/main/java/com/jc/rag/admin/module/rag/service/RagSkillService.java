package com.jc.rag.admin.module.rag.service;

import com.jc.rag.admin.module.rag.model.param.RagSkillQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagSkillSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagSkillVO;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;

import java.util.List;
import java.util.Map;

/**
 * Skill Service。
 * @author JC
 */
public interface RagSkillService {

    PageResponse<RagSkillVO> pageList(RagSkillQueryParam query);

    List<RagSkillVO> listAll();

    RagSkillVO getById(Long id);

    Long create(RagSkillSaveParam param, Long userId);

    /**
     * 更新 Skill
     * @return true=成功 false=失败
     */
    boolean update(Long id, RagSkillSaveParam param);

    /**
     * 删除 Skill
     * @return true=成功 false=失败
     */
    boolean delete(Long id);

    /** 兼容旧前端：直接返回 Map 列表 */
    PageResponse<Map<String, Object>> pageListAsMap(String keyword, String status, PageRequest pageRequest);
}
