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


package com.jc.rag.admin;

import com.jc.rag.admin.config.AppProperties;
import com.jc.rag.admin.config.ExternalServiceProperties;
import com.jc.rag.admin.module.sys.service.RuntimeConfigService;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Map;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(
    basePackages = "com.jc.rag",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.jc\\.rag\\.common\\.controller\\..*"
    )
)
@MapperScan({"com.jc.rag.common.mapper", "com.jc.rag.domain.mapper", "com.jc.rag.admin.module.*.mapper"})
public class RagServerApplication {

    private static final Logger log = LoggerFactory.getLogger(RagServerApplication.class);

    private final Environment environment;
    private final RuntimeConfigService runtimeConfigService;
    private final ExternalServiceProperties externalServices;
    private final AppProperties appProperties;

    public RagServerApplication(
        Environment environment,
        RuntimeConfigService runtimeConfigService,
        ExternalServiceProperties externalServices,
        AppProperties appProperties
    ) {
        this.environment = environment;
        this.runtimeConfigService = runtimeConfigService;
        this.externalServices = externalServices;
        this.appProperties = appProperties;
    }

    public static void main(String[] args) {
        com.jc.rag.common.banner.JcRagStartupBanner.markStart();
        SpringApplication.run(RagServerApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printAccessUrls() {
        String serverPort = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "/");
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        // 本服务地址由实际监听端口拼出, 不再依赖 yml 的 service.admin-backend (已移除)
        String adminBase = "http://localhost:" + serverPort + ("/".equals(contextPath) ? "" : contextPath);
        // Python RAG 地址走 yml 配置 (service.python.rag.base-url), sys_config 有值时运行时以 sys_config 为准
        String pythonRagBase = externalServices.getPython().getRag().getBaseUrl();

        log.info("App: {} {} ({})", appProperties.getName(), appProperties.getVersion(), appProperties.getDeploymentMode());
        log.info("SpringBoot API: {}/api/system/runtime-config", adminBase);
        log.info("SpringBoot Health: {}/api/health/basic", adminBase);
        log.info("Python RAG: {}/health", pythonRagBase);

        // 异步尝试拉取一次运行时配置，验证 DB 可达
        try {
            runtimeConfigService.buildResponse();
        } catch (Exception ex) {
            log.warn("Load runtime config for startup log failed: {}", ex.getMessage());
        }
    }
}

