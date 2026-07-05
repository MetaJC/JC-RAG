package com.jc.rag.admin.module.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.admin.module.rag.model.param.RagSkillQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagSkillSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagSkillVO;
import com.jc.rag.admin.module.rag.service.RagSkillService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.web.page.AbstractPageService;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.common.web.page.TimeFormatUtil;
import com.jc.rag.domain.entity.rag.RagSkillEntity;
import com.jc.rag.domain.mapper.rag.RagSkillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Skill Service 实现。
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagSkillServiceImpl
        extends AbstractPageService<RagSkillVO, RagSkillQueryParam, RagSkillEntity>
        implements RagSkillService {

    private final RagSkillMapper ragSkillMapper;

    @Override
    protected IPage<RagSkillEntity> pageQuery(RagSkillQueryParam q, IPage<RagSkillEntity> page) {
        return ragSkillMapper.pageSkills(page, q.getStatus(), PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    protected long countQuery(RagSkillQueryParam q) {
        return ragSkillMapper.countSkills(q.getStatus(), PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    protected RagSkillVO toVO(RagSkillEntity e) {
        RagSkillVO v = new RagSkillVO();
        v.setId(e.getId());
        v.setSkillName(e.getSkillName());
        v.setSkillKey(e.getSkillKey());
        v.setDescription(e.getDescription());
        v.setPromptTemplate(e.getPromptTemplate());
        v.setTriggerType(e.getTriggerType());
        v.setTriggerRules(e.getTriggerRules());
        v.setIcon(e.getIcon());
        v.setSortOrder(e.getSortOrder());
        v.setStatus(e.getStatus());
        v.setCreatedBy(e.getCreatedBy());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }

    @Override
    public List<RagSkillVO> listAll() {
        return ragSkillMapper.selectList(new QueryWrapper<RagSkillEntity>().orderByAsc("sortOrder").orderByDesc("id"))
                .stream().map(this::toVO).toList();
    }

    @Override
    public RagSkillVO getById(Long id) {
        RagSkillEntity e = ragSkillMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Skill 不存在: id=" + id);
        }
        return toVO(e);
    }

    @Override
    @Transactional
    public Long create(RagSkillSaveParam param, Long userId) {
        RagSkillEntity e = new RagSkillEntity();
        e.setSkillName(param.getSkillName());
        e.setSkillKey(param.getSkillKey());
        e.setDescription(param.getDescription());
        e.setPromptTemplate(param.getPromptTemplate());
        e.setTriggerType(param.getTriggerType());
        e.setTriggerRules(param.getTriggerRules());
        e.setIcon(param.getIcon());
        e.setSortOrder(param.getSortOrder() == null ? 0 : param.getSortOrder());
        e.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        e.setCreatedBy(userId);
        ragSkillMapper.insert(e);
        return e.getId();
    }

    @Override
    @Transactional
    public boolean update(Long id, RagSkillSaveParam param) {
        RagSkillEntity e = ragSkillMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Skill 不存在: id=" + id);
        }
        if (param.getSkillName() != null) e.setSkillName(param.getSkillName());
        if (param.getSkillKey() != null) e.setSkillKey(param.getSkillKey());
        if (param.getDescription() != null) e.setDescription(param.getDescription());
        if (param.getPromptTemplate() != null) e.setPromptTemplate(param.getPromptTemplate());
        if (param.getTriggerType() != null) e.setTriggerType(param.getTriggerType());
        if (param.getTriggerRules() != null) e.setTriggerRules(param.getTriggerRules());
        if (param.getIcon() != null) e.setIcon(param.getIcon());
        if (param.getSortOrder() != null) e.setSortOrder(param.getSortOrder());
        if (param.getStatus() != null) e.setStatus(param.getStatus());
        return ragSkillMapper.updateById(e) > 0;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        return ragSkillMapper.deleteSkill(id) > 0;
    }

    @Override
    public PageResponse<Map<String, Object>> pageListAsMap(String keyword, String status, PageRequest pageRequest) {
        Integer s = parseStatus(status);
        String like = PageHelper.buildLikeClause(keyword);
        IPage<RagSkillEntity> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize(), false);
        page = ragSkillMapper.pageSkills(page, s, like);
        page.setTotal(ragSkillMapper.countSkills(s, like));
        return PageHelper.toPageResponse(page, this::toMap);
    }

    /* ============================================================ */
    /* 内部: 转换                                                     */
    /* ============================================================ */
    /** 兼容旧前端: entity -> Map (时间字段格式化). */
    private Map<String, Object> toMap(RagSkillEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("skillName", e.getSkillName());
        m.put("skillKey", e.getSkillKey());
        m.put("description", e.getDescription());
        m.put("promptTemplate", e.getPromptTemplate());
        m.put("triggerType", e.getTriggerType());
        m.put("triggerRules", e.getTriggerRules());
        m.put("icon", e.getIcon());
        m.put("sortOrder", e.getSortOrder());
        m.put("status", e.getStatus());
        m.put("createdBy", e.getCreatedBy());
        m.put("createdAt", TimeFormatUtil.format(e.getCreatedAt()));
        m.put("updatedAt", TimeFormatUtil.format(e.getUpdatedAt()));
        return m;
    }

    private static Integer parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }
}
