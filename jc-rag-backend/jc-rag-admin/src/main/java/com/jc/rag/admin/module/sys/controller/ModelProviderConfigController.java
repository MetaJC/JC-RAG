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
import com.jc.rag.admin.module.sys.model.param.ModelProviderSaveParam;
import com.jc.rag.admin.module.sys.model.param.ModelProviderTestParam;
import com.jc.rag.admin.module.sys.service.ModelProviderConfigService;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/model/providers")
@RequiredArgsConstructor
public class ModelProviderConfigController {
    private final ModelProviderConfigService service;
    @GetMapping public R<?> list() { return R.ok(service.list()); }
    @PostMapping @OperationLog(module = "MODEL", action = "SAVE_PROVIDER", description = "保存模型服务商配置")
    public R<?> save(@RequestBody @Valid ModelProviderSaveParam param) { return R.ok(service.save(param)); }
    @DeleteMapping("/{id}") @OperationLog(module = "MODEL", action = "DELETE_PROVIDER", description = "删除模型服务商配置")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }
    @PostMapping("/{id}/models/refresh") public R<?> refreshModels(@PathVariable Long id) { return R.ok(service.refreshModels(id)); }
    @PostMapping("/test") public R<?> test(@RequestBody @Valid ModelProviderTestParam param) { return R.ok(service.testConnection(param)); }
    @PostMapping("/{id}/activate") @OperationLog(module = "MODEL", action = "ACTIVATE_PROVIDER", description = "启用主模型服务")
    public R<Void> activate(@PathVariable Long id) { service.activate(id); return R.ok(); }
}
