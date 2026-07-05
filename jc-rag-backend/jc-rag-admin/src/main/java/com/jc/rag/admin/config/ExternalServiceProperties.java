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
 *   lmstudio:
 *     base-url: http://localhost:1234
 *   elasticsearch:
 *     base-url: http://localhost:9200
 *   minio:
 *     endpoint: http://localhost:9000
 *   frontend:
 *     base-url: http://localhost:5173
 *   admin-backend:
 *     base-url: http://localhost:8080
 * </pre>
 */
@ConfigurationProperties(prefix = "service")
public class ExternalServiceProperties {

    private Python python = new Python();
    private Ollama ollama = new Ollama();
    private Lmstudio lmstudio = new Lmstudio();
    private Elasticsearch elasticsearch = new Elasticsearch();
    private Minio minio = new Minio();
    private Frontend frontend = new Frontend();
    private AdminBackend adminBackend = new AdminBackend();

    public Python getPython() { return python; }
    public void setPython(Python python) { this.python = python; }

    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }

    public Lmstudio getLmstudio() { return lmstudio; }
    public void setLmstudio(Lmstudio lmstudio) { this.lmstudio = lmstudio; }

    public Elasticsearch getElasticsearch() { return elasticsearch; }
    public void setElasticsearch(Elasticsearch elasticsearch) { this.elasticsearch = elasticsearch; }

    public Minio getMinio() { return minio; }
    public void setMinio(Minio minio) { this.minio = minio; }

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

    public static class Lmstudio {
        private String baseUrl = "http://localhost:1234";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Elasticsearch {
        private String baseUrl = "http://localhost:9200";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Minio {
        private String endpoint = "http://localhost:9000";
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
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
