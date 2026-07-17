package com.jc.rag.user.module.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.common.config.RuntimeConfigDefaults;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.security.SensitiveFilterResult;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.user.module.security.SecurityAuditService;
import com.jc.rag.user.module.security.UserSensitiveWordCache;
import com.jc.rag.user.module.user.service.UserMemoryProfileService;
import com.jc.rag.user.module.user.service.UserPreferenceService;
import com.jc.rag.user.module.chat.model.param.UserChatSessionQueryParam;
import com.jc.rag.domain.entity.chat.ChatMessageEntity;
import com.jc.rag.domain.entity.chat.ChatSessionEntity;
import com.jc.rag.domain.entity.chat.ChatSessionSummaryEntity;
import com.jc.rag.domain.entity.chat.UserApiKeyEntity;
import com.jc.rag.domain.entity.chat.UserQuotaUsageEntity;
import com.jc.rag.domain.entity.rag.RagAgentEntity;
import com.jc.rag.domain.entity.sys.SysConfigEntity;
import com.jc.rag.domain.mapper.chat.ChatMessageMapper;
import com.jc.rag.domain.mapper.chat.ChatSessionMapper;
import com.jc.rag.domain.mapper.chat.ChatSessionSummaryMapper;
import com.jc.rag.domain.mapper.chat.UserApiKeyMapper;
import com.jc.rag.domain.mapper.chat.UserQuotaUsageMapper;
import com.jc.rag.domain.mapper.rag.RagAgentMapper;
import com.jc.rag.domain.mapper.user.UserCustomAgentMapper;
import com.jc.rag.domain.mapper.sys.DemoConfigMapper;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.jc.rag.user.module.chat.model.vo.AskResponseVO;
import com.jc.rag.user.module.chat.model.vo.ChatMessageVO;
import com.jc.rag.user.module.chat.model.vo.ChatSessionVO;
import com.jc.rag.user.module.chat.model.vo.KbOptionVO;
import com.jc.rag.user.module.chat.model.vo.KbSelectConfigVO;
import com.jc.rag.user.module.chat.model.vo.ModelInfoVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * C 端问答中心 Service 实现。
 * <p>所有公共方法返回严格类型 VO, 严禁 Map 表达; 业务逻辑保持与原版一致。
 *
 * @author JC
 */
@Service
public class UserChatServiceImpl implements UserChatService {

    private static final Logger log = LoggerFactory.getLogger(UserChatService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build();

    /** RAG 服务地址走 yml (service.python.rag.base-url), 允许 env 覆盖; 旧值作为兜底. */
    @Value("${service.python.rag.base-url:http://localhost:8001}")
    private String ragBaseUrl;

    private static final ExecutorService STREAM_EXECUTOR = Executors.newCachedThreadPool();

    /** L0 短期记忆: 默认从 chat_message 读最近 N 轮 (1 轮 = 1 user + 1 assistant = 2 条消息) 喂给 LLM, 解决上下文断裂. */
    private static final int DEFAULT_HISTORY_ROUNDS = 10;
    /** L0 短期记忆: 单次请求 history 字段总字符上限 (超出则从最老开始截断, 避免 token 爆炸). */
    private static final int HISTORY_MAX_CHARS = 8000;

    /** L1 摘要压缩: 会话累计轮数达到此阈值时, 触发异步压缩最前 SUMMARY_COMPRESS_ROUNDS 轮. */
    private static final int SUMMARY_TRIGGER_ROUNDS = 20;
    /** L1 摘要压缩: 每次触发压缩的轮数 (从会话最前取这么多轮原文, 压缩为摘要入库). */
    private static final int SUMMARY_COMPRESS_ROUNDS = 10;
    /** L1 摘要压缩: 摘要生成调用超时 (秒). 失败 3 次降级为取前 5 轮直接拼接. */
    private static final int SUMMARY_LLM_TIMEOUT_SEC = 60;
    /** L1 摘要压缩: 摘要注入 system prompt 时的提示前缀. */
    private static final String SUMMARY_SYSTEM_PREFIX = "\n\n[本会话历史摘要 (L1)]\n";

    /** L2 长期事实: 注入 systemPrompt 时的提示前缀 (与 L1 配合, 在 L1 之后追加). */
    private static final String MEMORY_SYSTEM_PREFIX = "\n\n[用户长期记忆 (L2) — 来自历史会话, AI 可引用]\n";
    /** L2 长期事实: 记忆提取调用超时 (秒). 失败不阻塞主对话. */
    private static final int MEMORY_EXTRACT_TIMEOUT_SEC = 60;
    /** L2 长期事实: 记忆召回调用超时 (秒). 召回失败则跳过, 不注入. */
    private static final int MEMORY_RECALL_TIMEOUT_SEC = 10;

    /** L3 跨会话画像: 每累计 N 轮触发一次画像提取 (从 L2 fact 聚合并提炼). 值由 application.yml: jc-rag.memory.profile.trigger-rounds 注入. */
    @Value("${jc-rag.memory.profile.trigger-rounds:10}")
    private int profileTriggerRounds;
    private static final String[] VISION_KEYWORDS = {"gpt-4o", "gpt-4-turbo", "gpt-4-vision", "gpt-4v", "claude-3", "gemini", "qwen-vl", "qwen2-vl", "qwen2.5-vl", "qwen3-vl", "llava", "pixtral", "phi-3.5-vision", "glm-4v", "yi-vision", "minicpm-v", "llama3.2-vision", "llama-3.2-vision", "internvl", "cogvlm", "vision"};
    private static final String[] TOOL_KEYWORDS = {"gpt-4o", "gpt-4-turbo", "claude-3", "claude-3.5", "claude-4", "gemini", "qwen2.5", "qwen3", "llama4", "deepseek", "mistral", "function-calling", "tool-use"};
    private static final String[] THINKING_KEYWORDS = {"o1", "o3", "o4", "deepseek-r1", "qwq", "qwen3", "gemini-2.5", "claude-3.5", "claude-4", "think"};

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatSessionSummaryMapper summaryMapper;
    private final UserApiKeyMapper apiKeyMapper;
    private final UserQuotaUsageMapper quotaMapper;
    private final SysConfigMapper configMapper;
    private final RagAgentMapper agentMapper;
    private final DemoConfigMapper demoConfigMapper;
    private final com.jc.rag.domain.mapper.knowledge.KbKnowledgeBaseMapper kbMapper;
    private final ObjectMapper objectMapper;
    private final UserMemoryProfileService userMemoryProfileService;
    /** 敏感词本地缓存 (user 模块, 拉取 admin 端词库) */
    private final UserSensitiveWordCache sensitiveCache;
    /** 安全审计日志 */
    private final SecurityAuditService auditService;
    /** 用户自定义智能体 mapper */
    private final UserCustomAgentMapper userAgentMapper;
    /**
     * 自注入: 让内部调用 saveSummary 走 Spring 代理, @Transactional 才能生效
     * (避免 STREAM_EXECUTOR 线程内 this.saveSummary() 事务失效).
     */
    private final UserChatServiceImpl self;

    public UserChatServiceImpl(ChatSessionMapper s, ChatMessageMapper m, ChatSessionSummaryMapper sm,
                               UserApiKeyMapper a,
                               UserQuotaUsageMapper q, SysConfigMapper c, RagAgentMapper am,
                               DemoConfigMapper dc, com.jc.rag.domain.mapper.knowledge.KbKnowledgeBaseMapper kb,
                               ObjectMapper om,
                               UserMemoryProfileService ump,
                               UserSensitiveWordCache sensitiveCache,
                               SecurityAuditService auditService,
                               UserCustomAgentMapper userAgentMapper,
                               @org.springframework.context.annotation.Lazy UserChatServiceImpl self) {
        this.sessionMapper = s;
        this.messageMapper = m;
        this.summaryMapper = sm;
        this.apiKeyMapper = a;
        this.quotaMapper = q;
        this.configMapper = c;
        this.agentMapper = am;
        this.demoConfigMapper = dc;
        this.kbMapper = kb;
        this.objectMapper = om;
        this.userMemoryProfileService = ump;
        this.sensitiveCache = sensitiveCache;
        this.auditService = auditService;
        this.userAgentMapper = userAgentMapper;
        this.self = self;
    }

    private void verifyOwnership(Long userId, Long sessionId) {
        ChatSessionEntity s = sessionMapper.selectById(sessionId);
        if (s == null) throw new RuntimeException("会话不存在");
        if (!s.getUserId().equals(userId)) throw new RuntimeException("无权访问此会话");
    }

    private void checkDailyQuota(Long userId) {
        boolean hasKey = apiKeyMapper.selectCount(
            new QueryWrapper<UserApiKeyEntity>().eq("user_id", userId).eq("is_active", 1)) > 0;
        if (hasKey) return;
        int dailyLimit = Integer.MAX_VALUE;
        boolean demoEnabled = false;
        try {
            Map<String, Object> dc = demoConfigMapper.selectOne().stream().findFirst().orElse(Map.of());
            demoEnabled = ((Number) dc.getOrDefault("demo_mode_enabled", 0)).intValue() == 1;
            dailyLimit = ((Number) dc.getOrDefault("default_quota_per_day", 5)).intValue();
        } catch (Exception ignored) {}
        if (!demoEnabled) return;
        UserQuotaUsageEntity todayUsage = quotaMapper.selectOne(
            new QueryWrapper<UserQuotaUsageEntity>().eq("user_id", userId).eq("usage_date", LocalDate.now())
        );
        int used = todayUsage != null ? todayUsage.getQueryCount() : 0;
        if (used >= dailyLimit) {
            throw new RuntimeException("提问今日次数已达限制（限额 " + dailyLimit + " 次），请配置私有 API Keys 继续使用，或明日再试");
        }
    }

    private void recordUsage(Long userId) {
        LocalDate today = LocalDate.now();
        UserQuotaUsageEntity uq = quotaMapper.selectOne(
            new QueryWrapper<UserQuotaUsageEntity>().eq("user_id", userId).eq("usage_date", today));
        if (uq != null) {
            uq.setQueryCount(uq.getQueryCount() + 1);
            quotaMapper.updateById(uq);
        } else {
            uq = new UserQuotaUsageEntity();
            uq.setUserId(userId);
            uq.setUsageDate(today);
            uq.setQueryCount(1);
            uq.setTokensUsed(0);
            quotaMapper.insert(uq);
        }
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        return listSessions(userId, UserChatSessionQueryParam.all()).getRecords();
    }

    @Override
    public PageResponse<ChatSessionVO> listSessions(Long userId, UserChatSessionQueryParam query) {
        String like = PageHelper.buildLikeClause(query.getKeyword());
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = sessionMapper.pageSessions(page, userId, like);
        page.setTotal(sessionMapper.countSessions(userId, like));
        return PageHelper.toPageResponse(page, this::toChatSessionVO);
    }

    /** map -> ChatSessionVO (会话列表行). */
    private ChatSessionVO toChatSessionVO(Map<String, Object> m) {
        ChatSessionVO v = new ChatSessionVO();
        v.setId(asLong(m.get("id")));
        v.setSessionTitle((String) m.get("sessionTitle"));
        v.setKbId(asLong(m.get("kbId")));
        v.setKbName((String) m.get("kbName"));
        v.setIsPinned(asBoolean(m.get("isPinned")));
        v.setCreatedAt(asDateTime(m.get("createdAt")));
        v.setUpdatedAt(asDateTime(m.get("updatedAt")));
        return v;
    }

    @Override
    public ChatSessionVO getSession(Long id) {
        Map<String, Object> m = sessionMapper.selectDetailById(id);
        if (m == null) return null;
        ChatSessionVO v = toChatSessionVO(m);
        v.setUserId(asLong(m.get("userId")));
        v.setSessionMode((String) m.get("sessionMode"));
        v.setModelMode((String) m.get("modelMode"));
        v.setModelName((String) m.get("modelName"));
        v.setStatus(asInt(m.get("status")));
        return v;
    }

    @Override
    @Transactional
    public ChatSessionVO createSession(Long userId, String title, Long kbId) {
        // 按 KB 选择 mode 校验
        KbSelectConfigVO cfg = getKbSelectConfig();
        Long resolvedKbId;
        if ("FIXED".equals(cfg.getMode())) {
            resolvedKbId = cfg.getDefaultKb() == null ? null : cfg.getDefaultKb().getId();
        } else if ("SELECTABLE".equals(cfg.getMode())) {
            // 用户可传, 但必须命中可选项; 不传则取列表第一个
            if (kbId == null) {
                if (cfg.getOptions() == null || cfg.getOptions().isEmpty()) {
                    throw new ResourceNotFoundException("kb.none.available");
                }
                resolvedKbId = cfg.getOptions().get(0).getId();
            } else {
                boolean hit = cfg.getOptions() != null && cfg.getOptions().stream()
                        .anyMatch(o -> kbId.equals(o.getId()));
                if (!hit) {
                    throw new ResourceNotFoundException("kb.not.selectable", kbId);
                }
                resolvedKbId = kbId;
            }
        } else {
            throw new ResourceNotFoundException("kb.none.available");
        }
        if (resolvedKbId == null) {
            throw new ResourceNotFoundException("kb.none.available");
        }
        Long kbCount = kbMapper.selectCount(new QueryWrapper<com.jc.rag.domain.entity.knowledge.KbKnowledgeBaseEntity>().eq("id", resolvedKbId).eq("status", 1));
        if (kbCount == null || kbCount <= 0) {
            throw new ResourceNotFoundException("kb.not.found", resolvedKbId);
        }
        String resolvedTitle = title == null || title.isBlank() ? "新会话" : title.trim();
        ChatSessionEntity s = new ChatSessionEntity();
        s.setUserId(userId);
        s.setSessionTitle(resolvedTitle);
        s.setSessionMode("RAG");
        s.setKbId(resolvedKbId);
        s.setIsPinned(0);
        s.setStatus(1);
        sessionMapper.insert(s);
        return getSession(s.getId());
    }

    @Override
    @Transactional
    public ChatSessionVO renameSession(Long userId, Long sessionId, String sessionTitle) {
        verifyOwnership(userId, sessionId);
        String title = trimSessionTitle(sessionTitle);
        sessionMapper.updateTitle(sessionId, title);
        return getSession(sessionId);
    }

    @Override
    @Transactional
    public ChatSessionVO pinSession(Long userId, Long sessionId, boolean pinned) {
        verifyOwnership(userId, sessionId);
        sessionMapper.updatePinned(sessionId, pinned ? 1 : 0);
        return getSession(sessionId);
    }

    @Override
    public List<ChatMessageVO> listMessages(Long userId, Long sessionId) {
        verifyOwnership(userId, sessionId);
        List<ChatMessageEntity> msgs = messageMapper.selectList(
            new QueryWrapper<ChatMessageEntity>().eq("session_id", sessionId).orderByAsc("message_index"));
        List<ChatMessageVO> result = new ArrayList<>();
        for (ChatMessageEntity m : msgs) {
            ChatMessageVO v = new ChatMessageVO();
            v.setId(m.getId());
            v.setRoleType(m.getRoleType());
            v.setMessageContent(m.getMessageContent());
            v.setMessageIndex(m.getMessageIndex());
            v.setTokenCount(m.getTokenCount());
            v.setLatencyMs(m.getLatencyMs());
            v.setCreatedAt(m.getCreatedAt());
            if (m.getAttachments() != null && !m.getAttachments().isBlank()) {
                try {
                    v.setAttachments(objectMapper.readValue(m.getAttachments(), new TypeReference<List<Map<String, Object>>>() {}));
                } catch (Exception e) {
                    /* ignore */
                }
            }
            result.add(v);
        }
        return result;
    }

    @Override
    @Transactional
    public AskResponseVO ask(Long userId, Long sessionId, String question, Long agentId, Long customAgentId) {
        verifyOwnership(userId, sessionId);
        enforceInputSecurity(question, userId, sessionId);
        Long resolvedAgentId = resolveActiveAgentId(agentId, customAgentId);
        checkDailyQuota(userId);
        int nextIdx = Optional.ofNullable(messageMapper.selectCount(new QueryWrapper<ChatMessageEntity>().eq("session_id", sessionId))).orElse(0L).intValue() + 1;
        boolean shouldGenerateTitle = shouldGenerateTitle(sessionId, nextIdx);
        insertMsg(sessionId, "USER", question, nextIdx, 0, 0);
        if (shouldGenerateTitle) {
            generateSessionTitleAsync(userId, sessionId, question);
        }
        long started = System.currentTimeMillis();
        String answer;
        try {
            answer = callPythonAsk(userId, sessionId, question, resolvedAgentId, customAgentId);
        } catch (com.jc.rag.common.exception.BizException be) {
            // 业务异常 (如 AGENT_NOT_AVAILABLE) 直接抛出, 让 GlobalExceptionHandler 返回给前端
            throw be;
        } catch (Exception e) {
            log.warn("ask failed: {}", e.getMessage());
            answer = "提问失败，请确认 Python RAG 服务和模型服务是否正常运行。";
        }
        long latency = System.currentTimeMillis() - started;
        insertMsg(sessionId, "ASSISTANT", answer, nextIdx + 1, 0, latency);
        touchSession(sessionId);
        recordUsage(userId);
        // L1 摘要压缩: 累计轮数达到阈值时, 异步压缩最前 N 轮原文
        maybeTriggerSummary(userId, sessionId, nextIdx + 1);
        // L2 长期事实: 每轮都异步提取 fact 入库 (用户关闭时跳过)
        maybeExtractMemory(userId, sessionId, nextIdx + 1);
        // L3 跨会话画像: 每累计 profileTriggerRounds 轮触发一次画像提取 (从 L2 fact 聚合)
        maybeExtractProfile(userId, nextIdx + 1);
        AskResponseVO v = new AskResponseVO();
        v.setAnswer(answer);
        return v;
    }

    @Override
    public SseEmitter askStream(Long userId, Long sessionId, String question, boolean deepThinking,
                                Long agentId, Long customAgentId, Long overrideKbId, List<Map<String, Object>> attachments) {
        verifyOwnership(userId, sessionId);
        SseEmitter emitter = new SseEmitter(600_000L);
        int nextIdx = Optional.ofNullable(messageMapper.selectCount(new QueryWrapper<ChatMessageEntity>().eq("session_id", sessionId))).orElse(0L).intValue() + 1;
        boolean shouldGenerateTitle = shouldGenerateTitle(sessionId, nextIdx);
        // 序列化附件（含 base64 以支持刷新后展示缩略图）
        String attachmentsJson = null;
        if (attachments != null && !attachments.isEmpty()) {
            try {
                attachmentsJson = objectMapper.writeValueAsString(attachments);
            } catch (Exception e) {
                /* ignore */
            }
        }
        Long userMessageId = insertMsg(sessionId, "USER", question, nextIdx, 0, 0, attachmentsJson);
        if (shouldGenerateTitle) {
            generateSessionTitleAsync(userId, sessionId, question);
        }
        // 预校验: agent 是否可用 (主线程抛 BizException, 走 GlobalExceptionHandler 返回前端 SSE error event)
        // 如果放到 STREAM_EXECUTOR 里, 异常会被 executor 吞掉
        try {
            Long resolvedAgentId = resolveActiveAgentId(agentId, customAgentId);
            ensureAgentAvailable(userId, resolvedAgentId, customAgentId);
        } catch (BizException be) {
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("type", "error", "error", be.getMessage(), "code", be.getCode())));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }
        try {
            emitter.send(SseEmitter.event().name("meta").data(Map.of("type", "meta", "userMessageId", userMessageId)));
        } catch (Exception ignored) {}
        emitter.onCompletion(() -> log.info("SSE completed for session {}", sessionId));
        emitter.onTimeout(() -> log.warn("SSE timeout for session {}", sessionId));
        emitter.onError(e -> log.warn("SSE error for session {}: {}", sessionId, e.getMessage()));
        STREAM_EXECUTOR.execute(() -> {
            StringBuilder fullAnswer = new StringBuilder();
            boolean ok = false;
            boolean doneForwarded = false;
            long started = System.currentTimeMillis();
            try {
                checkDailyQuota(userId);
                enforceInputSecurity(question, userId, sessionId);
                Long resolvedAgentId = resolveActiveAgentId(agentId, customAgentId);
                Map<String, Object> payload = buildPayload(userId, sessionId, question, deepThinking, resolvedAgentId, customAgentId, overrideKbId, attachments);
                // 兜底注入: 即使 buildPayload 因任何原因没注入 systemPrompt/userPromptSuffix, 此处再补一次, 100% 生效
                ensureAgentPromptInjected(payload, resolvedAgentId, customAgentId, userId);
                String body = objectMapper.writeValueAsString(payload);
                // [DIAG] 打印最终发到 Python 的完整 payload, 确认 systemPrompt / userPromptSuffix 真的传过去了
                log.info("askStream: [FINAL_PAYLOAD] {}", body);
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(ragBaseUrl + "/chat/ask/stream"))
                    .timeout(Duration.ofSeconds(300)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
                HttpResponse<java.io.InputStream> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() >= 400) {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("type", "error", "error", "模型服务暂时不可用")));
                } else {
                    ok = true;
                    BufferedReader br = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!line.startsWith("data:")) continue;
                        String raw = line.substring(5).trim();
                        if (raw.isEmpty()) continue;
                        try {
                            Map<String, Object> data = objectMapper.readValue(raw, new TypeReference<>() {});
                            String type = String.valueOf(data.getOrDefault("type", ""));
                            if ("done".equals(type) || Boolean.TRUE.equals(data.get("done"))) {
                                Map<String, Object> doneData = new LinkedHashMap<>(data);
                                doneData.put("llmUsed", true);
                                emitter.send(SseEmitter.event().name("done").data(doneData));
                                doneForwarded = true;
                                break;
                            }
                            if ("meta".equals(type)) {
                                emitter.send(SseEmitter.event().name("meta").data(data));
                            } else if ("delta".equals(type)) {
                                String delta = String.valueOf(data.getOrDefault("delta", ""));
                                if (!delta.isEmpty()) {
                                    emitter.send(SseEmitter.event().name("delta").data(Map.of("type", "delta", "delta", delta)));
                                    fullAnswer.append(delta);
                                }
                            } else if ("thinking".equals(type)) {
                                emitter.send(SseEmitter.event().name("thinking").data(data));
                            } else if ("error".equals(type)) {
                                emitter.send(SseEmitter.event().name("error").data(data));
                            }
                        } catch (Exception e) {
                            log.warn("SSE parse event failed for session {}: {}", sessionId, e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("SSE stream error for session {}: {}", sessionId, e.getMessage());
                try {
                    String errMsg = (e.getMessage() != null && e.getMessage().contains("已达限制")) ? e.getMessage() : "连接异常";
                    emitter.send(SseEmitter.event().name("error").data(Map.of("type", "error", "error", errMsg)));
                } catch (Exception ignore) {}
            }
            if (!fullAnswer.isEmpty()) {
                insertMsg(sessionId, "ASSISTANT", fullAnswer.toString(), nextIdx + 1, 0, System.currentTimeMillis() - started);
            }
            touchSession(sessionId);
            if (ok) recordUsage(userId);
            // L1 摘要压缩: 累计轮数达到阈值时, 异步压缩最前 N 轮原文 (失败/无答案也尝试, 避免漏触发)
            maybeTriggerSummary(userId, sessionId, nextIdx + 1);
            // L2 长期事实: 每轮都异步提取 fact 入库 (用户关闭时跳过)
            maybeExtractMemory(userId, sessionId, nextIdx + 1);
            // L3 跨会话画像: 每累计 profileTriggerRounds 轮触发一次画像提取 (从 L2 fact 聚合)
            maybeExtractProfile(userId, nextIdx + 1);
            try {
                if (!doneForwarded) {
                    emitter.send(SseEmitter.event().name("done").data(Map.of("type", "done", "llmUsed", ok)));
                }
                emitter.complete();
            } catch (Exception ignore) {
                try { emitter.completeWithError(ignore); } catch (Exception ignored) {}
            }
        });
        return emitter;
    }

    @Override
    @Transactional
    public BaseOpResultVO deleteSession(Long userId, Long sessionId) {
        verifyOwnership(userId, sessionId);
        ChatSessionEntity s = new ChatSessionEntity();
        s.setId(sessionId);
        s.setStatus(0);
        sessionMapper.updateById(s);
        return BaseOpResultVO.success(sessionId, "会话已删除");
    }

    @Override
    public List<KbOptionVO> listDefaultKnowledgeBases() {
        Long defaultKbId = resolveDefaultKbId();
        List<Map<String, Object>> rows = kbMapper.listActiveWithDefault(defaultKbId);
        List<KbOptionVO> result = new ArrayList<>();
        for (Map<String, Object> m : rows) {
            KbOptionVO v = new KbOptionVO();
            v.setId(asLong(m.get("id")));
            v.setKbCode((String) m.get("kbCode"));
            v.setKbName((String) m.get("kbName"));
            v.setKbDesc((String) m.get("kbDesc"));
            v.setIsDefault(asInt(m.get("isDefault")));
            result.add(v);
        }
        return result;
    }

    @Override
    public KbSelectConfigVO getKbSelectConfig() {
        KbSelectConfigVO cfg = new KbSelectConfigVO();
        // 1. 优先: 系统默认 KB (chat.default.kbId)
        Long defaultKbId = resolveDefaultKbId();
        if (defaultKbId != null) {
            KbOptionVO opt = loadKbOption(defaultKbId, true);
            if (opt != null) {
                cfg.setMode("FIXED");
                cfg.setDefaultKb(opt);
                cfg.setOptions(List.of(opt));
                return cfg;
            }
        }
        // 2. 次选: 公开且 client_selectable=1 的 KB
        List<KbOptionVO> selectable = new ArrayList<>();
        for (com.jc.rag.domain.entity.knowledge.KbKnowledgeBaseEntity kb : kbMapper.listPublicSelectable()) {
            KbOptionVO v = new KbOptionVO();
            v.setId(kb.getId());
            v.setKbCode(kb.getKbCode());
            v.setKbName(kb.getKbName());
            v.setKbDesc(kb.getKbDesc());
            v.setIsDefault(0);
            selectable.add(v);
        }
        if (!selectable.isEmpty()) {
            cfg.setMode("SELECTABLE");
            cfg.setDefaultKb(null);
            cfg.setOptions(selectable);
            return cfg;
        }
        // 3. 无任何可用 KB
        cfg.setMode("EMPTY");
        cfg.setDefaultKb(null);
        cfg.setOptions(List.of());
        return cfg;
    }

    /** 加载单条 KB 选项; 不存在或禁用返回 null */
    private KbOptionVO loadKbOption(Long kbId, boolean asDefault) {
        try {
            com.jc.rag.domain.entity.knowledge.KbKnowledgeBaseEntity kb = kbMapper.selectActiveById(kbId);
            if (kb == null) return null;
            KbOptionVO v = new KbOptionVO();
            v.setId(kb.getId());
            v.setKbCode(kb.getKbCode());
            v.setKbName(kb.getKbName());
            v.setKbDesc(kb.getKbDesc());
            v.setIsDefault(asDefault ? 1 : 0);
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ModelInfoVO getModelInfo(Long userId) {
        Map<String, Object> providerOverride = buildProviderOverride(userId);
        String activeMode = String.valueOf(providerOverride.getOrDefault("activeMode", "offline"));
        String modelName = "";
        List<String> capabilities = List.of();
        if ("online".equals(activeMode)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> online = (Map<String, Object>) providerOverride.get("online");
            if (online != null) {
                modelName = String.valueOf(online.getOrDefault("modelName", ""));
            }
            capabilities = detectOnlineCapabilities(modelName);
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> offline = (Map<String, Object>) providerOverride.get("offline");
            String ollamaBaseUrl = "";
            if (offline != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> ollama = (Map<String, Object>) offline.get("ollama");
                if (ollama != null) {
                    modelName = String.valueOf(ollama.getOrDefault("modelName", ""));
                    ollamaBaseUrl = String.valueOf(ollama.getOrDefault("baseUrl", ""));
                }
            }
            capabilities = fetchOllamaCapabilities(ollamaBaseUrl, modelName);
        }
        ModelInfoVO v = new ModelInfoVO();
        v.setActiveMode(activeMode);
        v.setModelName(modelName);
        v.setCapabilities(capabilities);
        v.setSupportsVision(capabilities.contains("vision"));
        return v;
    }

    /* ===================== 私有工具 ===================== */

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }

    private Boolean asBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() == 1;
        return Boolean.valueOf(o.toString());
    }

    @SuppressWarnings("unchecked")
    private java.time.LocalDateTime asDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime();
        if (o instanceof java.time.LocalDateTime ldt) return ldt;
        return null;
    }

    private Long insertMsg(Long sessionId, String role, String content, int idx, int tokens, long latency, String attachmentsJson) {
        ChatMessageEntity m = new ChatMessageEntity();
        m.setSessionId(sessionId);
        m.setRoleType(role);
        m.setMessageContent(content);
        m.setMessageIndex(idx);
        m.setTokenCount(tokens);
        m.setLatencyMs(latency);
        m.setAttachments(attachmentsJson);
        messageMapper.insert(m);
        return m.getId();
    }

    private Long insertMsg(Long sessionId, String role, String content, int idx, int tokens, long latency) {
        return insertMsg(sessionId, role, content, idx, tokens, latency, null);
    }

    private void touchSession(Long sessionId) {
        sessionMapper.touch(sessionId);
    }

    private String callPythonAsk(Long userId, Long sessionId, String question, Long agentId, Long customAgentId) throws Exception {
        Map<String, Object> payload = buildPayload(userId, sessionId, question, false, agentId, customAgentId, null, null);
        ensureAgentPromptInjected(payload, agentId, customAgentId, userId);
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(ragBaseUrl + "/chat/ask"))
            .timeout(Duration.ofSeconds(180)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) return "模型服务暂时不可用，请稍后重试。";
        return String.valueOf(objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {}).getOrDefault("answer", ""));
    }

    private void generateSessionTitleAsync(Long userId, Long sessionId, String question) {
        STREAM_EXECUTOR.execute(() -> {
            String title = fallbackSessionTitle(question);
            try {
                title = callPythonGenerateSessionTitle(userId, question);
            } catch (Exception e) {
                log.warn("generate session title failed: sessionId={}, message={}", sessionId, e.getMessage());
            }
            sessionMapper.updateTitleIfBlank(sessionId, trimSessionTitle(title));
        });
    }

    private String callPythonGenerateSessionTitle(Long userId, String question) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("question", question);
        payload.put("providerOverride", buildProviderOverride(userId));
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(ragBaseUrl + "/chat/title"))
            .timeout(Duration.ofSeconds(60)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            log.error("title service failed, status={}, body={}", resp.statusCode(), resp.body());
            throw new ResourceNotFoundException("chat.title.service.unavailable");
        }
        Map<String, Object> response = objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        return trimSessionTitle(String.valueOf(response.getOrDefault("title", "")));
    }

    private Map<String, Object> buildPayload(Long userId, Long sessionId, String question, boolean deepThinking,
                                             Long agentId, Long customAgentId, Long overrideKbId,
                                             List<Map<String, Object>> attachments) {
        Map<String, Object> p = new LinkedHashMap<>();
        ChatSessionEntity s = sessionMapper.selectById(sessionId);
        Long resolvedKbId = resolveEffectiveKbId(s, overrideKbId);
        p.put("kbId", resolvedKbId);
        log.info("buildPayload: resolvedKbId={}, overrideKbId={}, sessionKbId={}", resolvedKbId, overrideKbId, s != null ? s.getKbId() : null);
        p.put("question", question);
        p.put("topK", 5);
        p.put("topN", 20);
        p.put("similarityThreshold", 0.6);
        p.put("ragOverride", buildRagOverride());
        // L0 短期记忆: 读 chat_message 拼装 history 传给 Python
        p.put("history", buildShortTermHistory(sessionId, DEFAULT_HISTORY_ROUNDS));
        Map<String, Object> providerOverride = buildProviderOverride(userId);
        if (agentId != null) {
            p.put("agentId", agentId);
            try {
                RagAgentEntity agent = agentMapper.selectById(agentId);
                log.info("buildPayload: agent lookup result - agentId={}, agentFound={}", agentId, agent != null);
                if (agent == null) {
                    log.warn("buildPayload: agent not found by id {}", agentId);
                } else {
                    boolean isSystem = Integer.valueOf(1).equals(agent.getIsSystem());
                    log.info("buildPayload: agent fields - isSystem={}, status={}, temperature={}, maxTokens={}",
                        agent.getIsSystem(), agent.getStatus(), agent.getTemperature(), agent.getMaxTokens());
                    if (!Integer.valueOf(1).equals(agent.getStatus())) {
                        log.warn("buildPayload: agent status is not 1 (published), agentId={}, status={}, skip injection", agentId, agent.getStatus());
                        // 已下架/草稿: 不注入 systemPrompt / userPromptSuffix, 视为未选
                    } else {
                        if (isSystem) {
                            // 系统智能体 (isSystem=1): systemPrompt 走 system message
                            if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isBlank()) {
                                p.put("systemPrompt", agent.getSystemPrompt());
                                p.put("agentSystemPromptInjected", true);
                                log.info("buildPayload: [system agent] systemPrompt added, len={}", agent.getSystemPrompt().length());
                            } else {
                                log.warn("buildPayload: [system agent] systemPrompt is null or blank");
                            }
                        } else {
                            // 内建用户智能体 (isSystem=0): content 走 user message 增强片段
                            if (agent.getContent() != null && !agent.getContent().isBlank()) {
                                p.put("userPromptSuffix", agent.getContent());
                                log.info("buildPayload: [built-in user agent] content added as userPromptSuffix, len={}", agent.getContent().length());
                            } else {
                                log.warn("buildPayload: [built-in user agent] content is null or blank");
                            }
                        }
                        if (agent.getTemperature() != null || agent.getMaxTokens() != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> defaults = (Map<String, Object>) providerOverride.computeIfAbsent("defaults", key -> new LinkedHashMap<String, Object>());
                            if (agent.getTemperature() != null) defaults.put("temperature", agent.getTemperature().doubleValue());
                            if (agent.getMaxTokens() != null) defaults.put("maxTokens", agent.getMaxTokens());
                        }
                        log.info("buildPayload: agent fully loaded, agentId={}, agentKey={}, isSystem={}", agentId, agent.getAgentKey(), agent.getIsSystem());
                    }
                }
            } catch (Exception ex) {
                log.error("buildPayload: failed to load agent {}, error: {}", agentId, ex.getMessage(), ex, ex);
            }
        } else {
            log.info("buildPayload: no agentId provided");
        }
        // userPromptSuffix 3 级优先级:
        //   1) 用户选了自建智能体 (customAgentId): 用自建 content
        //   2) 用户选了内建用户智能体 (resolvedAgentId, is_system=0): 用内建 content
        //   3) 用户没选, 自动用系统里第一个已发布内建用户智能体 (id ASC, status=1)
        //   4) 系统里没有任何已发布内建用户智能体: 不注入, 上层负责提示
        if (p.get("userPromptSuffix") == null) {
            if (customAgentId != null) {
                try {
                    com.jc.rag.domain.entity.user.UserCustomAgentEntity cu = userAgentMapper.selectById(customAgentId);
                    if (cu != null && cu.getUserId() != null && cu.getUserId().equals(userId) && Integer.valueOf(1).equals(cu.getStatus())) {
                        String suffix = cu.getContent();
                        if (suffix != null && !suffix.isBlank()) {
                            p.put("userPromptSuffix", suffix);
                            log.info("buildPayload: [P1 custom] userPromptSuffix from custom agent {}, len={}", customAgentId, suffix.length());
                        }
                    } else {
                        log.warn("buildPayload: custom agent {} not found / not owned / not enabled", customAgentId);
                    }
                } catch (Exception ex) {
                    log.warn("buildPayload: load custom agent {} failed: {}", customAgentId, ex.getMessage());
                }
            } else if (agentId != null) {
                // agentId 来自 askStream 入口的 resolveActiveAgentId (null 时已走 P3 兜底)
                RagAgentEntity resolvedAgent = agentMapper.selectById(agentId);
                if (resolvedAgent != null && Integer.valueOf(0).equals(resolvedAgent.getIsSystem())
                    && Integer.valueOf(1).equals(resolvedAgent.getStatus())
                    && resolvedAgent.getContent() != null && !resolvedAgent.getContent().isBlank()) {
                    p.put("userPromptSuffix", resolvedAgent.getContent());
                    log.info("buildPayload: [P2 built-in] userPromptSuffix from built-in agent {}, len={}", agentId, resolvedAgent.getContent().length());
                }
            } else {
                // P3 兜底: 系统里第一个已发布内建用户智能体
                try {
                    List<RagAgentEntity> builtinList = agentMapper.selectList(
                        new QueryWrapper<RagAgentEntity>()
                            .eq("is_system", 0)
                            .eq("status", 1)
                            .orderByAsc("id")
                            .last("LIMIT 1"));
                    if (!builtinList.isEmpty()) {
                        RagAgentEntity fallback = builtinList.get(0);
                        if (fallback.getContent() != null && !fallback.getContent().isBlank()) {
                            p.put("userPromptSuffix", fallback.getContent());
                            log.info("buildPayload: [P3 fallback] userPromptSuffix from first built-in agent id={}, len={}",
                                fallback.getId(), fallback.getContent().length());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("buildPayload: P3 fallback query failed: {}", ex.getMessage());
                }
            }
        }
        // P4 兜底: 如果 userPromptSuffix 仍为空 且 用户没选 system agent,
        // 查任意一个内建智能体 (不做 status/is_system 限制) 都没结果 → 系统未配置, 抛错
        if (p.get("userPromptSuffix") == null) {
            try {
                Long anyBuiltin = agentMapper.selectCount(
                    new QueryWrapper<RagAgentEntity>().eq("is_system", 0));
                if (anyBuiltin == null || anyBuiltin == 0) {
                    log.warn("buildPayload: no built-in user agent configured in system, reject user={}", userId);
                    throw new BizException(ResultCode.AGENT_NOT_AVAILABLE, "管理员尚未配置用户智能体, 请联系管理员");
                }
            } catch (BizException be) {
                throw be;
            } catch (Exception ex) {
                log.warn("buildPayload: P4 no-agent check failed: {}", ex.getMessage());
            }
        }
        p.put("providerOverride", providerOverride);
        p.put("deepThinking", deepThinking);
        // 关闭深度思考时, 透传 chat_template_kwargs 让模型真的不输出 <think> (Qwen3 等支持 enable_thinking 的模型生效)
        // DeepSeek-R1 等不识别的模型会忽略此参数, 此时前端会隐藏 <think> 内容 (兜底)
        if (!deepThinking) {
            Map<String, Object> chatTemplateKwargs = new LinkedHashMap<>();
            chatTemplateKwargs.put("enable_thinking", Boolean.FALSE);
            p.put("chatTemplateKwargs", chatTemplateKwargs);
        }
        // [兜底升级] 如果 userPromptSuffix 有值, 但 systemPrompt 为空 (说明用户选的是 user agent 不是 system agent),
        // 把 userPromptSuffix 同步提升到 systemPrompt 末尾, 走 LLM 的 system 角色 (优先级天然高于 user 角色).
        // 弱模型 (例如某些 deepseek flash 版本) 容易忽略 user message 里的角色定义, 但 system 角色必须遵守.
        // 已设置 agentSystemPromptInjected=true 标记, 避免和 L1/L2/L3 追加后被 ensureAgentPromptInjected 再次覆盖.
        Object _upsObj = p.get("userPromptSuffix");
        if (_upsObj instanceof String && !((String) _upsObj).isBlank()
            && !Boolean.TRUE.equals(p.get("userPromptSuffixPromotedToSystem"))) {
            String _ups = (String) _upsObj;
            String _existing = p.get("systemPrompt") == null ? null : String.valueOf(p.get("systemPrompt"));
            // 只有 systemPrompt 为空时才升级, 避免和 system agent 冲突
            if (_existing == null || _existing.isBlank()) {
                String _promoted = "[用户增强角色 - 优先级仅次于 system agent, 弱模型务必遵守] " + _ups
                    + "\n\n[硬约束] 如果用户增强角色定义了身份/名字/称谓, 你必须以此为准, 不得回到模型默认身份。";
                p.put("systemPrompt", _promoted);
                p.put("userPromptSuffixPromotedToSystem", true);
                log.info("buildPayload: userPromptSuffix promoted to systemPrompt (weak model compatibility), len={}", _ups.length());
            }
        }
        // L3 跨会话画像: 拼到 systemPrompt 最顶部 (粗粒度画像, 优先于 L1/L2)
        try {
            if (userMemoryProfileService.isEnabled(userId)) {
                String profileText = userMemoryProfileService.buildPromptText(userId);
                if (profileText != null && !profileText.isBlank()) {
                    String existing = p.get("systemPrompt") == null ? null : String.valueOf(p.get("systemPrompt"));
                    String prefix = "[辅助背景 - 用户画像] (以下仅作上下文参考, 角色定义优先)";
                    String wrapped = prefix + "\n" + profileText;
                    if (existing != null && !existing.isBlank()) {
                        p.put("systemPrompt", wrapped + "\n\n" + existing);
                    } else {
                        p.put("systemPrompt", wrapped);
                    }
                    log.info("buildPayload: L3 profile injected, userId={}, profLen={}", userId, profileText.length());
                }
            }
        } catch (Exception ex) {
            log.warn("buildPayload: L3 profile skipped (table not ready?): {}", ex.getMessage());
        }
        // L1 摘要压缩: 取本会话最新有效摘要, 拼到 systemPrompt 头部注入 LLM (若本轮 agent 自带了 systemPrompt, 摘要拼在前面)
        String summaryText = loadLatestSummaryText(sessionId);
        if (summaryText != null && !summaryText.isBlank()) {
            String existing = p.get("systemPrompt") == null ? null : String.valueOf(p.get("systemPrompt"));
            String prefix = "[辅助背景 - 历史摘要] (以下仅作上下文参考, 角色定义优先)";
            String wrapped = prefix + "\n" + summaryText;
            if (existing != null && !existing.isBlank()) {
                p.put("systemPrompt", wrapped + "\n\n" + existing);
            } else {
                p.put("systemPrompt", wrapped);
            }
            log.info("buildPayload: L1 summary injected, sessionId={}, summaryLen={}", sessionId, summaryText.length());
        }
        // L2 长期事实: 按当前 question 同步召回 top 5 fact, 追加到 systemPrompt (L1 之后, agent 自带 systemPrompt 之前)
        // 用户可在偏好设置中关闭 (UserPreferenceService.getEnableLongMemory)
        if (isLongMemoryEnabled(userId)) {
            String memoryText = recallLongMemory(userId, question);
            if (memoryText != null && !memoryText.isBlank()) {
                String existing = p.get("systemPrompt") == null ? null : String.valueOf(p.get("systemPrompt"));
                String prefix = "[辅助背景 - 长期事实] (以下仅作上下文参考, 角色定义优先)";
                String wrapped = prefix + "\n" + memoryText;
                if (existing != null && !existing.isBlank()) {
                    p.put("systemPrompt", wrapped + "\n\n" + existing);
                } else {
                    p.put("systemPrompt", wrapped);
                }
                log.info("buildPayload: L2 memory injected, userId={}, memLen={}", userId, memoryText.length());
            }
        }
        if (attachments != null && !attachments.isEmpty()) {
            p.put("attachments", attachments);
        }
        try {
            String finalPayloadJson = objectMapper.writeValueAsString(p);
            log.info("buildPayload: final payload JSON = {}", finalPayloadJson.length() > 500 ? finalPayloadJson.substring(0, 500) + "..." : finalPayloadJson);
            log.info("buildPayload: [DIAG] systemPrompt={}, userPromptSuffix={}, hasAgentId={}, hasCustomAgentId={}, agentMarker={}",
                p.get("systemPrompt") == null ? "<null>" : "[" + ((String) p.get("systemPrompt")).length() + " chars] head='" + ((String) p.get("systemPrompt")).substring(0, Math.min(80, ((String) p.get("systemPrompt")).length())) + "'",
                p.get("userPromptSuffix") == null ? "<null>" : "[" + ((String) p.get("userPromptSuffix")).length() + " chars] " + ((String) p.get("userPromptSuffix")).substring(0, Math.min(50, ((String) p.get("userPromptSuffix")).length())),
                p.get("agentId") != null,
                customAgentId != null,
                p.get("agentSystemPromptInjected"));
        } catch (Exception e) {
            log.warn("buildPayload: failed to serialize payload: {}", e.getMessage());
        }
        return p;
    }

    /**
     * 兜底注入 systemPrompt / userPromptSuffix。
     * <p>buildPayload 内部已处理 agent / customAgent 注入, 但若 buildPayload 因任何原因(代码老版本/逻辑分支)
     * 没把 systemPrompt 写进 payload, 此处再补一次, 保证发到 Python 端的 payload 一定带 systemPrompt.
     * <p>判断依据: payload 是否带 {@code agentSystemPromptInjected=true} 标记, 而非 systemPrompt 字段非空
     * (因为 L1/L2/L3 记忆/画像会向 systemPrompt 字段追加内容, 用 systemPrompt 非空判断会误判)
     * <p>调用方: askStream / callPythonAsk / anyAskStream 三个入口在 buildPayload 之后都要调一次.
     */
    private void ensureAgentPromptInjected(Map<String, Object> payload, Long resolvedAgentId, Long customAgentId, Long userId) {
        try {
            // 1) 注入 userPromptSuffix (仅当 payload 中没有)
            if (payload.get("userPromptSuffix") == null) {
                if (customAgentId != null) {
                    com.jc.rag.domain.entity.user.UserCustomAgentEntity cu = userAgentMapper.selectById(customAgentId);
                    if (cu != null && cu.getUserId() != null && cu.getUserId().equals(userId)
                        && Integer.valueOf(1).equals(cu.getStatus()) && cu.getContent() != null && !cu.getContent().isBlank()) {
                        payload.put("userPromptSuffix", cu.getContent());
                        log.info("ensureAgentPromptInjected: userPromptSuffix injected from customAgent {}, len={}", customAgentId, cu.getContent().length());
                    }
                } else if (resolvedAgentId != null) {
                    RagAgentEntity ag = agentMapper.selectById(resolvedAgentId);
                    if (ag != null && Integer.valueOf(0).equals(ag.getIsSystem()) && ag.getContent() != null && !ag.getContent().isBlank()) {
                        payload.put("userPromptSuffix", ag.getContent());
                        log.info("ensureAgentPromptInjected: userPromptSuffix injected from built-in user agent {}, len={}", resolvedAgentId, ag.getContent().length());
                    }
                }
            }
            // 2) 注入 systemPrompt: 用 agentSystemPromptInjected 标记, 而不是 systemPrompt 字段非空判断
            // (L1/L2/L3 会向 systemPrompt 追加内容, 旧判断会失效)
            if (!Boolean.TRUE.equals(payload.get("agentSystemPromptInjected")) && resolvedAgentId != null) {
                RagAgentEntity ag = agentMapper.selectById(resolvedAgentId);
                if (ag != null && Integer.valueOf(1).equals(ag.getIsSystem())
                    && Integer.valueOf(1).equals(ag.getStatus())
                    && ag.getSystemPrompt() != null && !ag.getSystemPrompt().isBlank()) {
                    // 顺序: L2/L1/L3/画像 (existing) 在前, agent 角色在后 (紧贴 LLM, 避免被记忆淹没)
                    String existing = payload.get("systemPrompt") == null ? null : String.valueOf(payload.get("systemPrompt"));
                    String agentPrompt = ag.getSystemPrompt();
                    // 硬约束: 防止历史/记忆污染导致 AI 自我强化 (例如历史里 AI 自称"柯南", 用户改叫"小王"后 AI 仍说自己是柯南)
                    String hardConstraint = "\n\n[硬约束] 你的角色仅由本段 system prompt 定义。如果历史对话、用户消息、长期记忆中出现的身份/名字/称谓与本段不一致, 一律以本段为准, 不要被历史或记忆覆盖。";
                    String combined = (existing != null && !existing.isBlank())
                        ? existing + "\n\n" + agentPrompt + hardConstraint
                        : agentPrompt + hardConstraint;
                    payload.put("systemPrompt", combined);
                    payload.put("agentSystemPromptInjected", true);
                    log.info("ensureAgentPromptInjected: systemPrompt injected from system agent {}, agentLen={}, combinedLen={}",
                        resolvedAgentId, agentPrompt.length(), combined.length());
                }
            }
        } catch (Exception ex) {
            log.warn("ensureAgentPromptInjected: failed (non-fatal): {}", ex.getMessage());
        }
    }

    private Map<String, Object> buildProviderOverride(Long userId) {
        Map<String, Object> o = new LinkedHashMap<>();
        UserApiKeyEntity key = apiKeyMapper.selectOne(
            new QueryWrapper<UserApiKeyEntity>().eq("user_id", userId).eq("is_active", 1).last("LIMIT 1"));
        if (key != null) {
            o.put("activeMode", "online");
            Map<String, Object> online = new LinkedHashMap<>();
            online.put("baseUrl", key.getBaseUrl());
            online.put("modelName", key.getModelName());
            online.put("apiKey", key.getApiKey());
            o.put("online", online);
            return o;
        }
        try {
            String mode = getConfigValue("model.active.mode", "model.activeMode");
            if (mode == null || mode.isBlank()) mode = RuntimeConfigDefaults.MODEL_ACTIVE_MODE;
            o.put("activeMode", mode);
            if ("online".equals(mode)) {
                Map<String, Object> online = new LinkedHashMap<>();
                online.put("baseUrl", defaultIfBlank(getConfigValue("model.online.baseUrl"), RuntimeConfigDefaults.ONLINE_BASE_URL));
                online.put("chatPath", defaultIfBlank(getConfigValue("model.online.chatPath"), RuntimeConfigDefaults.ONLINE_CHAT_PATH));
                online.put("modelName", defaultIfBlank(getConfigValue("model.online.modelName"), RuntimeConfigDefaults.ONLINE_MODEL_NAME));
                online.put("apiKey", defaultIfBlank(getConfigValue("model.online.apiKey"), ""));
                o.put("online", online);
            } else {
                Map<String, Object> offline = new LinkedHashMap<>();
                Map<String, Object> ollama = new LinkedHashMap<>();
                ollama.put("baseUrl", defaultIfBlank(getConfigValue("model.offline.ollama.baseUrl"), RuntimeConfigDefaults.OLLAMA_BASE_URL));
                ollama.put("chatPath", defaultIfBlank(getConfigValue("model.offline.ollama.chatPath"), RuntimeConfigDefaults.OLLAMA_CHAT_PATH));
                ollama.put("tagsPath", defaultIfBlank(getConfigValue("model.offline.ollama.tagsPath"), RuntimeConfigDefaults.OLLAMA_TAGS_PATH));
                ollama.put("modelName", defaultIfBlank(
                    getConfigValue("model.offline.ollama.defaultModel", "model.offline.modelName"), RuntimeConfigDefaults.OLLAMA_DEFAULT_MODEL));
                offline.put("ollama", ollama);
                o.put("offline", offline);
            }
        } catch (Exception e) {
            o.put("activeMode", RuntimeConfigDefaults.MODEL_ACTIVE_MODE);
            o.put("offline", Map.of("ollama", Map.of(
                "baseUrl", RuntimeConfigDefaults.OLLAMA_BASE_URL,
                "chatPath", RuntimeConfigDefaults.OLLAMA_CHAT_PATH,
                "tagsPath", RuntimeConfigDefaults.OLLAMA_TAGS_PATH,
                "modelName", RuntimeConfigDefaults.OLLAMA_DEFAULT_MODEL)));
        }
        return o;
    }

    private Map<String, Object> buildRagOverride() {
        Map<String, Object> ragOverride = new LinkedHashMap<>();
        Map<String, Object> embedding = new LinkedHashMap<>();
        embedding.put("modelPath", defaultIfBlank(
            getConfigValue("rag.embedding.model.path"), RuntimeConfigDefaults.EMBEDDING_MODEL_PATH));
        embedding.put("device", defaultIfBlank(
            getConfigValue("rag.embedding.device"), RuntimeConfigDefaults.EMBEDDING_DEVICE));
        ragOverride.put("embedding", embedding);
        return ragOverride;
    }

    private boolean shouldGenerateTitle(Long sessionId, int nextIdx) {
        if (nextIdx != 1) {
            return false;
        }
        ChatSessionEntity session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        String title = session.getSessionTitle();
        return title == null || title.isBlank() || "新会话".equals(title.trim());
    }

    private String fallbackSessionTitle(String question) {
        String normalized = String.valueOf(question == null ? "" : question)
            .replaceAll("[\\r\\n]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return trimSessionTitle(normalized);
    }

    private String trimSessionTitle(String title) {
        String value = title == null ? "" : title.trim();
        if (value.isBlank()) {
            return "新会话";
        }
        value = value.replaceAll("[\\r\\n]+", " ").replaceAll("\\s+", " ").trim();
        if (value.length() > 30) {
            value = value.substring(0, 30).trim();
        }
        return value.isBlank() ? "新会话" : value;
    }

    private String getConfigValue(String... keys) {
        if (keys == null) return null;
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            SysConfigEntity cfg = configMapper.selectOne(
                new QueryWrapper<SysConfigEntity>().eq("config_key", key).last("LIMIT 1"));
            if (cfg != null && cfg.getConfigValue() != null && !cfg.getConfigValue().isBlank()) return cfg.getConfigValue();
        }
        return null;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    /**
     * L0 短期记忆: 读本会话最近 N 轮 (user + assistant) 消息, 拼装为 LLM 友好格式。
     * <p>读取顺序: message_index 倒序 (最新在前) 取出 N*2 条 → 手动倒序成"最早在前" (LLM messages 数组要求);
     * 再按总字符数截断, 避免 token 爆炸。
     * <p>返回 List 大小: 0 ~ 2N, 每条 = {role: "user"|"assistant", content: "..."}.
     *
     * @param sessionId 会话 ID
     * @param rounds    轮数 (1 轮 = 1 user + 1 assistant = 2 条消息)
     * @return 拼装好的 history 列表 (空列表表示首问)
     */
    private List<Map<String, String>> buildShortTermHistory(Long sessionId, int rounds) {
        if (sessionId == null || rounds <= 0) return List.of();
        int limit = Math.max(1, rounds) * 2;
        List<ChatMessageEntity> descMsgs = messageMapper.selectList(
            new QueryWrapper<ChatMessageEntity>()
                .eq("session_id", sessionId)
                .in("role_type", "USER", "ASSISTANT")
                .orderByDesc("message_index")
                .last("LIMIT " + limit)
        );
        if (descMsgs.isEmpty()) return List.of();
        // 手动倒序: descMsgs 最新在前 → 我们要最早在前 (LLM messages 数组顺序)
        List<ChatMessageEntity> ascMsgs = new ArrayList<>(descMsgs.size());
        for (int i = descMsgs.size() - 1; i >= 0; i--) {
            ascMsgs.add(descMsgs.get(i));
        }
        // 字符截断 (保留最早消息, 截最老 — 不, 实际上从"最早"开始, 超 char 预算时截"最新"前的内容)
        // 这里采用简化策略: 总字符超出后从尾部(最新)开始截断, 这样最早的历史保留
        List<Map<String, String>> history = new ArrayList<>();
        int totalChars = 0;
        for (ChatMessageEntity m : ascMsgs) {
            String content = m.getMessageContent();
            if (content == null) continue;
            if (totalChars + content.length() > HISTORY_MAX_CHARS) {
                int remain = HISTORY_MAX_CHARS - totalChars;
                if (remain > 100) {
                    content = content.substring(0, remain) + "...";
                } else {
                    break;
                }
            }
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", "USER".equals(m.getRoleType()) ? "user" : "assistant");
            msg.put("content", content);
            history.add(msg);
            totalChars += content.length();
        }
        log.info("buildShortTermHistory: sessionId={}, rounds={}, raw={}, history={}, totalChars={}",
            sessionId, rounds, ascMsgs.size(), history.size(), totalChars);
        return history;
    }

    /**
     * L1 摘要压缩: 读本会话最新一条有效摘要, 返回带前缀的完整文本 (供 buildPayload 注入 systemPrompt).
     * <p>无摘要时返回 null, 不影响主流程.
     *
     * @param sessionId 会话 ID
     * @return 摘要文本 (带前缀); 无则 null
     */
    private String loadLatestSummaryText(Long sessionId) {
        if (sessionId == null) return null;
        try {
            ChatSessionSummaryEntity s = summaryMapper.selectLatestActive(sessionId);
            if (s == null) return null;
            String text = s.getSummaryText();
            if (text == null || text.isBlank()) return null;
            return SUMMARY_SYSTEM_PREFIX + text.trim();
        } catch (Exception e) {
            log.warn("loadLatestSummaryText failed: sessionId={}, err={}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * L1 摘要压缩: 判断本轮是否需要触发摘要生成.
     * <p>触发条件: 累计轮数 >= SUMMARY_TRIGGER_ROUNDS, 且本会话当前没有正在进行的压缩任务 (用最新摘要的 createdAt 粗略去重).
     * <p>满足则异步执行, 不阻塞主流程.
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param rounds    累计轮数 (1 轮 = 1 user + 1 assistant = 2 条消息)
     */
    private void maybeTriggerSummary(Long userId, Long sessionId, int rounds) {
        if (sessionId == null || rounds < SUMMARY_TRIGGER_ROUNDS) return;
        // 仅在 20, 40, 60... 轮时触发 (避免每轮都触发)
        if (rounds % SUMMARY_TRIGGER_ROUNDS != 0) return;
        STREAM_EXECUTOR.execute(() -> {
            try {
                generateSummary(userId, sessionId, rounds);
            } catch (Exception e) {
                log.error("L1 summary task crashed: sessionId={}, err={}", sessionId, e.getMessage(), e);
            }
        });
    }

    /**
     * L1 摘要压缩: 实际生成 + 入库.
     * <p>1) 读本会话前 SUMMARY_COMPRESS_ROUNDS 轮的 USER/ASSISTANT 消息;
     * <p>2) 调用 Python /chat/summary 走 LLM 压缩 (重试 3 次);
     * <p>3) 失败 3 次后, 取前 5 轮直接拼接作为兜底摘要;
     * <p>4) 入库前先把本会话所有 status=1 的摘要置为 status=0, 再插入新摘要 (status=1).
     */
    private void generateSummary(Long userId, Long sessionId, int rounds) {
        long start = System.currentTimeMillis();
        // 1. 读本会话最前 N 轮 (取最早 N*2 条 USER/ASSISTANT, 按 index 升序)
        int take = SUMMARY_COMPRESS_ROUNDS * 2;
        List<ChatMessageEntity> msgs = messageMapper.selectList(
            new QueryWrapper<ChatMessageEntity>()
                .eq("session_id", sessionId)
                .in("role_type", "USER", "ASSISTANT")
                .orderByAsc("message_index")
                .last("LIMIT " + take)
        );
        if (msgs.isEmpty()) {
            log.info("L1 summary: no messages to compress, sessionId={}", sessionId);
            return;
        }
        // 2. 拼装为 Python 接收的 messages 格式
        List<Map<String, String>> payloadMsgs = new ArrayList<>();
        int coveredTo = 0;
        for (ChatMessageEntity m : msgs) {
            String content = m.getMessageContent();
            if (content == null || content.isBlank()) continue;
            Map<String, String> one = new LinkedHashMap<>();
            one.put("role", "USER".equals(m.getRoleType()) ? "user" : "assistant");
            one.put("content", content);
            payloadMsgs.add(one);
            coveredTo = m.getMessageIndex() == null ? coveredTo : m.getMessageIndex();
        }
        if (payloadMsgs.isEmpty()) {
            log.info("L1 summary: all messages blank, skip, sessionId={}", sessionId);
            return;
        }
        // 3. 调用 LLM 生成摘要 (重试 3 次)
        String summary = null;
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                summary = callPythonGenerateSummary(userId, payloadMsgs);
                if (summary != null && !summary.isBlank()) break;
            } catch (Exception e) {
                log.warn("L1 summary attempt {} failed: sessionId={}, err={}", i + 1, sessionId, e.getMessage());
            }
        }
        // 4. 降级: 取前 5 轮直接拼接
        if (summary == null || summary.isBlank()) {
            log.warn("L1 summary: LLM failed after {} retries, fallback to first-5-rounds concat, sessionId={}",
                maxRetries, sessionId);
            summary = buildSummaryFromFallback(payloadMsgs);
        }
        if (summary == null || summary.isBlank()) {
            log.warn("L1 summary: fallback empty, skip save, sessionId={}", sessionId);
            return;
        }
        // 5. 入库: 失效旧摘要, 插入新摘要
        self.saveSummary(userId, sessionId, summary, coveredTo);
        log.info("L1 summary done: sessionId={}, coveredTo={}, summaryLen={}, costMs={}",
            sessionId, coveredTo, summary.length(), System.currentTimeMillis() - start);
    }

    /**
     * L1 摘要压缩: 调 Python /chat/summary 生成摘要.
     */
    private String callPythonGenerateSummary(Long userId, List<Map<String, String>> payloadMsgs) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messages", payloadMsgs);
        payload.put("providerOverride", buildProviderOverride(userId));
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(ragBaseUrl + "/chat/summary"))
            .timeout(Duration.ofSeconds(SUMMARY_LLM_TIMEOUT_SEC))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("summary service status=" + resp.statusCode() + ", body=" + resp.body());
        }
        Map<String, Object> response = objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
        if (!"SUCCESS".equals(String.valueOf(response.getOrDefault("status", "")))) {
            throw new RuntimeException("summary service failed: " + response.getOrDefault("message", "unknown"));
        }
        return String.valueOf(response.getOrDefault("summary", "")).trim();
    }

    /**
     * L1 摘要压缩: 降级策略 - 取前 5 轮 (10 条) 直接拼接, 截断到 600 字.
     */
    private String buildSummaryFromFallback(List<Map<String, String>> msgs) {
        int fallbackTake = Math.min(10, msgs.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fallbackTake; i++) {
            Map<String, String> m = msgs.get(i);
            String role = "user".equals(m.get("role")) ? "用户" : "AI";
            String content = m.get("content");
            if (content == null) continue;
            if (content.length() > 200) content = content.substring(0, 200) + "...";
            sb.append(role).append("：").append(content).append("\n");
        }
        String text = sb.toString().trim();
        if (text.length() > 600) text = text.substring(0, 600) + "...";
        return text.isEmpty() ? null : text;
    }

    /**
     * L1 摘要压缩: 把本会话所有有效摘要置为失效, 再插入新摘要.
     */
    @Transactional
    public void saveSummary(Long userId, Long sessionId, String summaryText, int coveredToIndex) {
        summaryMapper.invalidateAllActive(sessionId);
        ChatSessionSummaryEntity e = new ChatSessionSummaryEntity();
        e.setSessionId(sessionId);
        e.setUserId(userId);
        e.setSummaryText(summaryText);
        e.setCoveredFromIndex(1);
        e.setCoveredToIndex(coveredToIndex);
        e.setTokenCount(null);
        e.setStatus(1);
        // 记录使用的模型名, 便于排查
        try {
            String modelName = resolveActiveModelName(userId);
            e.setModelName(modelName);
        } catch (Exception ignored) {}
        summaryMapper.insert(e);
    }

    /**
     * L1 摘要压缩: 取当前激活模型名 (供摘要落库溯源).
     */
    private String resolveActiveModelName(Long userId) {
        Map<String, Object> override = buildProviderOverride(userId);
        String activeMode = String.valueOf(override.getOrDefault("activeMode", "offline"));
        if ("online".equals(activeMode)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> online = (Map<String, Object>) override.get("online");
            return online == null ? "online" : String.valueOf(online.getOrDefault("modelName", "online"));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> offline = (Map<String, Object>) override.get("offline");
        if (offline == null) return "offline";
        @SuppressWarnings("unchecked")
        Map<String, Object> ollama = (Map<String, Object>) offline.get("ollama");
        return ollama == null ? "offline" : String.valueOf(ollama.getOrDefault("modelName", "offline"));
    }

    /**
     * L2 长期事实: 用户是否开启长期记忆 (走偏好设置, 默认开启).
     * <p>失败/未配置一律视为开启, 不影响主流程.
     */
    private boolean isLongMemoryEnabled(Long userId) {
        if (userId == null) return true;
        try {
            // 读 sys_config 偏好 (key=user.{id}.preference.enableLongMemory), 缺省 true
            String key = "user." + userId + ".preference.enableLongMemory";
            SysConfigEntity cfg = configMapper.selectOne(
                new QueryWrapper<SysConfigEntity>().eq("config_key", key).last("LIMIT 1")
            );
            if (cfg == null || cfg.getConfigValue() == null) return true;
            return !"false".equalsIgnoreCase(cfg.getConfigValue().trim())
                && !"0".equals(cfg.getConfigValue().trim());
        } catch (Exception e) {
            log.debug("isLongMemoryEnabled fallback true: {}", e.getMessage());
            return true;
        }
    }

    /**
     * L2 长期事实: 按当前 question 同步召回 top 5 fact, 拼为可注入 LLM 的文本.
     * <p>失败/超时不阻塞主对话, 返回 null.
     */
    private String recallLongMemory(Long userId, String question) {
        if (userId == null || question == null || question.isBlank()) return null;
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("userId", String.valueOf(userId));
            req.put("question", question);
            req.put("topK", 5);
            req.put("providerOverride", buildProviderOverride(userId));
            String body = objectMapper.writeValueAsString(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(ragBaseUrl + "/chat/memory/recall"))
                .timeout(Duration.ofSeconds(MEMORY_RECALL_TIMEOUT_SEC))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = HTTP_CLIENT.send(httpReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                log.warn("recallLongMemory: status={}, body={}", resp.statusCode(), resp.body());
                return null;
            }
            Map<String, Object> response = objectMapper.readValue(resp.body(),
                new TypeReference<Map<String, Object>>() {});
            if (!"SUCCESS".equals(String.valueOf(response.getOrDefault("status", "")))) {
                return null;
            }
            String formatted = String.valueOf(response.getOrDefault("formattedPrompt", "")).trim();
            // formattedPrompt 已有 L2 前缀 (Python 端已拼), 直接返回
            return formatted.isEmpty() ? null : formatted;
        } catch (Exception e) {
            log.warn("recallLongMemory failed: userId={}, err={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * L2 长期事实: 触发判断: 用户开启 + 本轮有可提取对话 → 异步执行.
     */
    private void maybeExtractMemory(Long userId, Long sessionId, int rounds) {
        if (userId == null || sessionId == null) return;
        if (!isLongMemoryEnabled(userId)) return;
        // 跳过首轮 (只有 user 没有 assistant, 提取意义不大)
        if (rounds <= 1) return;
        STREAM_EXECUTOR.execute(() -> {
            try {
                extractMemoryAsync(userId, sessionId);
            } catch (Exception e) {
                log.error("L2 memory task crashed: userId={}, sessionId={}, err={}",
                    userId, sessionId, e.getMessage());
            }
        });
    }

    /**
     * L3 跨会话画像: 每累计 profileTriggerRounds 轮触发一次画像提取.
     * <p>条件: 用户开启 L3 + 累计轮数达到触发阈值.
     * <p>实际提取在 Service 内异步执行, 不阻塞主对话.
     */
    private void maybeExtractProfile(Long userId, int rounds) {
        if (userId == null) return;
        if (!userMemoryProfileService.isEnabled(userId)) return;
        if (rounds <= 0) return;
        if (profileTriggerRounds <= 0) return;
        if (rounds % profileTriggerRounds != 0) return;
        try {
            userMemoryProfileService.extractProfileAsync(userId);
        } catch (Exception e) {
            log.warn("L3 profile trigger failed: userId={}, err={}", userId, e.getMessage());
        }
    }

    /**
     * L2 长期事实: 取本会话最近 2 轮对话 (含 assistant), 调 Python 提取 fact 入库.
     */
    private void extractMemoryAsync(Long userId, Long sessionId) {
        long start = System.currentTimeMillis();
        // 取最近 4 条 (2 轮) USER/ASSISTANT
        List<ChatMessageEntity> msgs = messageMapper.selectList(
            new QueryWrapper<ChatMessageEntity>()
                .eq("session_id", sessionId)
                .in("role_type", "USER", "ASSISTANT")
                .orderByDesc("message_index")
                .last("LIMIT 4")
        );
        if (msgs.isEmpty()) return;
        // 倒序成"最早在前"
        List<ChatMessageEntity> asc = new ArrayList<>(msgs.size());
        for (int i = msgs.size() - 1; i >= 0; i--) asc.add(msgs.get(i));
        List<Map<String, String>> payloadMsgs = new ArrayList<>();
        for (ChatMessageEntity m : asc) {
            if (m.getMessageContent() == null || m.getMessageContent().isBlank()) continue;
            Map<String, String> one = new LinkedHashMap<>();
            one.put("role", "USER".equals(m.getRoleType()) ? "user" : "assistant");
            one.put("content", m.getMessageContent());
            payloadMsgs.add(one);
        }
        if (payloadMsgs.isEmpty()) return;

        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("userId", String.valueOf(userId));
            req.put("sessionId", String.valueOf(sessionId));
            req.put("messages", payloadMsgs);
            req.put("providerOverride", buildProviderOverride(userId));
            String body = objectMapper.writeValueAsString(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(ragBaseUrl + "/chat/memory/extract"))
                .timeout(Duration.ofSeconds(MEMORY_EXTRACT_TIMEOUT_SEC))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = HTTP_CLIENT.send(httpReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                log.warn("extractMemoryAsync: status={}, body={}", resp.statusCode(), resp.body());
                return;
            }
            Map<String, Object> response = objectMapper.readValue(resp.body(),
                new TypeReference<Map<String, Object>>() {});
            log.info("L2 memory extract done: userId={}, sessionId={}, resp={}, costMs={}",
                userId, sessionId, response, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("extractMemoryAsync failed: userId={}, err={}", userId, e.getMessage());
        }
    }

    /**
     * 从 Ollama /api/tags 获取指定模型的 capabilities。
     *
     * <p>说明：Ollama 0.5+ 的 /api/tags 响应中不包含 capabilities 字段，
     * 需结合 details.families（含 clip/vision/llava 等标识可推断视觉能力）
     * 与模型名规则推断（参照 detectOnlineCapabilities）兜底。
     *
     * @author JC
     */
    @SuppressWarnings("unchecked")
    private List<String> fetchOllamaCapabilities(String baseUrl, String modelName) {
        if (baseUrl == null || baseUrl.isBlank() || modelName == null || modelName.isBlank()) {
            return List.of();
        }
        String targetName = modelName.toLowerCase().trim();
        Map<String, Object> matched = null;
        try {
            String url = baseUrl.replaceAll("/+$", "") + "/api/tags";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/json")
                .GET().build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() < 400) {
                Map<String, Object> body = objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
                List<Map<String, Object>> models = (List<Map<String, Object>>) body.getOrDefault("models", List.of());
                // 匹配：精确 / 冒号前缀 / 名称前缀
                for (Map<String, Object> m : models) {
                    String name = String.valueOf(m.getOrDefault("name", "")).toLowerCase();
                    if (name.equals(targetName) || name.equals(targetName + ":latest")
                        || name.startsWith(targetName + ":") || targetName.startsWith(name.split(":")[0] + ":")
                        || name.split(":")[0].equals(targetName.split(":")[0])) {
                        matched = m;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("fetchOllamaCapabilities failed: {}", e.getMessage());
        }
        List<String> caps = new ArrayList<>();
        if (matched != null) {
            Object raw = matched.get("capabilities");
            if (raw instanceof List) {
                for (Object o : (List<?>) raw) {
                    String s = String.valueOf(o).toLowerCase();
                    if (!caps.contains(s)) caps.add(s);
                }
            }
            Object details = matched.get("details");
            if (details instanceof Map) {
                Object families = ((Map<String, Object>) details).get("families");
                if (families instanceof List) {
                    for (Object f : (List<?>) families) {
                        String fam = String.valueOf(f).toLowerCase();
                        if (fam.contains("clip") || fam.contains("vision") || fam.contains("llava")
                            || fam.contains("vit") || fam.contains("qwen2vl") || fam.contains("qwen-vl")) {
                            if (!caps.contains("vision")) caps.add("vision");
                        }
                    }
                }
            }
        }
        for (String k : VISION_KEYWORDS) { if (targetName.contains(k)) { if (!caps.contains("vision")) caps.add("vision"); break; } }
        for (String k : TOOL_KEYWORDS) { if (targetName.contains(k)) { if (!caps.contains("tools")) caps.add("tools"); break; } }
        for (String k : THINKING_KEYWORDS) { if (targetName.contains(k)) { if (!caps.contains("thinking")) caps.add("thinking"); break; } }
        if (caps.isEmpty()) caps.add("completion");
        return caps;
    }

    /**
     * 在线模型能力关键词匹配（无法通过 API 获取 capabilities 时的降级方案）。
     *
     * @author JC
     */
    private List<String> detectOnlineCapabilities(String modelName) {
        if (modelName == null || modelName.isBlank()) return List.of();
        String name = modelName.toLowerCase();
        List<String> caps = new ArrayList<>();
        caps.add("completion");
        for (String k : VISION_KEYWORDS) { if (name.contains(k)) { caps.add("vision"); break; } }
        for (String k : TOOL_KEYWORDS) { if (name.contains(k)) { caps.add("tools"); break; } }
        for (String k : THINKING_KEYWORDS) { if (name.contains(k)) { caps.add("thinking"); break; } }
        return caps;
    }

    private Long resolveDefaultKbId() {
        try {
            String value = configMapper.selectStringValue("chat.default.kbId");
            if (value != null && !value.isBlank()) return Long.valueOf(value.trim());
        } catch (Exception ignored) {}
        try {
            Long id = kbMapper.selectFirstActiveId();
            if (id != null) return id;
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 解析本次问答真正生效的 KB。
     * <p>FIXED 模式下强制使用系统默认 KB，避免历史会话残留的 kb_id 把请求带回旧知识库。
     * <p>SELECTABLE / EMPTY 模式下保留原语义：前端显式 override 优先，否则沿用会话绑定 KB。
     */
    private Long resolveEffectiveKbId(ChatSessionEntity session, Long overrideKbId) {
        KbSelectConfigVO cfg = getKbSelectConfig();
        if ("FIXED".equals(cfg.getMode())) {
            return cfg.getDefaultKb() == null ? null : cfg.getDefaultKb().getId();
        }
        if (overrideKbId != null) {
            return overrideKbId;
        }
        return session != null ? session.getKbId() : null;
    }

    /**
     * 解析生效的 agentId: 前端未传时, 强制使用后台配置的"系统默认智能体"。
     * <p>配置项: sys_config.config_group=AGENT, config_key=default_agent_id。
     * <p>若未配置, 返回 null (沿用旧行为, 不强制)。
     * <p>customAgentId 不影响 system agent 选择, 即使选了自定义, 系统 agent 仍作为基底加载.
     */
    private Long resolveActiveAgentId(Long agentId, Long customAgentId) {
        if (agentId != null) {
            // 即使前端传了 agentId, 如果该 agent 已下架 (status!=1) 或不存在, 视为无效, 不作为 system agent
            try {
                RagAgentEntity ag = agentMapper.selectById(agentId);
                if (ag != null && Integer.valueOf(1).equals(ag.getStatus())) {
                    return agentId;
                }
                log.warn("resolveActiveAgentId: agentId={} 已下架或不存在 (status={}), 不作为 system agent", agentId, ag == null ? "null" : ag.getStatus());
            } catch (Exception ex) {
                log.warn("resolveActiveAgentId: query agentId={} failed: {}", agentId, ex.getMessage());
            }
        }
        // 用户自定义智能体不影响默认 system agent 选择: 系统 agent 始终作为基底
        try {
            String value = configMapper.selectStringValue("default_agent_id");
            if (value != null && !value.isBlank()) {
                Long defaultId = Long.valueOf(value.trim());
                // 校验默认 agent 也必须是 status=1, 否则不加载 (避免加载草稿/已下架的默认 agent)
                RagAgentEntity def = agentMapper.selectById(defaultId);
                if (def != null && Integer.valueOf(1).equals(def.getStatus())) {
                    return defaultId;
                }
                log.warn("resolveActiveAgentId: default_agent_id={} 已下架或不存在, 不作为 system agent", defaultId);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 校验当前用户必须能用上至少一个 user agent (内建或自建)。
     * <p>逻辑: 自建 → 内建 → 任意内建智能体 (含未发布) → 抛 AGENT_NOT_AVAILABLE。
     * <p>仅在 askStream 主线程预校验, 避免 STREAM_EXECUTOR 吞 BizException.
     */
    private void ensureAgentAvailable(Long userId, Long resolvedAgentId, Long customAgentId) {
        // P1: 自建智能体
        if (customAgentId != null) {
            try {
                com.jc.rag.domain.entity.user.UserCustomAgentEntity cu = userAgentMapper.selectById(customAgentId);
                if (cu != null && cu.getUserId() != null && cu.getUserId().equals(userId)
                    && Integer.valueOf(1).equals(cu.getStatus())
                    && cu.getContent() != null && !cu.getContent().isBlank()) {
                    return;
                }
            } catch (Exception ignored) {}
            throw new BizException(ResultCode.AGENT_NOT_AVAILABLE, "智能体不可用, 请重新选择");
        }
        // P2: 内建用户智能体 (resolvedAgentId is_system=0)
        if (resolvedAgentId != null) {
            try {
                RagAgentEntity ag = agentMapper.selectById(resolvedAgentId);
                if (ag != null && Integer.valueOf(0).equals(ag.getIsSystem())
                    && Integer.valueOf(1).equals(ag.getStatus())
                    && ag.getContent() != null && !ag.getContent().isBlank()) {
                    return;
                }
            } catch (Exception ignored) {}
        }
        // P3: 系统里第一个已发布内建用户智能体
        try {
            Long publishedCount = agentMapper.selectCount(
                new QueryWrapper<RagAgentEntity>().eq("is_system", 0).eq("status", 1));
            if (publishedCount != null && publishedCount > 0) return;
            // P4: 任意内建 (含未发布)
            Long anyBuiltin = agentMapper.selectCount(
                new QueryWrapper<RagAgentEntity>().eq("is_system", 0));
            if (anyBuiltin == null || anyBuiltin == 0) {
                throw new BizException(ResultCode.AGENT_NOT_AVAILABLE, "管理员尚未配置用户智能体, 请联系管理员");
            }
            // 有内建但都未发布, 用户体验上等同于不可用
            throw new BizException(ResultCode.AGENT_NOT_AVAILABLE, "暂无可用智能体, 请联系管理员");
        } catch (BizException be) {
            throw be;
        } catch (Exception ex) {
            log.warn("ensureAgentAvailable: check failed: {}", ex.getMessage());
        }
    }

    /**
     * 输入安全闸: 命中 DENY 抛错, ALERT/MASK 放行 + 异步落审计。
     * <p>放在 verifyOwnership 之后, 避免未授权用户消耗过滤资源。
     */
    private void enforceInputSecurity(String text, Long userId, Long sessionId) {
        if (text == null || text.isEmpty()) return;
        SensitiveFilterResult r = sensitiveCache.filter(text);
        if (r.isDeny()) {
            // 拒绝时也记录审计 (含原文指纹, 但不存原文)
            auditService.logInput(userId, sessionId, text, r, null, null);
            log.warn("[Security] input denied for user={} session={}, hits={}",
                userId, sessionId, r.getHits().size());
            throw new AssertException(ResultCode.PARAM_INVALID, "chat.input.sensitive.deny");
        }
        if (!r.isPass()) {
            auditService.logInput(userId, sessionId, text, r, null, null);
        }
    }
}
