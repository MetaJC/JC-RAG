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
import com.jc.rag.admin.module.sys.model.vo.ModelCatalogVO;
import com.jc.rag.admin.module.sys.service.ModelCatalogService;
import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模型目录接口 (三层同步机制的 L1/L2 入口)。
 * @author JC
 */
@RestController
@RequestMapping("/api/model/catalog")
@RequiredArgsConstructor
public class ModelCatalogController {

    private final ModelCatalogService service;

    /** 获取当前生效的模型目录 (TTL 内远程缓存 &gt; 远程拉取 &gt; 过期缓存 &gt; 内置目录) */
    @GetMapping
    public R<ModelCatalogVO> catalog() {
        return R.ok(service.getCatalog());
    }

    /** 强制从远程地址更新模型目录缓存 (L2) */
    @PostMapping("/refresh")
    @OperationLog(module = "MODEL", action = "REFRESH_CATALOG", description = "更新远程模型目录")
    public R<ModelCatalogVO> refresh() {
        return R.ok(service.refreshCatalog());
    }
}
