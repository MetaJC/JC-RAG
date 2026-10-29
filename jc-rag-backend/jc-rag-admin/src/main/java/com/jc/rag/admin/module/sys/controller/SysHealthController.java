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

import com.jc.rag.admin.module.sys.model.vo.ServiceHealthItemVO;
import com.jc.rag.admin.module.sys.service.SystemHealthService;
import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统服务健康 Controller (SpringBoot / Python RAG / Ollama / ES / MinIO)。
 * <p>路径: /api/sys/health/services
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/health/services")
@RequiredArgsConstructor
public class SysHealthController {

    private final SystemHealthService systemHealthService;

    @GetMapping
    public R<List<ServiceHealthItemVO>> get() {
        return R.ok(systemHealthService.collectHealthItems());
    }
}
