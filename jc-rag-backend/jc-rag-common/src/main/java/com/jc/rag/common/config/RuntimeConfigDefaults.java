package com.jc.rag.common.config;

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

    public static final String EMBEDDING_MODEL_PATH = "D:\\models\\embedding\\bge-large-zh-v1.5";
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
}
