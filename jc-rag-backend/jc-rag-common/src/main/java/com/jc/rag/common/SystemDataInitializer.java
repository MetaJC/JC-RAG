package com.jc.rag.common;

import com.jc.rag.common.config.RuntimeConfigDefaults;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 系统首次启动数据初始化。
 * 创建 developer 兜底账号、admin 超级管理员账号、默认角色/权限、演示配置、系统提示词。
 * developer: 跳过所有鉴权，仅自身可见，不可删除
 * admin: 拥有全部权限但需鉴权
 */
public class SystemDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public SystemDataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            normalizeBuiltinUsersAndRoles();
            initRuntimeConfigs();
            String applied = jdbcTemplate.query(
                "SELECT config_value FROM sys_config WHERE config_key = ? LIMIT 1",
                rs -> rs.next() ? rs.getString(1) : null,
                "system.bootstrap.completed"
            );
            if ("1".equals(applied)) return;

            initUsers();
            initRoles();
            initPermissions();
            initUserRoles();
            initRolePermissions();
            initDemoConfig();
            initPrompts();
            initSkillsAndAgents();

            jdbcTemplate.update(
                "INSERT INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE config_value=VALUES(config_value)",
                "system", "system.bootstrap.completed", "1", "STRING", 0, "系统初始化完成标记"
            );
            System.out.println("[SystemDataInitializer] 系统初始化完成");
        } catch (Exception e) {
            System.out.println("[SystemDataInitializer] 初始化跳过（数据库可能尚未就绪）: " + e.getMessage());
        }
    }

    private void initUsers() {
        // developer - 兜底账号，跳过所有鉴权
        jdbcTemplate.update(
            "INSERT IGNORE INTO sys_user (username, password_hash, nickname, user_type, status, email) VALUES (?,?,?,?,?,?)",
            "developer", passwordEncoder.encode("developer@2024"), "开发者", "DEVELOPER", 1, "developer@jc-rag.local"
        );
        // admin - 超级管理员
        jdbcTemplate.update(
            "INSERT IGNORE INTO sys_user (username, password_hash, nickname, user_type, status, email) VALUES (?,?,?,?,?,?)",
            "admin", passwordEncoder.encode("123456"), "系统管理员", "ADMIN", 1, "admin@jc-rag.local"
        );
        // 普通演示用户
        jdbcTemplate.update(
            "INSERT IGNORE INTO sys_user (username, password_hash, nickname, user_type, status, email) VALUES (?,?,?,?,?,?)",
            "user01", passwordEncoder.encode("123456"), "张参谋", "NORMAL", 1, "user01@jc-rag.local"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO sys_user (username, password_hash, nickname, user_type, status, email) VALUES (?,?,?,?,?,?)",
            "user02", passwordEncoder.encode("123456"), "李分析员", "NORMAL", 1, "user02@jc-rag.local"
        );
    }

    private void initRoles() {
        jdbcTemplate.update("INSERT IGNORE INTO sys_role (role_code, role_name, role_desc) VALUES (?,?,?)", "super_admin", "超级管理员", "系统最高权限角色，可管理除 developer 兜底账号外的全部资源");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role (role_code, role_name, role_desc) VALUES (?,?,?)", "admin", "系统管理员", "负责日常后台运维、配置和审计管理");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role (role_code, role_name, role_desc) VALUES (?,?,?)", "analyst", "情报分析员", "可执行知识检索、问答分析和文档研判");
        jdbcTemplate.update("INSERT IGNORE INTO sys_role (role_code, role_name, role_desc) VALUES (?,?,?)", "viewer", "只读观察员", "仅允许查看结果和只读浏览业务数据");
    }

    private void initPermissions() {
        String[][] perms = {
            {"admin:dashboard", "仪表盘查看", "MENU", "/admin/dashboard", "GET"},
            {"admin:prompts", "提示词管理", "MENU", "/admin/prompts", "GET"},
            {"admin:prompts:edit", "提示词编辑", "BUTTON", null, null},
            {"admin:agents", "Agent管理", "MENU", "/admin/agents", "GET"},
            {"admin:agents:edit", "Agent编辑", "BUTTON", null, null},
            {"admin:users", "用户管理", "MENU", "/admin/users", "GET"},
            {"admin:users:edit", "用户编辑", "BUTTON", null, null},
            {"admin:demo-config", "演示模式配置", "MENU", "/admin/demo-config", "GET"},
            {"admin:data-permissions", "数据权限", "MENU", "/admin/data-permissions", "GET"},
            {"admin:knowledge-bases", "知识库管理", "MENU", "/admin/knowledge-bases", "GET"},
            {"user:chat", "问答功能", "API", "/api/chat/**", null},
            {"user:api-keys", "APIKey管理", "API", "/api/user/**", null},
            {"user:profile", "个人中心", "API", "/api/user/profile", null},
        };
        for (String[] p : perms) {
            jdbcTemplate.update(
                "INSERT IGNORE INTO sys_permission (perm_code, perm_name, perm_type, path, method) VALUES (?,?,?,?,?)",
                p[0], p[1], p[2], p[3], p[4]
            );
        }
    }

    private void initUserRoles() {
        // developer 不绑定任何角色（bypass逻辑）
        Long adminUserId = jdbcTemplate.query("SELECT id FROM sys_user WHERE username=?", rs -> rs.next() ? rs.getLong("id") : null, "admin");
        Long superRoleId = jdbcTemplate.query("SELECT id FROM sys_role WHERE role_code=?", rs -> rs.next() ? rs.getLong("id") : null, "super_admin");
        if (adminUserId != null && superRoleId != null) {
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?,?)", adminUserId, superRoleId);
        }
        // user01 -> ANALYST
        Long user01Id = jdbcTemplate.query("SELECT id FROM sys_user WHERE username=?", rs -> rs.next() ? rs.getLong("id") : null, "user01");
        Long analystRoleId = jdbcTemplate.query("SELECT id FROM sys_role WHERE role_code=?", rs -> rs.next() ? rs.getLong("id") : null, "analyst");
        if (user01Id != null && analystRoleId != null) {
            jdbcTemplate.update("INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (?,?)", user01Id, analystRoleId);
        }
    }

    private void initRolePermissions() {
        Long superRoleId = jdbcTemplate.query("SELECT id FROM sys_role WHERE role_code=?", rs -> rs.next() ? rs.getLong("id") : null, "super_admin");
        if (superRoleId != null) {
            jdbcTemplate.update("INSERT IGNORE INTO sys_role_permission (role_id, permission_id) SELECT ?, id FROM sys_permission", superRoleId);
        }
    }

    private void initDemoConfig() {
        Integer exists = jdbcTemplate.query("SELECT COUNT(*) FROM demo_config", rs -> rs.next() ? rs.getInt(1) : 0);
        if (exists == null || exists == 0) {
            jdbcTemplate.update("INSERT INTO demo_config (demo_mode_enabled, default_quota_per_day, default_tokens_per_q, default_tokens_per_day, upgrade_hint) VALUES (0, 20, 2048, 10000, '当日额度已用完。请在个人中心配置您自己的 API Key 以继续使用。')");
        }
    }

    private void initRuntimeConfigs() {
        insertConfigIfAbsent("model", "model.active.mode", RuntimeConfigDefaults.MODEL_ACTIVE_MODE, "STRING", 0, "当前模型模式");
        insertConfigIfAbsent("model", "model.default.temperature", String.valueOf(RuntimeConfigDefaults.MODEL_TEMPERATURE), "NUMBER", 0, "默认 temperature");
        insertConfigIfAbsent("model", "model.default.topP", String.valueOf(RuntimeConfigDefaults.MODEL_TOP_P), "NUMBER", 0, "默认 topP");
        insertConfigIfAbsent("model", "model.default.maxTokens", String.valueOf(RuntimeConfigDefaults.MODEL_MAX_TOKENS), "NUMBER", 0, "默认 max tokens");

        insertConfigIfAbsent("model", "model.offline.ollama.baseUrl", RuntimeConfigDefaults.OLLAMA_BASE_URL, "STRING", 0, "Ollama 地址");
        insertConfigIfAbsent("model", "model.offline.ollama.chatPath", RuntimeConfigDefaults.OLLAMA_CHAT_PATH, "STRING", 0, "Ollama 聊天接口");
        insertConfigIfAbsent("model", "model.offline.ollama.tagsPath", RuntimeConfigDefaults.OLLAMA_TAGS_PATH, "STRING", 0, "Ollama 模型列表接口");
        insertConfigIfAbsent("model", "model.offline.ollama.defaultModel", RuntimeConfigDefaults.OLLAMA_DEFAULT_MODEL, "STRING", 0, "默认离线模型");

        insertConfigIfAbsent("model", "model.online.baseUrl", RuntimeConfigDefaults.ONLINE_BASE_URL, "STRING", 0, "在线模型接口地址");
        insertConfigIfAbsent("model", "model.online.chatPath", RuntimeConfigDefaults.ONLINE_CHAT_PATH, "STRING", 0, "在线模型 chat path");
        insertConfigIfAbsent("model", "model.online.modelName", RuntimeConfigDefaults.ONLINE_MODEL_NAME, "STRING", 0, "在线模型名称");

        insertConfigIfAbsent("rag", "rag.embedding.model.path", RuntimeConfigDefaults.EMBEDDING_MODEL_PATH, "STRING", 0, "Embedding 模型路径");
        insertConfigIfAbsent("rag", "rag.embedding.device", RuntimeConfigDefaults.EMBEDDING_DEVICE, "STRING", 0, "Embedding 设备");
        insertConfigIfAbsent("rag", "rag.embedding.dims", String.valueOf(RuntimeConfigDefaults.EMBEDDING_DIMS), "NUMBER", 0, "Embedding 向量维度");
        insertConfigIfAbsent("rag", "rag.embedding.batchSize", String.valueOf(RuntimeConfigDefaults.EMBEDDING_BATCH_SIZE), "NUMBER", 0, "Embedding 批大小");
        insertConfigIfAbsent("rag", "rag.embedding.normalizeEmbeddings", String.valueOf(RuntimeConfigDefaults.EMBEDDING_NORMALIZE), "BOOLEAN", 0, "Embedding 向量归一化");

        insertConfigIfAbsent("rag", "rag.retrieval.topK", String.valueOf(RuntimeConfigDefaults.RETRIEVAL_TOP_K), "NUMBER", 0, "TopK");
        insertConfigIfAbsent("rag", "rag.retrieval.topN", String.valueOf(RuntimeConfigDefaults.RETRIEVAL_TOP_N), "NUMBER", 0, "TopN");
        insertConfigIfAbsent("rag", "rag.retrieval.similarityThreshold", String.valueOf(RuntimeConfigDefaults.RETRIEVAL_SIMILARITY_THRESHOLD), "NUMBER", 0, "相似度阈值");
        insertConfigIfAbsent("rag", "rag.chunk.size", String.valueOf(RuntimeConfigDefaults.CHUNK_SIZE), "NUMBER", 0, "Chunk Size");
        insertConfigIfAbsent("rag", "rag.chunk.overlap", String.valueOf(RuntimeConfigDefaults.CHUNK_OVERLAP), "NUMBER", 0, "Chunk Overlap");
        insertConfigIfAbsent("rag", "rag.history.rounds", String.valueOf(RuntimeConfigDefaults.HISTORY_ROUNDS), "NUMBER", 0, "历史轮数");

        // ===== Pro 付费功能开关 (Demo: AI 对话质量评估) =====
        // 社区版默认 quota=0 (用户端按钮不可见, 提示升级 Pro)
        // 付费版启动后, jc-rag-pro 模块会覆盖这两条 config 为 quota=999
        insertConfigIfAbsent("feature", "feature.dialogue_evaluation.enabled", "true", "BOOLEAN", 0, "Pro 功能: AI 对话质量评估 - 总开关");
        insertConfigIfAbsent("feature", "feature.dialogue_evaluation.daily_quota", "0", "NUMBER", 0, "Pro 功能: 每用户每日评估次数 (0=禁用, 999=无限制)");
    }

    private void initPrompts() {
        Integer exists = jdbcTemplate.query("SELECT COUNT(*) FROM sys_prompts", rs -> rs.next() ? rs.getInt(1) : 0);
        if (exists == null || exists == 0) {
            jdbcTemplate.update("INSERT INTO sys_prompts (prompt_key, prompt_name, prompt_content, scope, status, description) VALUES (?,?,?,?,?,?)",
                "chat_qa", "问答提示词（默认）", "你是军事领域RAG问答助手。优先基于给定知识片段作答。如果知识片段不足以回答，可以结合常识自由作答，不要输出固定话术。", "global", 1, "默认RAG问答提示词");
            jdbcTemplate.update("INSERT INTO sys_prompts (prompt_key, prompt_name, prompt_content, scope, status, description) VALUES (?,?,?,?,?,?)",
                "chat_general", "通用对话提示词", "直接用简洁的语言回答用户问题。", "global", 1, "无知识库命中时的通用提示词");
            jdbcTemplate.update("INSERT INTO sys_prompts (prompt_key, prompt_name, prompt_content, scope, status, description) VALUES (?,?,?,?,?,?)",
                "agent_default", "Agent默认提示词", "你是一个智能助手。请根据你的知识和上下文回答用户的问题。", "global", 1, "Agent默认系统提示词");
        }
    }

    private void initSkillsAndAgents() {
        Integer exists = jdbcTemplate.query("SELECT COUNT(*) FROM sys_skills", rs -> rs.next() ? rs.getInt(1) : 0);
        if (exists == null || exists > 0) return;
        jdbcTemplate.update("INSERT INTO sys_skills (skill_name, skill_key, description, trigger_type, status) VALUES (?,?,?,?,?)",
            "通用问答", "general_qa", "通用问答技能，处理日常问题", "auto", 1);
        jdbcTemplate.update("INSERT INTO sys_skills (skill_name, skill_key, description, trigger_type, status) VALUES (?,?,?,?,?)",
            "文档分析", "doc_analysis", "基于上传文档进行分析和总结", "keyword", 1);
    }

    private void normalizeBuiltinUsersAndRoles() {
        jdbcTemplate.update(
            "UPDATE sys_user SET nickname=?, user_type=?, status=1, deleted=0, email=? WHERE username=?",
            "开发者", "DEVELOPER", "developer@jc-rag.local", "developer"
        );
        jdbcTemplate.update("UPDATE sys_role SET role_code = LOWER(role_code)");
        jdbcTemplate.update(
            "UPDATE sys_role SET role_name=?, role_desc=? WHERE role_code=?",
            "超级管理员", "系统最高权限角色，可管理除 developer 兜底账号外的全部资源", "super_admin"
        );
        jdbcTemplate.update(
            "UPDATE sys_role SET role_name=?, role_desc=? WHERE role_code=?",
            "系统管理员", "负责日常后台运维、配置和审计管理", "admin"
        );
        jdbcTemplate.update(
            "UPDATE sys_role SET role_name=?, role_desc=? WHERE role_code=?",
            "情报分析员", "可执行知识检索、问答分析和文档研判", "analyst"
        );
        jdbcTemplate.update(
            "UPDATE sys_role SET role_name=?, role_desc=? WHERE role_code=?",
            "只读观察员", "仅允许查看结果和只读浏览业务数据", "viewer"
        );
    }

    private void insertConfigIfAbsent(String group, String key, String value, String valueType, int encrypted, String remark) {
        jdbcTemplate.update(
            "INSERT IGNORE INTO sys_config (config_group, config_key, config_value, value_type, encrypted, remark) VALUES (?,?,?,?,?,?)",
            group, key, value, valueType, encrypted, remark
        );
    }
}
