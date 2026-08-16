package com.jc.rag.common.config;

import java.nio.file.Path;

public final class RuntimeConfigDefaults {

    private RuntimeConfigDefaults() {
    }

    public static final String MODEL_ACTIVE_MODE = "offline";
    public static final double MODEL_TEMPERATURE = 0.3d;
    public static final double MODEL_TOP_P = 0.8d;
    public static final int MODEL_MAX_TOKENS = 1024;

    public static final String OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String OLLAMA_CHAT_PATH = "/api/chat";
    public static final String OLLAMA_TAGS_PATH = "/api/tags";
    public static final String OLLAMA_DEFAULT_MODEL = "qwen1.5-1.8b-gguf";

    public static final String ONLINE_BASE_URL = "https://api.example.com";
    public static final String ONLINE_CHAT_PATH = "/v1/chat/completions";
    public static final String ONLINE_MODEL_NAME = "gpt-4o-mini";

    /** All locally persisted JC RAG content is rooted in the current OS user's home directory. */
    public static final String JC_RAG_HOME = Path.of(System.getProperty("user.home"), ".jc-rag").toString();
    public static final String EMBEDDING_MODEL_PATH = Path.of(JC_RAG_HOME, "models", "embedding", "bge-m3").toString();
    public static final String EMBEDDING_DEVICE = "auto";
    public static final int EMBEDDING_DIMS = 1024;
    public static final int EMBEDDING_BATCH_SIZE = 32;
    public static final boolean EMBEDDING_NORMALIZE = true;

    public static final int RETRIEVAL_TOP_K = 5;
    public static final int RETRIEVAL_TOP_N = 20;
    public static final double RETRIEVAL_SIMILARITY_THRESHOLD = 0.6d;

    public static final int CHUNK_SIZE = 700;
    public static final int CHUNK_OVERLAP = 120;
    public static final int HISTORY_ROUNDS = 3;

    /** Embedding 来源: local=本地模型, service=在线模型服务 (/v1/embeddings). */
    public static final String EMBEDDING_SOURCE = "local";

    /** 模型目录远程更新地址 (L2), 空值表示禁用远程更新, 仅使用内置目录 (L1). */
    public static final String MODEL_CATALOG_REMOTE_URL = "";
    /** 模型目录远程缓存有效期 (小时), 过期后读取目录时自动重新拉取. */
    public static final int MODEL_CATALOG_TTL_HOURS = 168;
}
