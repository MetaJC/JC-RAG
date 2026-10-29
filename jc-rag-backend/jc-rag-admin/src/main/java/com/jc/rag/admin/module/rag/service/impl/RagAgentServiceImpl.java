/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.admin.module.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.admin.module.rag.model.param.RagAgentQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagAgentSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagAgentVO;
import com.jc.rag.admin.module.rag.service.RagAgentService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.web.page.AbstractPageService;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.common.web.page.TimeFormatUtil;
import com.jc.rag.domain.entity.rag.RagAgentEntity;
import com.jc.rag.domain.entity.rag.RagAgentSkillEntity;
import com.jc.rag.domain.mapper.rag.RagAgentMapper;
import com.jc.rag.domain.mapper.rag.RagAgentSkillMapper;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent Service 实现。
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAgentServiceImpl
        extends AbstractPageService<RagAgentVO, RagAgentQueryParam, RagAgentEntity>
        implements RagAgentService {

    private final RagAgentMapper ragAgentMapper;
    private final RagAgentSkillMapper ragAgentSkillMapper;
    private final SysConfigMapper sysConfigMapper;

    /**
     * 业务 Key 生成: 优先用入参; 入参为空时按 agentName slug 化生成; 中文名/全符号降级为 "agent-{ms}" 唯一兜底.
     */
    private String slugifyAgentKey(String agentName, String agentKey) {
        if (agentKey != null && !agentKey.isBlank()) {
            return agentKey.trim();
        }
        if (agentName == null) return "agent-" + System.currentTimeMillis();
        String base = agentName.trim().toLowerCase()
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-+|-+$", "");
        return base.isEmpty() ? "agent-" + System.currentTimeMillis() : base;
    }

    @Override
    protected IPage<RagAgentEntity> pageQuery(RagAgentQueryParam q, IPage<RagAgentEntity> page) {
        // 透传 isSystem 过滤 (null=全部, 1=系统智能体, 0=内建用户智能体)
        return ragAgentMapper.pageAgents(page, q.getStatus(), q.getIsSystem(), PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    protected long countQuery(RagAgentQueryParam q) {
        return ragAgentMapper.countAgents(q.getStatus(), q.getIsSystem(), PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    public PageResponse<RagAgentVO> pageList(RagAgentQueryParam query) {
        PageResponse<RagAgentVO> resp = super.pageList(query);
        if (!resp.getRecords().isEmpty()) {
            List<Long> ids = resp.getRecords().stream().map(RagAgentVO::getId).toList();
            Map<Long, List<Long>> skillMap = batchLoadSkillIds(ids);
            for (RagAgentVO vo : resp.getRecords()) {
                vo.setSkillIds(skillMap.getOrDefault(vo.getId(), List.of()));
            }
        }
        return resp;
    }

    @Override
    protected RagAgentVO toVO(RagAgentEntity e) {
        RagAgentVO v = new RagAgentVO();
        v.setId(e.getId());
        v.setAgentName(e.getAgentName());
        v.setAgentKey(e.getAgentKey());
        v.setIsSystem(e.getIsSystem());
        v.setDescription(e.getDescription());
        v.setSystemPrompt(e.getSystemPrompt());
        v.setContent(e.getContent());
        v.setTemperature(e.getTemperature());
        v.setMaxTokens(e.getMaxTokens());
        v.setStatus(e.getStatus());
        v.setVersion(e.getVersion());
        v.setPublishedAt(e.getPublishedAt());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        v.setCreatedBy(e.getCreatedBy());
        return v;
    }

    @Override
    public List<RagAgentVO> listAll() {
        return ragAgentMapper.selectList(new QueryWrapper<RagAgentEntity>().orderByDesc("id"))
                .stream().map(this::toVO).toList();
    }

    @Override
    public RagAgentVO getById(Long id) {
        RagAgentEntity e = ragAgentMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Agent 不存在: id=" + id);
        }
        RagAgentVO vo = toVO(e);
        vo.setSkillIds(loadSkillIds(id));
        return vo;
    }

    @Override
    @Transactional
    public Long create(RagAgentSaveParam param, Long userId) {
        // 校验 isSystem (1=系统智能体, 0=内建用户智能体), 必填
        Integer isSystem = param.getIsSystem();
        if (isSystem == null || (isSystem != 0 && isSystem != 1)) {
            throw new BizException(ResultCode.PARAM_INVALID, "isSystem 必须为 0(内建用户智能体) 或 1(系统智能体)");
        }
        // 校验 prompt 字段与类型一致: 系统智能体必填 systemPrompt, 内建用户智能体必填 content
        if (isSystem == 1 && (param.getSystemPrompt() == null || param.getSystemPrompt().isBlank())) {
            throw new BizException(ResultCode.PARAM_INVALID, "系统智能体必须填写 systemPrompt");
        }
        if (isSystem == 0 && (param.getContent() == null || param.getContent().isBlank())) {
            throw new BizException(ResultCode.PARAM_INVALID, "内建用户智能体必须填写 content (user prompt)");
        }
        // 走 XML insertAgent: 传 RagAgentEntity POJO, 反射调 getter, 彻底绕开 OGNL Map 解析陷阱
        RagAgentEntity e = new RagAgentEntity();
        e.setAgentName(param.getAgentName());
        e.setAgentKey(slugifyAgentKey(param.getAgentName(), param.getAgentKey()));
        e.setIsSystem(isSystem);
        e.setDescription(param.getDescription());
        e.setSystemPrompt(param.getSystemPrompt());
        e.setContent(param.getContent());
        e.setTemperature(param.getTemperature());
        e.setMaxTokens(param.getMaxTokens());
        e.setStatus(param.getStatus() == null ? 0 : param.getStatus());
        e.setVersion(0);
        e.setCreatedBy(userId);
        ragAgentMapper.insertAgent(e);
        Long newId = e.getId();
        replaceSkills(newId, param.getSkillIds());
        return newId;
    }

    @Override
    @Transactional
    public boolean update(Long id, RagAgentSaveParam param) {
        RagAgentEntity e = ragAgentMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Agent 不存在: id=" + id);
        }
        if (param.getAgentName() != null) e.setAgentName(param.getAgentName());
        if (param.getAgentKey() != null) e.setAgentKey(param.getAgentKey());
        // isSystem 允许修改 (admin 可调整类型), 但必须 0/1, 且若该 Agent 是当前默认则不能改成内建
        if (param.getIsSystem() != null) {
            if (param.getIsSystem() != 0 && param.getIsSystem() != 1) {
                throw new BizException(ResultCode.PARAM_INVALID, "isSystem 必须为 0 或 1");
            }
            Long curDefault = getDefaultId();
            if (curDefault != null && curDefault.equals(id) && param.getIsSystem() == 0) {
                throw new BizException(ResultCode.PARAM_INVALID, "默认 Agent 不能改为内建用户智能体, 请先取消默认");
            }
            e.setIsSystem(param.getIsSystem());
        }
        if (param.getDescription() != null) e.setDescription(param.getDescription());
        if (param.getSystemPrompt() != null) e.setSystemPrompt(param.getSystemPrompt());
        if (param.getContent() != null) e.setContent(param.getContent());
        if (param.getTemperature() != null) e.setTemperature(param.getTemperature());
        if (param.getMaxTokens() != null) e.setMaxTokens(param.getMaxTokens());
        if (param.getStatus() != null) e.setStatus(param.getStatus());
        boolean ok = ragAgentMapper.updateById(e) > 0;
        if (param.getSkillIds() != null) {
            replaceSkills(id, param.getSkillIds());
        }
        return ok;
    }

    @Override
    @Transactional
    public boolean publish(Long id) {
        RagAgentEntity e = ragAgentMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Agent 不存在: id=" + id);
        }
        return ragAgentMapper.updateAgentStatus(id, 1, e.getVersion()) > 0;
    }

    @Override
    @Transactional
    public boolean unpublish(Long id) {
        return ragAgentMapper.updateAgentStatus(id, 0, 0) > 0;
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        ragAgentSkillMapper.deleteByAgentId(id);
        // 取消默认: 如果是当前默认 Agent, 一并清空 default_agent_id
        Long curDefault = getDefaultId();
        if (curDefault != null && curDefault.equals(id)) {
            unsetDefault();
        }
        return ragAgentMapper.deleteAgent(id) > 0;
    }

    /**
     * 设置默认 Agent: 校验存在+已发布, 写入 sys_config(AGENT, default_agent_id, id).
     * <p>写入前检查: 已有默认 Agent 时静默覆盖, 无需前端"先取消再设置" (符合 UX 直觉).
     */
    @Override
    @Transactional
    public boolean setDefault(Long agentId) {
        RagAgentEntity e = ragAgentMapper.selectById(agentId);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "Agent 不存在: id=" + agentId);
        }
        if (e.getStatus() == null || e.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "只能将已发布的 Agent 设为默认");
        }
        // 默认 Agent 必须是系统智能体 (isSystem=1), C 端用户不可见, 后端强制使用
        if (e.getIsSystem() == null || e.getIsSystem() != 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "默认 Agent 必须是系统智能体, 预置用户智能体不可作为默认");
        }
        int n = sysConfigMapper.upsertConfig(
            "AGENT", "default_agent_id", String.valueOf(agentId),
            "NUMBER", 0, "系统默认 Agent ID (前端未指定时强制使用)");
        return n > 0;
    }

    /**
     * 取消默认: 物理删除 sys_config 行, 不保留历史.
     */
    @Override
    @Transactional
    public boolean unsetDefault() {
        try {
            sysConfigMapper.delete(new QueryWrapper<com.jc.rag.domain.entity.sys.SysConfigEntity>()
                .eq("config_key", "default_agent_id"));
        } catch (Exception ignored) {}
        return true;
    }

    @Override
    public Long getDefaultId() {
        try {
            String v = sysConfigMapper.selectStringValue("default_agent_id");
            if (v == null || v.isBlank()) return null;
            return Long.valueOf(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public PageResponse<Map<String, Object>> pageListAsMap(String keyword, String status, PageRequest pageRequest) {
        Integer s = parseStatus(status);
        String like = PageHelper.buildLikeClause(keyword);
        IPage<RagAgentEntity> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize(), false);
        page = ragAgentMapper.pageAgents(page, s, null, like);
        page.setTotal(ragAgentMapper.countAgents(s, null, like));
        return PageHelper.toPageResponse(page, this::toMap);
    }

    /** 兼容旧前端: entity -> Map (时间字段格式化, 含 skillIds). */
    private Map<String, Object> toMap(RagAgentEntity e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("agentName", e.getAgentName());
        m.put("agentKey", e.getAgentKey());
        m.put("isSystem", e.getIsSystem());
        m.put("description", e.getDescription());
        m.put("systemPrompt", e.getSystemPrompt());
        m.put("temperature", e.getTemperature());
        m.put("maxTokens", e.getMaxTokens());
        m.put("status", e.getStatus());
        m.put("version", e.getVersion());
        m.put("publishedAt", TimeFormatUtil.format(e.getPublishedAt()));
        m.put("createdAt", TimeFormatUtil.format(e.getCreatedAt()));
        m.put("updatedAt", TimeFormatUtil.format(e.getUpdatedAt()));
        m.put("createdBy", e.getCreatedBy());
        m.put("skillIds", loadSkillIds(e.getId()));
        return m;
    }

    /* ============================================================ */
    /* 内部: Skill 关联                                              */
    /* ============================================================ */
    private List<Long> loadSkillIds(Long agentId) {
        List<RagAgentSkillEntity> list = ragAgentSkillMapper.selectList(
                new QueryWrapper<RagAgentSkillEntity>().eq("agent_id", agentId));
        return list.stream().map(RagAgentSkillEntity::getSkillId).toList();
    }

    private Map<Long, List<Long>> batchLoadSkillIds(List<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) return Map.of();
        List<RagAgentSkillEntity> rows = ragAgentSkillMapper.selectList(
                new QueryWrapper<RagAgentSkillEntity>().in("agent_id", agentIds));
        Map<Long, List<Long>> out = new HashMap<>();
        for (Long id : agentIds) out.put(id, new ArrayList<>());
        for (RagAgentSkillEntity x : rows) {
            out.computeIfAbsent(x.getAgentId(), k -> new ArrayList<>()).add(x.getSkillId());
        }
        return out;
    }

    private void replaceSkills(Long agentId, List<Long> skillIds) {
        ragAgentSkillMapper.deleteByAgentId(agentId);
        if (skillIds == null || skillIds.isEmpty()) return;
        List<RagAgentSkillEntity> list = new ArrayList<>();
        int sort = 0;
        for (Long sid : skillIds) {
            RagAgentSkillEntity x = new RagAgentSkillEntity();
            x.setAgentId(agentId);
            x.setSkillId(sid);
            x.setSortOrder(sort++);
            list.add(x);
        }
        ragAgentSkillMapper.insertBatch(list);
    }

    private static Integer parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }
}
