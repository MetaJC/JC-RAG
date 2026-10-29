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

import com.jc.rag.admin.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基础健康检查 Controller (供 Security 白名单 /api/health/** 免登录)。
 * <p>路径: /api/health/basic  (保留 SecurityConfig 白名单一致)
 * <p>类归属 sys 模块 (从 system 包迁移)。
 * @author JC
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class SysHealthBasicController {

    private final AppProperties appProperties;

    @GetMapping("/basic")
    public Map<String, Object> basic() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("application", appProperties.getName());
        response.put("deploymentMode", appProperties.getDeploymentMode());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
