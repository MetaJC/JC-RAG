package com.jc.rag.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部服务地址配置。
 * <p>所有跨进程/跨机器调用的服务 URL 必须在这里声明，yml/properties/nacos 中赋值，
 * 严禁在业务代码中硬编码。
 *
 * <pre>
 * service:
 *   python:
 *     rag:
 *       base-url: http://localhost:8001
 *   ollama:
 *     base-url: http://localhost:11434
 *     chat-path: /api/chat
 *     tags-path: /api/tags
 *   frontend:
 *     base-url: http://localhost:5173
 *   admin-backend:
 *     base-url: http://localhost:8080
 * </pre>
 *
 * <p>注意: ollama 运行时实际地址由后台页面配置 (sys_config model.offline.ollama.*),
 * 本类 ollama 配置仅用于启动日志展示; 已移除无代码引用的 lmstudio / elasticsearch /
 * service.minio 配置 (MinIO 实际走 storage.minio.*, ES 实际走 elasticsearch.url).
 * @author JC
 */
@ConfigurationProperties(prefix = "service")
public class ExternalServiceProperties {

    private Python python = new Python();
    private Ollama ollama = new Ollama();
    private Frontend frontend = new Frontend();
    private AdminBackend adminBackend = new AdminBackend();

    public Python getPython() { return python; }
    public void setPython(Python python) { this.python = python; }

    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }

    public Frontend getFrontend() { return frontend; }
    public void setFrontend(Frontend frontend) { this.frontend = frontend; }

    public AdminBackend getAdminBackend() { return adminBackend; }
    public void setAdminBackend(AdminBackend adminBackend) { this.adminBackend = adminBackend; }

    public static class Python {
        private Rag rag = new Rag();
        public Rag getRag() { return rag; }
        public void setRag(Rag rag) { this.rag = rag; }
        public static class Rag {
            private String baseUrl;
            public String getBaseUrl() { return baseUrl; }
            public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        }
    }

    public static class Ollama {
        private String baseUrl = "http://localhost:11434";
        private String chatPath = "/api/chat";
        private String tagsPath = "/api/tags";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getChatPath() { return chatPath; }
        public void setChatPath(String chatPath) { this.chatPath = chatPath; }
        public String getTagsPath() { return tagsPath; }
        public void setTagsPath(String tagsPath) { this.tagsPath = tagsPath; }
    }

    public static class Frontend {
        private String baseUrl = "http://localhost:5173";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class AdminBackend {
        private String baseUrl = "http://localhost:8080";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
