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
