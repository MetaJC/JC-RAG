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


package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.sys.model.param.SysLegalDocSaveParam;
import com.jc.rag.admin.module.sys.model.vo.SysLegalDocVO;
import com.jc.rag.admin.module.sys.service.SysLegalDocService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户协议 / 隐私政策 管理 Controller (A 端管理后台 -> 用户设置)。
 * <p>路径: /api/sys/legal-doc, 与 rbac/rag 范式一致。
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/legal-doc")
@RequiredArgsConstructor
public class SysLegalDocController {

    private final SysLegalDocService sysLegalDocService;

    @GetMapping
    public R<List<SysLegalDocVO>> list() {
        return R.ok(sysLegalDocService.listAll());
    }

    @GetMapping("/{docType}")
    public R<SysLegalDocVO> get(@PathVariable String docType) {
        return R.ok(sysLegalDocService.getByDocType(docType));
    }

    @PostMapping
    @OperationLog(module = "SYS", action = "SAVE_LEGAL_DOC", description = "保存/更新协议草稿或发布内容")
    public R<Boolean> save(@RequestBody @Valid SysLegalDocSaveParam param, Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof DbUserDetails u ? u.getUserId() : null;
        return R.ok(sysLegalDocService.save(param, userId));
    }

    @PostMapping("/{docType}/publish")
    @OperationLog(module = "SYS", action = "PUBLISH_LEGAL_DOC", description = "发布协议")
    public R<Boolean> publish(@PathVariable String docType, Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof DbUserDetails u ? u.getUserId() : null;
        return R.ok(sysLegalDocService.publish(docType, userId));
    }

    /** 上传 word/markdown, 解析后返回 Markdown 字符串, 不入库 */
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file) {
        return R.ok(sysLegalDocService.parseUploadedFile(file));
    }
}
