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


package com.jc.rag.admin.module.knowledge.controller;

import com.jc.rag.admin.module.knowledge.model.param.KbAuthSaveParam;
import com.jc.rag.admin.module.knowledge.model.vo.KbAuthDetailVO;
import com.jc.rag.admin.module.knowledge.service.KbAuthService;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库授权管理接口。
 * <p>对 knowledge-base 业务模块的二级入口, 路径仍挂载在 /api/knowledge-bases/{kbId}/auth,
 * <p>避免对一级 menu 造成结构破坏。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/knowledge-bases/{kbId}/auth")
public class KbAuthController {

    private final KbAuthService kbAuthService;

    public KbAuthController(KbAuthService kbAuthService) {
        this.kbAuthService = kbAuthService;
    }

    /** 获取知识库授权详情 (全量角色 + 全量 ADMIN 用户 + 当前选中状态) */
    @GetMapping
    public R<KbAuthDetailVO> getAuthDetail(@PathVariable Long kbId) {
        return R.ok(kbAuthService.getAuthDetail(kbId));
    }

    /** 全量替换保存知识库授权 */
    @PostMapping
    public R<Void> saveAuth(@PathVariable Long kbId, @Valid @RequestBody KbAuthSaveParam param) {
        kbAuthService.saveAuth(kbId, param);
        return R.ok();
    }
}
