package com.jc.rag.user.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.domain.entity.chat.ChatMessageEntity;
import com.jc.rag.domain.entity.chat.ChatSessionEntity;
import com.jc.rag.domain.entity.chat.ChatSessionSummaryEntity;
import com.jc.rag.domain.entity.chat.UserMemoryProfileEntity;
import com.jc.rag.domain.entity.sys.SysConfigEntity;
import com.jc.rag.domain.mapper.chat.ChatMessageMapper;
import com.jc.rag.domain.mapper.chat.ChatSessionMapper;
import com.jc.rag.domain.mapper.chat.ChatSessionSummaryMapper;
import com.jc.rag.domain.mapper.chat.UserMemoryProfileMapper;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc.rag.user.module.user.model.param.MemoryFileSaveParam;
import com.jc.rag.user.module.user.model.param.UserPreferenceUpdateParam;
import com.jc.rag.user.module.user.model.vo.MemoryFileVO;
import com.jc.rag.user.module.user.model.vo.UserPreferenceVO;
import com.jc.rag.user.module.user.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UserPreferenceService 实现 (C 端): 复用 sys_config 表, 避免新增表。
 * <p>存储约定:
 * <ul>
 *   <li>config_group = 'user_pref'</li>
 *   <li>config_key = '{userId}.{preferenceKey}' (如 '1001.memoryFilePath')</li>
 *   <li>config_value = 偏好值字符串</li>
 *   <li>value_type = STRING</li>
 * </ul>
 * <p>读取走 QueryWrapper 按 (group, key) 精准匹配, 避免跨分组误命中。
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final String GROUP = "user_pref";

    /** 偏好项 Key 后缀 */
    private static final String K_MEMORY_FILE_PATH = "memoryFilePath";
    private static final String K_DEFAULT_MODEL_CODE = "defaultModelCode";
    private static final String K_CHAT_STYLE = "chatStyle";
    private static final String K_ENABLE_LONG_MEMORY = "enableLongMemory";
    private static final String K_ENABLE_MEMORY_PROFILE = "enableMemoryProfile";

    private static final String REMARK_MEMORY_FILE_PATH = "用户偏好 - 长记忆文件路径 (Markdown)";
    private static final String REMARK_DEFAULT_MODEL_CODE = "用户偏好 - 默认模型 code";
    private static final String REMARK_CHAT_STYLE = "用户偏好 - 回答风格";
    private static final String REMARK_ENABLE_LONG_MEMORY = "用户偏好 - L2 长期记忆开关 (true/false)";
    private static final String REMARK_ENABLE_MEMORY_PROFILE = "用户偏好 - L3 跨会话画像开关 (true/false)";

    /** 系统默认模型 code 标识 (前端拿到此值时按"未设置"处理) */
    public static final String USE_GLOBAL_DEFAULT = "";

    /** 默认回答风格 */
    private static final String DEFAULT_CHAT_STYLE = "concise";
    /** L2 长期记忆默认开启 */
    private static final boolean DEFAULT_ENABLE_LONG_MEMORY = true;
    /** L3 跨会话画像默认开启 */
    private static final boolean DEFAULT_ENABLE_MEMORY_PROFILE = true;

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;

    @Value("${service.python.rag.base-url:http://localhost:8001}")
    private String ragBaseUrl;

    private static final HttpClient FORGET_HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build();

    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatSessionSummaryMapper chatSessionSummaryMapper;
    private final UserMemoryProfileMapper userMemoryProfileMapper;

    @Override
    public UserPreferenceVO load(Long userId) {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setMemoryFilePath(readString(userId, K_MEMORY_FILE_PATH, defaultMemoryFilePath(userId)));
        vo.setDefaultModelCode(readString(userId, K_DEFAULT_MODEL_CODE, USE_GLOBAL_DEFAULT));
        vo.setChatStyle(readString(userId, K_CHAT_STYLE, defaultChatStyle()));
        vo.setEnableLongMemory(readBoolean(userId, K_ENABLE_LONG_MEMORY, DEFAULT_ENABLE_LONG_MEMORY));
        vo.setEnableMemoryProfile(readBoolean(userId, K_ENABLE_MEMORY_PROFILE, DEFAULT_ENABLE_MEMORY_PROFILE));
        return vo;
    }

    @Override
    public UserPreferenceVO update(Long userId, UserPreferenceUpdateParam param) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (param == null) {
            return load(userId);
        }
        if (param.getMemoryFilePath() != null) {
            String v = param.getMemoryFilePath().trim();
            if (v.isEmpty()) {
                v = defaultMemoryFilePath(userId);
            }
            writeString(userId, K_MEMORY_FILE_PATH, v, REMARK_MEMORY_FILE_PATH);
            log.info("用户偏好更新: userId={}, memoryFilePath={}", userId, v);
        }
        if (param.getDefaultModelCode() != null) {
            // 允许传空字符串表示"跟随系统默认", 显式写空串存到库
            String v = param.getDefaultModelCode().trim();
            writeString(userId, K_DEFAULT_MODEL_CODE, v, REMARK_DEFAULT_MODEL_CODE);
            log.info("用户偏好更新: userId={}, defaultModelCode={}", userId, v);
        }
        if (param.getChatStyle() != null) {
            String v = param.getChatStyle().trim();
            if (v.isEmpty()) {
                v = defaultChatStyle();
            }
            writeString(userId, K_CHAT_STYLE, v, REMARK_CHAT_STYLE);
            log.info("用户偏好更新: userId={}, chatStyle={}", userId, v);
        }
        if (param.getEnableLongMemory() != null) {
            String v = Boolean.TRUE.equals(param.getEnableLongMemory()) ? "true" : "false";
            writeString(userId, K_ENABLE_LONG_MEMORY, v, REMARK_ENABLE_LONG_MEMORY);
            log.info("用户偏好更新: userId={}, enableLongMemory={}", userId, v);
        }
        if (param.getEnableMemoryProfile() != null) {
            String v = Boolean.TRUE.equals(param.getEnableMemoryProfile()) ? "true" : "false";
            writeString(userId, K_ENABLE_MEMORY_PROFILE, v, REMARK_ENABLE_MEMORY_PROFILE);
            log.info("用户偏好更新: userId={}, enableMemoryProfile={}", userId, v);
        }
        return load(userId);
    }

    @Override
    public String defaultMemoryFilePath(Long userId) {
        // 动态拼出当前操作系统的真实路径, 不写死字面量.
        // <p>Windows: C:\Users\{user}\.jc\jc-rag\memory\\user_profile.md
        // <p>Linux/macOS: /home/{user}/.jc/jc-rag/memory/user_profile.md
        return Paths.get(System.getProperty("user.home"), ".jc", "jc-rag", "memory", "user_profile.md").toString();
    }

    @Override
    public String defaultChatStyle() {
        return DEFAULT_CHAT_STYLE;
    }

    /* ----------- 私有辅助 ----------- */

    private String readString(Long userId, String keySuffix, String defaultValue) {
        String key = userId + "." + keySuffix;
        SysConfigEntity e = sysConfigMapper.selectOne(
                new QueryWrapper<SysConfigEntity>()
                        .eq("config_group", GROUP)
                        .eq("config_key", key)
                        .last("LIMIT 1")
        );
        if (e == null || e.getConfigValue() == null || e.getConfigValue().isBlank()) {
            return defaultValue;
        }
        return e.getConfigValue();
    }

    /** 读 boolean 偏好, 缺省用 defaultValue. 解析失败按 defaultValue 处理. */
    private Boolean readBoolean(Long userId, String keySuffix, boolean defaultValue) {
        String raw = readString(userId, keySuffix, null);
        if (raw == null) return defaultValue;
        String t = raw.trim().toLowerCase();
        if (t.isEmpty()) return defaultValue;
        if ("false".equals(t) || "0".equals(t)) return Boolean.FALSE;
        if ("true".equals(t) || "1".equals(t)) return Boolean.TRUE;
        return defaultValue;
    }

    private void writeString(Long userId, String keySuffix, String value, String remark) {
        String key = userId + "." + keySuffix;
        sysConfigMapper.upsertConfig(GROUP, key, value, "STRING", 0, remark);
    }

    /* ----------- 记忆文件 (Markdown) 读写 ----------- */

    @Override
    public MemoryFileVO readMemoryFile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        String rawPath = readString(userId, K_MEMORY_FILE_PATH, defaultMemoryFilePath(userId));
        Path resolved = resolveMemoryPath(rawPath);
        Path parent = resolved.getParent();
        String content;
        boolean exists = Files.exists(resolved);
        boolean autoInitialized = false;

        try {
            if (!exists) {
                // 父目录不存在 -> mkdir -p
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                content = buildDefaultTemplate();
                // 首次自动写入默认模板, 后续可被用户编辑覆盖
                atomicWrite(resolved, content);
                autoInitialized = true;
                exists = true;
                log.info("记忆文件自动初始化: userId={}, path={}", userId, resolved);
            } else {
                content = Files.readString(resolved, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("记忆文件读取失败: userId={}, path={}", userId, resolved, e);
            throw new IllegalStateException("记忆文件读取失败: " + e.getMessage(), e);
        }

        return toMemoryFileVO(rawPath, resolved, parent, content, exists, autoInitialized);
    }

    @Override
    public MemoryFileVO writeMemoryFile(Long userId, MemoryFileSaveParam param) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (param == null || param.getContent() == null) {
            throw new IllegalArgumentException("content 不能为空 (允许空字符串)");
        }
        String rawPath = readString(userId, K_MEMORY_FILE_PATH, defaultMemoryFilePath(userId));
        Path resolved = resolveMemoryPath(rawPath);
        Path parent = resolved.getParent();
        String content = param.getContent();

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            atomicWrite(resolved, content);
            log.info("记忆文件保存: userId={}, path={}, bytes={}", userId, resolved, content.getBytes(StandardCharsets.UTF_8).length);
        } catch (IOException e) {
            log.error("记忆文件保存失败: userId={}, path={}", userId, resolved, e);
            throw new IllegalStateException("记忆文件保存失败: " + e.getMessage(), e);
        }

        return toMemoryFileVO(rawPath, resolved, parent, content, true, false);
    }

    /* ----------- 私有辅助: 记忆文件 ----------- */

    /**
     * 解析用户填的路径 (含 {@code ~}) 为真实磁盘路径.
     * <p>{@code ~} 展开为 {@code System.getProperty("user.home")}.
     * <p>平台说明:
     * <ul>
     *   <li>Linux/macOS: {@code ~} = {@code $HOME}</li>
     *   <li>Windows: {@code ~} = {@code %USERPROFILE%} ({@code C:\Users\{username}})</li>
     * </ul>
     */
    private Path resolveMemoryPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            rawPath = defaultMemoryFilePath(null);
        }
        String trimmed = rawPath.trim();
        if (trimmed.startsWith("~")) {
            String home = System.getProperty("user.home");
            trimmed = home + trimmed.substring(1);
        }
        return Paths.get(trimmed).toAbsolutePath().normalize();
    }

    /** 原子写: 写临时文件再 rename, 防止写一半挂掉导致文件损坏. */
    private void atomicWrite(Path target, String content) throws IOException {
        Path dir = target.getParent();
        Path tmp = (dir != null ? dir : Paths.get("."))
                .resolve(".user_profile.md." + UUID.randomUUID() + ".tmp");
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            // 优先原子 rename; 不支持时降级为普通 move
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignore) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            // 兜底清理 (rename 成功后 tmp 应已消失, 此处仅为异常路径)
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) { /* nop */ }
        }
    }

    /** 记忆文件默认模板 (Markdown). */
    private String buildDefaultTemplate() {
        return """
                # 个人偏好 (Personal Profile)

                > 此文件由 jc-rag 自动创建, 用于记录长期记忆与个人偏好.
                > 路径: `~/.jc/jc-rag/memory/user_profile.md`
                > 提示: 你可以直接在偏好设置中编辑, 也可以用任何 Markdown 编辑器打开本地文件编辑.

                ## 技术背景
                - (待补充)

                ## 工作偏好
                - (待补充)

                ## 长期目标
                - (待补充)

                ---
                最后更新: %s
                """.formatted(LocalDateTime.now().toLocalDate());
    }

    /** 构造统一 VO (含 lastModified / bytes 元信息). */
    private MemoryFileVO toMemoryFileVO(String rawPath, Path resolved, Path parent,
                                        String content, boolean exists, boolean autoInitialized) {
        MemoryFileVO vo = new MemoryFileVO();
        vo.setRawPath(rawPath);
        vo.setResolvedPath(resolved.toString());
        vo.setParentDir(parent != null ? parent.toString() : null);
        vo.setContent(content);
        vo.setExists(exists);
        vo.setAutoInitialized(autoInitialized);
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        vo.setBytes((long) bytes.length);
        try {
            if (exists) {
                Instant lastModified = Files.getLastModifiedTime(resolved).toInstant();
                vo.setLastModified(LocalDateTime.ofInstant(lastModified, ZoneId.systemDefault()));
            }
        } catch (IOException ignore) {
            // 元信息读取失败不影响主流程
        }
        return vo;
    }

    /* ----------- 被遗忘权 (GDPR / 个保法) 一键忘记我 ----------- */

    @Override
    @Transactional
    public Map<String, Object> forgetMe(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("executedAt", LocalDateTime.now().toString());
        int total = 0;

        // 1. 先取本用户所有 session_id (删 message 用)
        List<ChatSessionEntity> sessions = chatSessionMapper.selectList(
            new QueryWrapper<ChatSessionEntity>().eq("user_id", userId));
        List<Long> sessionIds = sessions.stream().map(ChatSessionEntity::getId).toList();
        result.put("sessionsFound", sessionIds.size());

        // 2. 物理删 chat_message (按 session_id 批量)
        int msgCount = 0;
        for (Long sid : sessionIds) {
            try {
                msgCount += chatMessageMapper.delete(
                    new QueryWrapper<ChatMessageEntity>().eq("session_id", sid));
            } catch (Exception e) {
                log.warn("forgetMe: delete chat_message failed, sessionId={}, err={}", sid, e.getMessage());
            }
        }
        result.put("chatMessageDeleted", msgCount);
        total += msgCount;

        // 3. 物理删 chat_session
        int sessCount = 0;
        try {
            sessCount = chatSessionMapper.delete(
                new QueryWrapper<ChatSessionEntity>().eq("user_id", userId));
        } catch (Exception e) {
            log.warn("forgetMe: delete chat_session failed, userId={}, err={}", userId, e.getMessage());
        }
        result.put("chatSessionDeleted", sessCount);
        total += sessCount;

        // 4. 物理删 chat_session_summary
        int sumCount = 0;
        try {
            sumCount = chatSessionSummaryMapper.delete(
                new QueryWrapper<ChatSessionSummaryEntity>().eq("user_id", userId));
        } catch (Exception e) {
            log.warn("forgetMe: delete chat_session_summary failed, userId={}, err={}", userId, e.getMessage());
        }
        result.put("chatSessionSummaryDeleted", sumCount);
        total += sumCount;

        // 5. 物理删 user_memory_profile (L3)
        int profCount = 0;
        try {
            UserMemoryProfileEntity exist = userMemoryProfileMapper.selectByUserId(userId);
            if (exist != null) {
                profCount = userMemoryProfileMapper.deleteById(exist.getId());
            }
        } catch (Exception e) {
            log.warn("forgetMe: delete user_memory_profile failed, userId={}, err={}", userId, e.getMessage());
        }
        result.put("userMemoryProfileDeleted", profCount);
        total += profCount;

        // 6. 调 Python 物理删 ES jc_chat_memory (L2)
        int esCount = 0;
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("userId", String.valueOf(userId));
            String body = objectMapper.writeValueAsString(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(ragBaseUrl + "/chat/memory/forget"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = FORGET_HTTP.send(httpReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 400) {
                @SuppressWarnings("unchecked")
                Map<String, Object> r = objectMapper.readValue(resp.body(), Map.class);
                Object removed = r.get("removed");
                esCount = removed == null ? 0 : Integer.parseInt(String.valueOf(removed));
            } else {
                log.warn("forgetMe: Python /chat/memory/forget status={}, body={}", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warn("forgetMe: ES clear failed, userId={}, err={}", userId, e.getMessage());
        }
        result.put("esMemoryDeleted", esCount);
        total += esCount;

        // 7. 物理删 user_pref sys_config (本用户全部偏好, 按 config_key 前缀 'userId.' 过滤)
        int cfgCount = 0;
        try {
            cfgCount = sysConfigMapper.delete(
                new QueryWrapper<SysConfigEntity>()
                    .eq("config_group", GROUP)
                    .likeRight("config_key", userId + "."));
        } catch (Exception e) {
            log.warn("forgetMe: delete sys_config failed, userId={}, err={}", userId, e.getMessage());
        }
        result.put("userPrefDeleted", cfgCount);
        total += cfgCount;

        // 8. 物理删记忆文件 (Markdown) - 可选, 失败不阻塞
        int fileCount = 0;
        try {
            String path = readString(userId, K_MEMORY_FILE_PATH, defaultMemoryFilePath(userId));
            Path p = resolveMemoryPath(path);
            if (Files.exists(p)) {
                Files.delete(p);
                fileCount = 1;
            }
        } catch (Exception e) {
            log.warn("forgetMe: delete memory file failed, userId={}, err={}", userId, e.getMessage());
        }
        result.put("memoryFileDeleted", fileCount);
        total += fileCount;

        result.put("totalDeleted", total);
        log.info("forgetMe done: userId={}, totalDeleted={}, detail={}", userId, total, result);
        return result;
    }
}
