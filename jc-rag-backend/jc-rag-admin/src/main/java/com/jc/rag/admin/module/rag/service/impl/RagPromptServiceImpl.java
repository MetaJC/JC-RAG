package com.jc.rag.admin.module.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.admin.module.rag.model.param.RagPromptQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagPromptSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagPromptVO;
import com.jc.rag.admin.module.rag.service.RagPromptService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.web.page.AbstractPageService;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.common.web.page.TimeFormatUtil;
import com.jc.rag.domain.entity.rag.RagPromptEntity;
import com.jc.rag.domain.mapper.rag.RagPromptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prompt Service 实现。
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPromptServiceImpl
        extends AbstractPageService<RagPromptVO, RagPromptQueryParam, RagPromptEntity>
        implements RagPromptService {

    private final RagPromptMapper ragPromptMapper;

    @Override
    protected IPage<RagPromptEntity> pageQuery(RagPromptQueryParam q, IPage<RagPromptEntity> page) {
        return ragPromptMapper.pagePrompts(page, q.getScope(), PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    protected long countQuery(RagPromptQueryParam q) {
        return ragPromptMapper.countPrompts(q.getScope(), PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    protected RagPromptVO toVO(RagPromptEntity e) {
        RagPromptVO v = new RagPromptVO();
        v.setId(e.getId());
        v.setPromptKey(e.getPromptKey());
        v.setPromptName(e.getPromptName());
        v.setPromptContent(e.getPromptContent());
        v.setScope(e.getScope());
        v.setScopeId(e.getScopeId());
        v.setVersion(e.getVersion());
        v.setStatus(e.getStatus());
        v.setDescription(e.getDescription());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        v.setCreatedBy(e.getCreatedBy());
        return v;
    }

    @Override
    public List<RagPromptVO> listAll() {
        return ragPromptMapper.selectList(new QueryWrapper<RagPromptEntity>().orderByAsc("scope").orderByAsc("promptKey"))
                .stream().map(this::toVO).toList();
    }

    @Override
    public RagPromptVO getById(Long id) {
        RagPromptEntity e = ragPromptMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Prompt 不存在: id=" + id);
        }
        return toVO(e);
    }

    @Override
    @Transactional
    public Long create(RagPromptSaveParam param, Long userId) {
        RagPromptEntity e = new RagPromptEntity();
        e.setPromptKey(param.getPromptKey());
        e.setPromptName(param.getPromptName());
        e.setPromptContent(param.getPromptContent());
        e.setScope(param.getScope());
        e.setScopeId(param.getScopeId());
        e.setVersion(1);
        e.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        e.setDescription(param.getDescription());
        e.setCreatedBy(userId);
        ragPromptMapper.insert(e);
        return e.getId();
    }

    @Override
    @Transactional
    public boolean update(Long id, RagPromptSaveParam param) {
        RagPromptEntity e = ragPromptMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Prompt 不存在: id=" + id);
        }
        if (param.getPromptKey() != null) e.setPromptKey(param.getPromptKey());
        if (param.getPromptName() != null) e.setPromptName(param.getPromptName());
        if (param.getPromptContent() != null) e.setPromptContent(param.getPromptContent());
        if (param.getScope() != null) e.setScope(param.getScope());
        if (param.getScopeId() != null) e.setScopeId(param.getScopeId());
        if (param.getStatus() != null) e.setStatus(param.getStatus());
        if (param.getDescription() != null) e.setDescription(param.getDescription());
        e.setVersion((e.getVersion() == null ? 0 : e.getVersion()) + 1);
        return ragPromptMapper.updateById(e) > 0;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        return ragPromptMapper.deletePrompt(id) > 0;
    }

    @Override
    public PageResponse<Map<String, Object>> pageListAsMap(String keyword, String scope, PageRequest pageRequest) {
        String like = PageHelper.buildLikeClause(keyword);
        IPage<RagPromptEntity> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize(), false);
        page = ragPromptMapper.pagePrompts(page, scope, like);
        page.setTotal(ragPromptMapper.countPrompts(scope, like));
        return PageHelper.toPageResponse(page, this::toMap);
    }

    /** 兼容旧前端: entity -> Map (时间字段格式化). */
    private Map<String, Object> toMap(RagPromptEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("promptKey", e.getPromptKey());
        m.put("promptName", e.getPromptName());
        m.put("promptContent", e.getPromptContent());
        m.put("scope", e.getScope());
        m.put("scopeId", e.getScopeId());
        m.put("version", e.getVersion());
        m.put("status", e.getStatus());
        m.put("description", e.getDescription());
        m.put("createdBy", e.getCreatedBy());
        m.put("createdAt", TimeFormatUtil.format(e.getCreatedAt()));
        m.put("updatedAt", TimeFormatUtil.format(e.getUpdatedAt()));
        return m;
    }
}
