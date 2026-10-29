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

import com.jc.rag.admin.module.sys.model.param.DirBrowseParam;
import com.jc.rag.admin.module.sys.model.param.EmbeddingVerifyParam;
import com.jc.rag.admin.module.sys.model.vo.DirBrowseVO;
import com.jc.rag.admin.module.sys.model.vo.EmbeddingVerifyVO;
import com.jc.rag.admin.module.sys.model.vo.OllamaModelVO;
import com.jc.rag.admin.module.sys.model.vo.SystemResourceVO;
import com.jc.rag.admin.module.sys.service.RuntimeConfigService;
import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 系统扩展 Controller (Ollama 模型列表 / 设备 / 模型验证 / 目录浏览)。
 * <p>路径: /api/sys/...
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys")
@RequiredArgsConstructor
public class SysExtraController {

    private final RuntimeConfigService runtimeConfigService;

    /** Ollama 模型列表 (调用用户在系统设置中配置的 Ollama /api/tags)。 */
    @GetMapping("/ollama/models")
    public R<OllamaModelVO> ollamaModels() {
        return R.ok(runtimeConfigService.listOllamaModels());
    }

    /** Embedding 设备列表 (Python RAG /system/devices)。 */
    @GetMapping("/devices")
    public R<List<Map<String, Object>>> devices() {
        return R.ok(runtimeConfigService.detectDevices());
    }

    /** 系统资源 (CPU/GPU/RAM)。 */
    @GetMapping("/resources")
    public R<SystemResourceVO> resources() {
        return R.ok(runtimeConfigService.getSystemResources());
    }

    /** 验证 Embedding 模型路径. */
    @PostMapping("/embedding/verify")
    public R<EmbeddingVerifyVO> verifyEmbedding(@RequestBody(required = false) EmbeddingVerifyParam param) {
        return R.ok(runtimeConfigService.verifyEmbeddingModel(param == null ? null : param.getModelPath()));
    }

    /** 浏览目录 (用于模型路径选择器). */
    @PostMapping("/browse-directory")
    public R<DirBrowseVO> browseDirectory(@RequestBody(required = false) DirBrowseParam param) {
        return R.ok(runtimeConfigService.browseDirectory(param == null ? null : param.getPath()));
    }

    @GetMapping("/local-models")
    public R<List<Map<String, Object>>> localModels() { return R.ok(runtimeConfigService.listLocalModels()); }

    @PostMapping("/local-models/download")
    public R<Map<String, Object>> downloadLocalModel(@RequestBody Map<String, Object> body) {
        return R.ok(runtimeConfigService.downloadLocalModel(String.valueOf(body.getOrDefault("modelId", ""))));
    }

    /** 兼容旧前端: /api/system/devices (旧 sysConfig 页降级方案)。 */
    @Deprecated
    @GetMapping("/_legacy/devices")
    public R<List<Map<String, Object>>> legacyDevices() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("id", "cpu");
        cpu.put("type", "CPU");
        cpu.put("name", System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "Unknown CPU"));
        cpu.put("cores", Runtime.getRuntime().availableProcessors());
        list.add(cpu);

        Map<String, Object> mem = new LinkedHashMap<>();
        mem.put("id", "memory");
        mem.put("type", "MEMORY");
        mem.put("name", "JVM Memory");
        mem.put("maxBytes", Runtime.getRuntime().maxMemory());
        mem.put("totalBytes", Runtime.getRuntime().totalMemory());
        mem.put("freeBytes", Runtime.getRuntime().freeMemory());
        list.add(mem);

        try {
            java.io.File root = new java.io.File(System.getProperty("user.home"));
            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("id", "disk-" + root.getAbsolutePath());
            disk.put("type", "DISK");
            disk.put("name", root.getAbsolutePath());
            disk.put("totalBytes", root.getTotalSpace());
            disk.put("freeBytes", root.getUsableSpace());
            list.add(disk);
        } catch (Exception e) {
            log.warn("设备-磁盘信息失败: {}", e.getMessage());
        }

        Map<String, Object> gpu = new LinkedHashMap<>();
        gpu.put("id", "gpu");
        gpu.put("type", "GPU");
        gpu.put("name", "GPU (未检测)");
        gpu.put("available", false);
        list.add(gpu);
        return R.ok(list);
    }

    /** 兼容旧前端: /api/system/embedding/verify */
    @Deprecated
    @PostMapping("/_legacy/embedding/verify")
    public R<Map<String, Object>> legacyVerifyEmbedding(@RequestBody Map<String, Object> body) {
        Map<String, Object> data = new HashMap<>();
        Object modelPath = body.get("modelPath");
        data.put("modelPath", modelPath);
        if (modelPath == null || modelPath.toString().isEmpty()) {
            data.put("status", "FAILED");
            data.put("message", "模型路径为空");
            return R.ok(data);
        }
        Path p = Paths.get(modelPath.toString());
        if (!Files.exists(p)) {
            data.put("status", "FAILED");
            data.put("message", "路径不存在");
        } else if (Files.isDirectory(p)) {
            data.put("status", "SUCCESS");
            data.put("message", "目录存在");
        } else {
            data.put("status", "SUCCESS");
            data.put("message", "文件存在, size=" + p.toFile().length());
        }
        return R.ok(data);
    }

    /** 兼容旧前端: /api/system/browse-directory */
    @Deprecated
    @PostMapping("/_legacy/browse-directory")
    public R<Map<String, Object>> legacyBrowseDirectory(@RequestBody Map<String, Object> body) {
        Map<String, Object> data = new HashMap<>();
        String path = body.get("path") == null ? "" : body.get("path").toString();
        Path p = path.isEmpty() ? Paths.get(System.getProperty("user.home")) : Paths.get(path);
        data.put("path", p.toString());
        try (Stream<Path> stream = Files.list(p)) {
            List<Map<String, Object>> entries = new ArrayList<>();
            stream.sorted().forEach(child -> {
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("name", child.getFileName().toString());
                e.put("path", child.toString());
                e.put("isDirectory", Files.isDirectory(child));
                try { e.put("size", Files.isDirectory(child) ? 0L : Files.size(child)); } catch (IOException ignore) { e.put("size", 0L); }
                entries.add(e);
            });
            data.put("entries", entries);
            data.put("status", "SUCCESS");
        } catch (Exception e) {
            data.put("status", "FAILED");
            data.put("message", e.getMessage());
            data.put("entries", new ArrayList<>());
        }
        return R.ok(data);
    }
}
