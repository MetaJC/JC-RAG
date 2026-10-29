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


package com.jc.rag.user.module.user.controller;

import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.domain.entity.user.UserCustomAgentEntity;
import com.jc.rag.user.module.user.service.UserCustomAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端用户自定义智能体 Controller。
 * <p>只允许配置 user 层 prompt 片段, 不可触及 system 层 (由 Service 校验, 字段无 system_prompt)。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/user/custom-agents")
public class UserCustomAgentController {

    private final UserCustomAgentService service;

    @Autowired
    public UserCustomAgentController(UserCustomAgentService service) {
        this.service = service;
    }

    @GetMapping
    public R<List<UserCustomAgentEntity>> list(Authentication auth) {
        return R.ok(service.listByUser(uid(auth)));
    }

    @PostMapping
    public R<UserCustomAgentEntity> create(@RequestBody UserCustomAgentEntity input, Authentication auth) {
        return R.ok(service.create(uid(auth), input));
    }

    @PutMapping("/{id}")
    public R<UserCustomAgentEntity> update(@PathVariable Long id, @RequestBody UserCustomAgentEntity input, Authentication auth) {
        return R.ok(service.update(uid(auth), id, input));
    }

    @DeleteMapping("/{id}")
    public R<BaseOpResultVO> delete(@PathVariable Long id, Authentication auth) {
        service.delete(uid(auth), id);
        return R.ok(BaseOpResultVO.success(id, "删除成功"));
    }

    @GetMapping("/{id}")
    public R<UserCustomAgentEntity> get(@PathVariable Long id, Authentication auth) {
        return R.ok(service.get(uid(auth), id));
    }

    private Long uid(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CoreUserDetails u) {
            return u.getUserId();
        }
        return null;
    }
}
