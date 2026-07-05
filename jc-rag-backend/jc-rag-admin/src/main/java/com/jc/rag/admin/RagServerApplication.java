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
        // URL 全部走 yml 配置，不再硬编码
        String adminBase = externalServices.getAdminBackend().getBaseUrl();
        String pythonRagBase = externalServices.getPython().getRag().getBaseUrl();
        String frontendBase = externalServices.getFrontend().getBaseUrl();
        String ollamaBaseUrl = externalServices.getOllama().getBaseUrl();
        String ollamaTagsPath = externalServices.getOllama().getTagsPath();

        log.info("App: {} {} ({})", appProperties.getName(), appProperties.getVersion(), appProperties.getDeploymentMode());
        log.info("SpringBoot API: {}/api/system/runtime-config", adminBase);
        log.info("SpringBoot Health: {}/api/health/basic", adminBase);
        log.info("Python RAG: {}/health", pythonRagBase);
        log.info("Frontend: {}/system/settings", frontendBase);
        log.info("Ollama Tags: {}{}", ollamaBaseUrl, ollamaTagsPath);

        // 异步尝试拉取一次运行时配置，验证 DB 可达
        try {
            runtimeConfigService.buildResponse();
        } catch (Exception ex) {
            log.warn("Load runtime config for startup log failed: {}", ex.getMessage());
        }
    }
}

