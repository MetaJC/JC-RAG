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


package com.jc.rag.admin.module.rag.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.rag.model.param.RagPromptQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagPromptSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagPromptVO;
import com.jc.rag.admin.module.rag.service.RagPromptService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Prompt 管理 Controller (A 端管理后台)。
 * <p>路径: /api/rag/prompt, 与 rbac 范式一致。
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rag/prompt")
@RequiredArgsConstructor
public class RagPromptController {

    private final RagPromptService ragPromptService;

    @GetMapping
    public R<PageResponse<RagPromptVO>> list(RagPromptQueryParam query) {
        return R.ok(ragPromptService.pageList(query));
    }

    @GetMapping("/{id}")
    public R<RagPromptVO> get(@PathVariable Long id) {
        return R.ok(ragPromptService.getById(id));
    }

    @PostMapping
    @OperationLog(module = "RAG", action = "CREATE_PROMPT", description = "创建 Prompt")
    public R<Long> create(@RequestBody @Valid RagPromptSaveParam param, Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof DbUserDetails u ? u.getUserId() : null;
        return R.ok(ragPromptService.create(param, userId));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "RAG", action = "UPDATE_PROMPT", description = "更新 Prompt")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RagPromptSaveParam param) {
        return R.ok(ragPromptService.update(id, param));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "RAG", action = "DELETE_PROMPT", description = "删除 Prompt")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(ragPromptService.delete(id));
    }

    /** 兼容旧前端 */
    @GetMapping("/legacy")
    public R<PageResponse<Map<String, Object>>> legacy(
            @RequestParam(required = false) String scope,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(ragPromptService.pageListAsMap(keyword, scope, PageRequest.of(page, size)));
    }
}
