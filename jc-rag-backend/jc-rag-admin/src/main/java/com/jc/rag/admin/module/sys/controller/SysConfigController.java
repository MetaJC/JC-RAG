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
import com.jc.rag.admin.module.sys.model.param.RuntimeConfigUpdateParam;
import com.jc.rag.admin.module.sys.model.vo.RuntimeConfigVO;
import com.jc.rag.admin.module.sys.service.RuntimeConfigService;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 运行时配置 Controller (A 端管理后台 + 系统设置页)。
 * <p>路径: /api/sys/runtime-config
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/runtime-config")
@RequiredArgsConstructor
public class SysConfigController {

    private final RuntimeConfigService runtimeConfigService;

    @GetMapping
    public R<RuntimeConfigVO> get() {
        return R.ok(runtimeConfigService.buildResponse());
    }

    @PostMapping
    @OperationLog(module = "SYS", action = "UPDATE_RUNTIME_CONFIG", description = "更新运行时配置")
    public R<RuntimeConfigVO> update(@RequestBody @Valid RuntimeConfigUpdateParam param) {
        return R.ok(runtimeConfigService.saveRuntimeConfig(param));
    }
}
