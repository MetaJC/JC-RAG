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


package com.jc.rag.user.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.security.PiiMasker;
import com.jc.rag.common.security.SensitiveFilterEngine;
import com.jc.rag.common.security.SensitiveFilterResult;
import com.jc.rag.domain.entity.user.UserCustomAgentEntity;
import com.jc.rag.domain.mapper.user.UserCustomAgentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * C 端用户自定义智能体 Service。
 * <p>只允许配置 user 层 prompt 片段, 不可触及 system 层。
 * <p>安全: 写入先经 PII 脱敏 + 敏感词过滤, DENY 直接拒绝, ALERT 放行, MASK 替换。
 * <p>配额: 单用户最多 maxPerUser 条 (默认 3, Pro 版 10), 通过 jc-rag.user-agent.max-per-user 配置。
 *
 * @author JC
 */
@Service
public class UserCustomAgentService {

    private static final Logger log = LoggerFactory.getLogger(UserCustomAgentService.class);

    /** 名称上限 */
    private static final int NAME_MAX = 30;
    /** 内容上限 (user prompt 片段) */
    private static final int CONTENT_MAX = 500;

    @Autowired
    private UserCustomAgentMapper mapper;

    /** 敏感词引擎: 来自 user 模块本地缓存 (从 admin 端 HTTP 拉取) */
    @Autowired
    private com.jc.rag.user.module.security.UserSensitiveWordCache sensitiveCache;

    @Value("${jc-rag.user-agent.max-per-user:3}")
    private int maxPerUser;

    public List<UserCustomAgentEntity> listByUser(Long userId) {
        if (userId == null) return Collections.emptyList();
        return mapper.selectList(new QueryWrapper<UserCustomAgentEntity>()
            .eq("user_id", userId)
            .eq("status", 1)
            .orderByDesc("updated_at"));
    }

    @Transactional
    public UserCustomAgentEntity create(Long userId, UserCustomAgentEntity input) {
        validateOwnership(userId, input);
        validateContent(input);
        // 配额校验
        Long count = mapper.selectCount(new QueryWrapper<UserCustomAgentEntity>()
            .eq("user_id", userId).eq("status", 1));
        if (count != null && count >= maxPerUser) {
            throw new AssertException(ResultCode.PARAM_INVALID, "user.custom.agent.quota.exceed", maxPerUser);
        }
        // 安全: PII 脱敏
        input.setContent(PiiMasker.mask(input.getContent()));
        // 安全: 敏感词过滤 (DENY 直接拒绝)
        SensitiveFilterResult r = sensitiveCache.currentEngine().filter(input.getContent());
        if (r.isDeny()) {
            log.warn("[UserCustomAgent] create denied for user={}, hits={}", userId, r.getHits().size());
            throw new AssertException(ResultCode.PARAM_INVALID, "user.custom.agent.sensitive.deny");
        }
        input.setId(null);
        input.setUserId(userId);
        if (input.getStatus() == null) input.setStatus(1);
        mapper.insert(input);
        return input;
    }

    @Transactional
    public UserCustomAgentEntity update(Long userId, Long id, UserCustomAgentEntity input) {
        UserCustomAgentEntity existing = requireOwned(userId, id);
        validateContent(input);
        // 安全过滤
        input.setContent(PiiMasker.mask(input.getContent()));
        SensitiveFilterResult r = sensitiveCache.currentEngine().filter(input.getContent());
        if (r.isDeny()) {
            throw new AssertException(ResultCode.PARAM_INVALID, "user.custom.agent.sensitive.deny");
        }
        existing.setAgentName(input.getAgentName());
        existing.setContent(input.getContent());
        existing.setStatus(input.getStatus() == null ? 1 : input.getStatus());
        mapper.updateById(existing);
        return existing;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        UserCustomAgentEntity existing = requireOwned(userId, id);
        mapper.deleteById(existing.getId());
    }

    public UserCustomAgentEntity get(Long userId, Long id) {
        return requireOwned(userId, id);
    }

    private UserCustomAgentEntity requireOwned(Long userId, Long id) {
        if (id == null) {
            throw new AssertException(ResultCode.PARAM_INVALID, "common.id.required");
        }
        UserCustomAgentEntity e = mapper.selectById(id);
        if (e == null) {
            throw new AssertException(ResultCode.NOT_FOUND, "user.custom.agent.not.found");
        }
        if (!Objects.equals(e.getUserId(), userId)) {
            throw new AssertException(ResultCode.FORBIDDEN, "user.custom.agent.no.permission");
        }
        return e;
    }

    private void validateOwnership(Long userId, UserCustomAgentEntity input) {
        if (userId == null) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        if (input == null) {
            throw new AssertException(ResultCode.PARAM_INVALID, "common.param.invalid");
        }
        if (input.getAgentName() == null || input.getAgentName().isBlank()) {
            throw new AssertException(ResultCode.PARAM_INVALID, "user.custom.agent.name.empty");
        }
        if (input.getAgentName().length() > NAME_MAX) {
            throw new AssertException(ResultCode.PARAM_INVALID, "user.custom.agent.name.too.long", NAME_MAX);
        }
    }

    private void validateContent(UserCustomAgentEntity input) {
        if (input.getContent() == null || input.getContent().isBlank()) {
            throw new AssertException(ResultCode.PARAM_INVALID, "user.custom.agent.content.empty");
        }
        if (input.getContent().length() > CONTENT_MAX) {
            throw new AssertException(ResultCode.PARAM_INVALID, "user.custom.agent.content.too.long", CONTENT_MAX);
        }
    }
}
