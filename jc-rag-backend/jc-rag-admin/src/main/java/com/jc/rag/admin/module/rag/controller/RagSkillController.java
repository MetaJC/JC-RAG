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
import com.jc.rag.admin.module.rag.model.param.RagSkillQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagSkillSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagSkillVO;
import com.jc.rag.admin.module.rag.service.RagSkillService;
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
 * Skill 管理 Controller (A 端管理后台)。
 * <p>路径: /api/rag/skill, 与 rbac 范式一致。
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rag/skill")
@RequiredArgsConstructor
public class RagSkillController {

    private final RagSkillService ragSkillService;

    @GetMapping
    public R<PageResponse<RagSkillVO>> list(RagSkillQueryParam query) {
        return R.ok(ragSkillService.pageList(query));
    }

    @GetMapping("/{id}")
    public R<RagSkillVO> get(@PathVariable Long id) {
        return R.ok(ragSkillService.getById(id));
    }

    @PostMapping
    @OperationLog(module = "RAG", action = "CREATE_SKILL", description = "创建 Skill")
    public R<Long> create(@RequestBody @Valid RagSkillSaveParam param, Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof DbUserDetails u ? u.getUserId() : null;
        return R.ok(ragSkillService.create(param, userId));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "RAG", action = "UPDATE_SKILL", description = "更新 Skill")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RagSkillSaveParam param) {
        return R.ok(ragSkillService.update(id, param));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "RAG", action = "DELETE_SKILL", description = "删除 Skill")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(ragSkillService.delete(id));
    }

    /** 兼容旧前端 */
    @GetMapping("/legacy")
    public R<PageResponse<Map<String, Object>>> legacy(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(ragSkillService.pageListAsMap(keyword, status, PageRequest.of(page, size)));
    }
}
