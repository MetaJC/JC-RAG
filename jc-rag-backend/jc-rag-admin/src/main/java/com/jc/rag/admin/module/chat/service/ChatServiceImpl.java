package com.jc.rag.admin.module.chat.service;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jc.rag.domain.entity.chat.ChatMessageEntity;
import com.jc.rag.domain.entity.chat.ChatMessageReferenceEntity;
import com.jc.rag.domain.entity.chat.ChatSessionEntity;
import com.jc.rag.domain.mapper.chat.ChatMessageMapper;
import com.jc.rag.domain.mapper.chat.ChatMessageReferenceMapper;
import com.jc.rag.domain.mapper.chat.ChatSessionMapper;
import com.jc.rag.domain.mapper.rag.RagAgentMapper;
import com.jc.rag.domain.mapper.rag.RagCallLogMapper;
import com.jc.rag.domain.entity.rag.RagCallLog;
import com.jc.rag.admin.config.ExternalServiceProperties;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.admin.module.chat.model.param.AdminChatSessionQueryParam;
import com.jc.rag.admin.module.chat.model.param.ChatAskParam;
import com.jc.rag.admin.module.chat.model.param.ChatSessionCreateParam;
import com.jc.rag.admin.module.chat.model.vo.AdminAskResponseVO;
import com.jc.rag.admin.module.chat.model.vo.AdminChatMessageVO;
import com.jc.rag.admin.module.chat.model.vo.AdminChatSessionVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.admin.module.sys.service.OperationLogService;
import com.jc.rag.admin.module.sys.service.RuntimeConfigService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;



import java.io.BufferedReader;

import java.io.InputStreamReader;

import java.net.URI;

import java.net.http.HttpClient;

import java.net.http.HttpRequest;

import java.net.http.HttpResponse;

import java.net.http.HttpTimeoutException;

import java.nio.charset.StandardCharsets;

import java.time.Duration;

import java.util.ArrayList;

import java.util.LinkedHashMap;

import java.util.List;

import java.util.Map;

import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;




import org.springframework.stereotype.Service;

/**
 * ChatService 实现
 *
 * @author JC
 */
@Service
public class ChatServiceImpl implements ChatService {




    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()

        .connectTimeout(Duration.ofSeconds(5))

        .version(HttpClient.Version.HTTP_1_1)

        .followRedirects(HttpClient.Redirect.NEVER)

        .build();

    private static final ExecutorService STREAM_EXECUTOR = Executors.newCachedThreadPool();



    private final RagCallLogMapper ragCallLogMapper;

    private final ChatSessionMapper sessionMapper;

    private final ChatMessageMapper messageMapper;

    private final ChatMessageReferenceMapper messageReferenceMapper;

    private final RagAgentMapper agentMapper;

    private final ExternalServiceProperties externalServiceProperties;

    private final OperationLogService operationLogService;

    private final RuntimeConfigService runtimeConfigService;

    private final ObjectMapper objectMapper;



    public ChatServiceImpl(

        RagCallLogMapper ragCallLogMapper,

        ChatSessionMapper sessionMapper,

        ChatMessageMapper messageMapper,

        ChatMessageReferenceMapper messageReferenceMapper,

        RagAgentMapper agentMapper,

        ExternalServiceProperties externalServiceProperties,

        OperationLogService operationLogService,

        RuntimeConfigService runtimeConfigService,

        ObjectMapper objectMapper

    ) {

        this.ragCallLogMapper = ragCallLogMapper;

        this.sessionMapper = sessionMapper;

        this.messageMapper = messageMapper;

        this.messageReferenceMapper = messageReferenceMapper;

        this.agentMapper = agentMapper;

        this.externalServiceProperties = externalServiceProperties;

        this.operationLogService = operationLogService;

        this.runtimeConfigService = runtimeConfigService;

        this.objectMapper = objectMapper;

    }



    /**
     * 列出会话列表（按最近更新时间倒序）。
     */
    @Override
    public List<AdminChatSessionVO> listSessions(Long userId) {
        return listSessions(userId, AdminChatSessionQueryParam.all()).getRecords();
    }

    /**
     * 列出会话列表（分页+搜索）。
     */
    @Override
    public PageResponse<AdminChatSessionVO> listSessions(Long userId, AdminChatSessionQueryParam query) {
        String like = PageHelper.buildLikeClause(query.getKeyword());
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = sessionMapper.pageSessions(page, userId, like);
        page.setTotal(sessionMapper.countSessions(userId, like));
        return PageHelper.toPageResponse(page, this::toChatSessionVO);
    }

    /**
     * 行记录转 AdminChatSessionVO。
     */
    private AdminChatSessionVO toChatSessionVO(Map<String, Object> m) {
        AdminChatSessionVO v = new AdminChatSessionVO();
        v.setId(asLong(m.get("id")));
        Object userId = m.get("userId");
        if (userId == null) {
            userId = m.get("user_id");
        }
        v.setUserId(asLong(userId));
        v.setSessionTitle((String) m.get("sessionTitle"));
        v.setSessionMode((String) m.get("sessionMode"));
        v.setModelMode((String) m.get("modelMode"));
        v.setModelName((String) m.get("modelName"));
        Object kbId = m.get("kbId");
        if (kbId == null) {
            kbId = m.get("kb_id");
        }
        v.setKbId(asLong(kbId));
        v.setKbName((String) m.get("kbName"));
        Object status = m.get("status");
        v.setStatus(status == null ? null : Integer.valueOf(String.valueOf(status)));
        Object ca = m.get("createdAt");
        if (ca == null) {
            ca = m.get("created_at");
        }
        v.setCreatedAt(asDateTime(ca));
        Object ua = m.get("updatedAt");
        if (ua == null) {
            ua = m.get("updated_at");
        }
        v.setUpdatedAt(asDateTime(ua));
        return v;
    }



    /**
     * 创建一个新的问答会话。
     */
    @Override
    @Transactional
    public AdminChatSessionVO createSession(Long userId, ChatSessionCreateParam request) {
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setUserId(userId);
        entity.setSessionTitle(request.getSessionTitle());
        entity.setSessionMode("RAG");
        entity.setModelMode("OFFLINE");
        entity.setModelName("qwen1.5-1.8b-gguf");
        entity.setKbId(request.getKbId());
        entity.setStatus(1);
        sessionMapper.insert(entity);
        operationLogService.log("问答中心", "CREATE_SESSION", "创建会话:" + request.getSessionTitle(), "SUCCESS");
        AdminChatSessionVO vo = new AdminChatSessionVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setSessionTitle(entity.getSessionTitle());
        vo.setSessionMode(entity.getSessionMode());
        vo.setModelMode(entity.getModelMode());
        vo.setModelName(entity.getModelName());
        vo.setKbId(entity.getKbId());
        vo.setStatus(entity.getStatus());
        return vo;
    }



    /**
     * 查询会话消息列表（按 message_index 升序）。
     *
     * @deprecated 仅占位, 请使用带用户上下文的 listMessages 接口
     */
    @Override
    @Deprecated
    public List<AdminChatMessageVO> listMessages(Long sessionId) {
        throw new UnsupportedOperationException("请使用带用户上下文的 listMessages 接口");
    }

    /**
     * 查询会话消息列表（按 message_index 升序）。
     */
    @Override
    public List<AdminChatMessageVO> listMessages(Long userId, Long sessionId) {
        verifySessionOwnership(userId, sessionId);
        return messageMapper.listBySessionId(sessionId).stream()
            .map(m -> {
                AdminChatMessageVO v = new AdminChatMessageVO();
                v.setId(asLong(m.get("id")));
                v.setRoleType((String) m.get("roleType"));
                v.setMessageContent((String) m.get("messageContent"));
                v.setMessageIndex(asInt(m.get("messageIndex")));
                v.setTokenCount(asInt(m.get("tokenCount")));
                v.setLatencyMs(asLong(m.get("latencyMs")));
                v.setCreatedAt(asDateTime(m.get("createdAt")));
                return v;
            })
            .toList();
    }



    /**
     * 发起一次问答（非流式）。
     *
     * <p>流程：写入用户消息 -> 调用 Python -> 写入助手消息 -> 写入引用与调用日志。
     */
    @Override
    @Transactional
    public AdminAskResponseVO ask(Long userId, Long sessionId, ChatAskParam request) {
        verifySessionOwnership(userId, sessionId);
        ChatSessionEntity session = sessionMapper.selectByOwner(userId, sessionId);
        if (session == null) {
            throw new AssertException(ResultCode.FORBIDDEN, "chat.session.no.permission");
        }
        int nextIndex = messageMapper.getMaxIndex(sessionId) + 1;

        // 先落库用户消息，保证失败时也可追溯。
        Long userMessageId = insertMessage(sessionId, "USER", request.getQuestion(), nextIndex, 0, 0);
        long startedAt = System.currentTimeMillis();

        Map<String, Object> answerPayload = null;
        boolean pythonCallSuccess = false;
        String errorMessage = null;
        try {
            // providerOverride 由后端运行时配置拼装，Python 端按 override 决定在线/离线与模型参数。
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("kbId", session.getKbId());
            payload.put("question", request.getQuestion());
            payload.put("topK", runtimeConfigService.resolveRagTopK());
            payload.put("topN", runtimeConfigService.resolveRagTopN());
            payload.put("similarityThreshold", runtimeConfigService.resolveRagSimilarityThreshold());
            payload.put("providerOverride", runtimeConfigService.buildProviderOverrideForPython());
            payload.put("ragOverride", runtimeConfigService.buildRagOverrideForPython());
            String requestBody = objectMapper.writeValueAsString(payload);
            String url = normalizeBaseUrl(externalServiceProperties.getPython().getRag().getBaseUrl()) + "/chat/ask";
            log.info("Call python ask: url={}, sessionId={}, kbId={}, jsonLen={}", url, sessionId, session.getKbId(), requestBody.length());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(180))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> httpResponse = HTTP_CLIENT.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            int statusCode = httpResponse.statusCode();
            String responseBody = httpResponse.body();
            if (statusCode >= 400) {
                errorMessage = "python /chat/ask failed: " + statusCode + " " + responseBody;
                log.warn(errorMessage);
            } else if (responseBody == null || responseBody.isBlank()) {
                errorMessage = "python /chat/ask returned empty response";
                log.warn(errorMessage);
            } else {
                answerPayload = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                pythonCallSuccess = true;
            }
        } catch (HttpTimeoutException ex) {
            errorMessage = "request timed out (python /chat/ask)";
            log.warn("Call python ask timed out: {}", ex.getMessage());
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            log.warn("Call python ask failed: {}", ex.getMessage());
        }

        String answer;
        if (pythonCallSuccess && answerPayload != null) {
            answer = String.valueOf(answerPayload.getOrDefault("answer", ""));
        } else {
            answer = "提问已提交，但模型服务暂时不可用。请检查：知识库是否已建立索引、Python RAG 服务是否可访问、Ollama/在线模型配置是否正确。";
        }
        long latency = System.currentTimeMillis() - startedAt;
        // 写入助手消息（即便失败也写入兜底文案，避免前端一直 loading）。
        Long assistantMessageId = insertMessage(sessionId, "ASSISTANT", answer, nextIndex + 1, 0, latency);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> references = answerPayload == null
            ? List.of()
            : (List<Map<String, Object>>) answerPayload.getOrDefault("references", List.of());
        if (!references.isEmpty()) {
            List<ChatMessageReferenceEntity> refEntities = new ArrayList<>(references.size());
            for (Map<String, Object> reference : references) {
                ChatMessageReferenceEntity ref = new ChatMessageReferenceEntity();
                ref.setMessageId(assistantMessageId);
                ref.setDocumentId(Long.parseLong(String.valueOf(reference.get("documentId"))));
                ref.setChunkId(String.valueOf(reference.get("chunkId")));
                ref.setPageNo(Integer.parseInt(String.valueOf(reference.getOrDefault("pageNo", 1))));
                ref.setSectionTitle(String.valueOf(reference.getOrDefault("sectionTitle", "")));
                ref.setScore(java.math.BigDecimal.valueOf(Double.parseDouble(String.valueOf(reference.getOrDefault("score", 0)))));
                ref.setQuoteText(String.valueOf(reference.getOrDefault("quoteText", "")));
                refEntities.add(ref);
            }
            // 引用溯源：记录本次回答引用的文档片段，支撑前端展示"引用来源"。
            messageReferenceMapper.insertBatch(refEntities);
        }

        Map<String, Object> providerOverride = runtimeConfigService.buildProviderOverrideForPython();
        String actualModelMode = String.valueOf(providerOverride.get("activeMode"));
        String actualModelName = resolveActualModelName(providerOverride);
        // 持久化 RAG 调用日志 (MyBatis-Plus BaseMapper.insert, 自动回填主键)
        RagCallLog ragCallLog = new RagCallLog();
        ragCallLog.setUserId(userId);
        ragCallLog.setSessionId(sessionId);
        ragCallLog.setQueryText(request.getQuestion());
        ragCallLog.setRewrittenQuery(request.getQuestion());
        ragCallLog.setRetrievedChunks(references.size());
        ragCallLog.setModelMode(actualModelMode);
        ragCallLog.setModelName(actualModelName);
        ragCallLog.setPromptTokens(0);
        ragCallLog.setCompletionTokens(0);
        ragCallLog.setLatencyMs(latency);
        ragCallLog.setSuccessFlag(pythonCallSuccess ? 1 : 0);
        ragCallLog.setErrorMessage(pythonCallSuccess ? null : errorMessage);
        ragCallLogMapper.insert(ragCallLog);

        sessionMapper.touch(sessionId);

        operationLogService.log("问答中心", "ASK_QUESTION", "发起问答会话:" + sessionId, pythonCallSuccess ? "SUCCESS" : "FAILED");

        AdminAskResponseVO vo = new AdminAskResponseVO();
        vo.setUserMessageId(userMessageId);
        vo.setAssistantMessageId(assistantMessageId);
        vo.setAnswer(answer);
        vo.setReferences(references);
        return vo;
    }



    /**
     * 发起一次问答（流式）。
     *
     * <p>说明：后端通过 SSE 向前端推送 meta/delta/done 事件；同时在后台线程完成数据库落库与日志记录。
     */
    @Override
    public SseEmitter askStream(Long userId, Long sessionId, ChatAskParam request) {

        SseEmitter emitter = new SseEmitter(600_000L);

        verifySessionOwnership(userId, sessionId);

        ChatSessionEntity session = sessionMapper.selectByOwner(userId, sessionId);
        if (session == null) {
            throw new AssertException(ResultCode.FORBIDDEN, "chat.session.no.permission");
        }
        int nextIndex = messageMapper.getMaxIndex(sessionId) + 1;



        String question = request.getQuestion();

        long startedAt = System.currentTimeMillis();

        // 先落库占位消息：USER + 空 ASSISTANT，后续把 assistant 内容更新为最终答案。

        Long userMessageId = insertMessage(sessionId, "USER", question, nextIndex, 0, 0);

        Long assistantMessageId = insertMessage(sessionId, "ASSISTANT", "", nextIndex + 1, 0, 0);



        try {

            emitter.send(

                SseEmitter.event()

                    .name("meta")

                    .data(

                        Map.of(

                            "type",

                            "meta",

                            "userMessageId",

                            userMessageId,

                            "assistantMessageId",

                            assistantMessageId

                        )

                    )

            );

        } catch (Exception ignored) {

        }



        STREAM_EXECUTOR.submit(() -> {

            boolean pythonCallSuccess = false;

            String errorMessage = null;

            StringBuilder answerBuilder = new StringBuilder();

            List<Map<String, Object>> references = new ArrayList<>();

            boolean doneSent = false;

            try {

                Map<String, Object> providerOverride = runtimeConfigService.buildProviderOverrideForPython();

                Map<String, Object> ragOverride = runtimeConfigService.buildRagOverrideForPython();

                String systemPrompt = null;

                Long agentId = request.getAgentId();

                if (agentId != null) {
                    try {
                        com.jc.rag.domain.entity.rag.RagAgentEntity agent = agentMapper.selectById(agentId);
                        if (agent != null && agent.getStatus() != null && agent.getStatus() == 1) {
                            if (agent.getSystemPrompt() != null) {
                                systemPrompt = agent.getSystemPrompt();
                            }
                            if (agent.getTemperature() != null) {
                                providerOverride.put("temperature", agent.getTemperature().doubleValue());
                            }

                        }

                        if (agent.getMaxTokens() != null) {
                            providerOverride.put("maxTokens", agent.getMaxTokens());
                        }
                        log.info("AskStream: agent loaded, agentId={}, agentKey={}", agentId, agent.getAgentKey());

                    } catch (Exception ex) {

                        log.warn("AskStream: failed to load agent {}: {}", agentId, ex.getMessage());

                    }

                }

                Map<String, Object> payload = new LinkedHashMap<>();

                payload.put("kbId", session.getKbId());

                payload.put("question", question);

                payload.put("topK", runtimeConfigService.resolveRagTopK());

                payload.put("topN", runtimeConfigService.resolveRagTopN());

                payload.put("similarityThreshold", runtimeConfigService.resolveRagSimilarityThreshold());

                payload.put("providerOverride", providerOverride);

                payload.put("ragOverride", ragOverride);

                if (systemPrompt != null) {

                    payload.put("systemPrompt", systemPrompt);

                }

                String requestBody = objectMapper.writeValueAsString(payload);

                String url = normalizeBaseUrl(externalServiceProperties.getPython().getRag().getBaseUrl()) + "/chat/ask/stream";

                log.info("Call python ask stream: url={}, sessionId={}, kbId={}, agentId={}, jsonLen={}", url, sessionId, session.getKbId(), agentId, requestBody.length());



                HttpRequest httpRequest = HttpRequest.newBuilder()

                    .uri(URI.create(url))

                    .timeout(Duration.ofSeconds(600))

                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)

                    .header("Accept", "text/event-stream")

                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))

                    .build();



                HttpResponse<java.io.InputStream> httpResponse = HTTP_CLIENT.send(

                    httpRequest,

                    HttpResponse.BodyHandlers.ofInputStream()

                );



                int statusCode = httpResponse.statusCode();

                if (statusCode >= 400) {

                    errorMessage = "python /chat/ask/stream failed: " + statusCode;

                    log.warn("AskStream: {}", errorMessage);

                } else {

                    long eventCount = 0;

                    long deltaCount = 0;

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.body(), StandardCharsets.UTF_8))) {

                        String line;

                        while ((line = reader.readLine()) != null) {

                            if (line == null || line.isBlank()) {

                                continue;

                            }

                            String trimmedLine = line.trim();

                            if (!trimmedLine.startsWith("data:")) {

                                continue;

                            }

                            String data = trimmedLine.substring("data:".length()).trim();

                            if (data.isBlank()) {

                                continue;

                            }

                            Map<String, Object> event = objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});

                            String type = String.valueOf(event.getOrDefault("type", ""));

                            eventCount++;

                            if ("meta".equals(type)) {

                                @SuppressWarnings("unchecked")

                                List<Map<String, Object>> refs = (List<Map<String, Object>>) event.getOrDefault("references", List.of());

                                references = new ArrayList<>(refs);

                                log.info("AskStream: received meta, references={}", refs.size());

                                emitter.send(SseEmitter.event().name("meta").data(Map.of("type", "meta", "references", references)));

                                continue;

                            }

                            if ("delta".equals(type)) {

                                String delta = String.valueOf(event.getOrDefault("delta", ""));

                                if (!delta.isEmpty()) {

                                    deltaCount++;

                                    answerBuilder.append(delta);

                                    emitter.send(SseEmitter.event().name("delta").data(Map.of("type", "delta", "delta", delta)));

                                }

                                continue;

                            }

                            if ("done".equals(type)) {

                                pythonCallSuccess = true;

                                Object llmUsed = event.get("llmUsed");

                                log.info("AskStream: received done, llmUsed={}, totalEvents={}, totalDeltas={}", llmUsed, eventCount, deltaCount);

                                if (llmUsed == null) {

                                    emitter.send(SseEmitter.event().name("done").data(Map.of("type", "done", "done", true)));

                                } else {

                                    emitter.send(SseEmitter.event().name("done").data(Map.of("type", "done", "done", true, "llmUsed", llmUsed)));

                                }

                                doneSent = true;

                                break;

                            }

                        }

                    }

                    log.info("AskStream: finished reading python stream, eventCount={}, deltaCount={}, doneSent={}", eventCount, deltaCount, doneSent);

                }

            } catch (HttpTimeoutException ex) {

                errorMessage = "request timed out (python /chat/ask/stream)";

                log.warn("Call python ask stream timed out: {}", ex.getMessage());

            } catch (Exception ex) {

                errorMessage = ex.getMessage();

                log.warn("Call python ask stream failed: {}", ex.getMessage());

            }



            String answer = answerBuilder.toString();

            if (!pythonCallSuccess) {

                answer = "提问已提交，但模型服务暂时不可用。请检查：知识库是否已建立索引、Python RAG 服务是否可访问、Ollama/在线模型配置是否正确。";

            } else if (answer.isBlank()) {

                if (references.isEmpty()) {

                    answer = "知识库中未检索到足够依据，请补充更准确的问题或先上传相关文档。";

                } else {

                    StringBuilder fallback = new StringBuilder();

                    fallback.append("模型返回空响应，以下是检索到的相关知识片段：\n\n");

                    for (int i = 0; i < references.size(); i++) {

                        Object quoteText = references.get(i).get("quoteText");

                        if (quoteText == null) {

                            continue;

                        }

                        String text = String.valueOf(quoteText).trim();

                        if (text.isEmpty()) {

                            continue;

                        }

                        fallback.append(text);

                        if (i < references.size() - 1) {

                            fallback.append("\n\n");

                        }

                    }

                    answer = fallback.toString().trim();

                }

            }



            long latency = System.currentTimeMillis() - startedAt;
            try {
                ChatMessageEntity assistantEntity = messageMapper.selectById(assistantMessageId);
                if (assistantEntity != null) {
                    assistantEntity.setMessageContent(answer);
                    assistantEntity.setLatencyMs(latency);
                    messageMapper.updateById(assistantEntity);
                }

                if (!references.isEmpty()) {
                    List<ChatMessageReferenceEntity> refEntities = new ArrayList<>(references.size());
                    for (Map<String, Object> reference : references) {
                        ChatMessageReferenceEntity ref = new ChatMessageReferenceEntity();
                        ref.setMessageId(assistantMessageId);
                        ref.setDocumentId(Long.parseLong(String.valueOf(reference.get("documentId"))));
                        ref.setChunkId(String.valueOf(reference.get("chunkId")));
                        ref.setPageNo(Integer.parseInt(String.valueOf(reference.getOrDefault("pageNo", 1))));
                        ref.setSectionTitle(String.valueOf(reference.getOrDefault("sectionTitle", "")));
                        ref.setScore(java.math.BigDecimal.valueOf(Double.parseDouble(String.valueOf(reference.getOrDefault("score", 0)))));
                        ref.setQuoteText(String.valueOf(reference.getOrDefault("quoteText", "")));
                        refEntities.add(ref);
                    }
                    // 引用溯源：记录本次回答引用的文档片段，支撑前端展示"引用来源"。
                    messageReferenceMapper.insertBatch(refEntities);
                }

                // 持久化 RAG 调用日志 (MyBatis-Plus BaseMapper.insert, 自动回填主键)
                RagCallLog ragCallLog = new RagCallLog();
                ragCallLog.setUserId(userId);
                ragCallLog.setSessionId(sessionId);
                ragCallLog.setQueryText(question);
                ragCallLog.setRewrittenQuery(question);
                ragCallLog.setRetrievedChunks(references.size());
                ragCallLog.setModelMode(session.getModelMode());
                ragCallLog.setModelName(session.getModelName());
                ragCallLog.setPromptTokens(0);
                ragCallLog.setCompletionTokens(0);
                ragCallLog.setLatencyMs(latency);
                ragCallLog.setSuccessFlag(pythonCallSuccess ? 1 : 0);
                ragCallLog.setErrorMessage(pythonCallSuccess ? null : errorMessage);
                ragCallLogMapper.insert(ragCallLog);
                sessionMapper.touch(sessionId);

                operationLogService.log("问答中心", "ASK_QUESTION_STREAM", "发起问答会话:" + sessionId, pythonCallSuccess ? "SUCCESS" : "FAILED");

            } catch (Exception ex) {

                log.warn("Finalize ask stream failed: {}", ex.getMessage());

            } finally {

                if (!doneSent) {

                    // 兜底：若 Python 未发送 done，则由后端补发，避免前端一直处于 loading 状态。

                    try {

                        emitter.send(SseEmitter.event().name("done").data(Map.of("type", "done", "done", true, "llmUsed", pythonCallSuccess)));

                    } catch (Exception ignored) {

                    }

                }

                emitter.complete();

            }

        });



        return emitter;

    }



    /**

     * 插入一条消息到 chat_message 表。

     */

    private Long insertMessage(
        Long sessionId,
        String roleType,
        String content,
        int messageIndex,
        int tokenCount,
        long latencyMs
    ) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setRoleType(roleType);
        entity.setMessageContent(content);
        entity.setMessageIndex(messageIndex);
        entity.setTokenCount(tokenCount);
        entity.setLatencyMs(latencyMs);
        messageMapper.insert(entity);
        return entity.getId();
    }



    /**
     * 删除当前用户自己的会话，避免不同账号互相看到历史记录。
     */
    @Override
    @Transactional
    public BaseOpResultVO deleteSession(Long userId, Long sessionId) {
        verifySessionOwnership(userId, sessionId);
        sessionMapper.close(sessionId);
        BaseOpResultVO vo = new BaseOpResultVO();
        vo.setSuccess(Boolean.TRUE);
        vo.setSessionId(sessionId);
        return vo;
    }

    private void verifySessionOwnership(Long userId, Long sessionId) {
        Integer count = sessionMapper.countOwned(userId, sessionId);
        if (count == null || count == 0) {
            throw new AssertException(ResultCode.FORBIDDEN, "chat.session.no.permission");
        }
    }



    /**
     * 查询会话详情（创建会话后回显使用）。
     */
    private AdminChatSessionVO getSession(Long sessionId) {
        ChatSessionEntity e = sessionMapper.selectById(sessionId);
        if (e == null) {
            return null;
        }
        AdminChatSessionVO vo = new AdminChatSessionVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setSessionTitle(e.getSessionTitle());
        vo.setSessionMode(e.getSessionMode());
        vo.setModelMode(e.getModelMode());
        vo.setModelName(e.getModelName());
        vo.setKbId(e.getKbId());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }



    /**

     * 规范化外部服务 baseUrl，避免末尾斜杠导致 URL 拼接错误。

     */

    private String normalizeBaseUrl(String baseUrl) {

        if (baseUrl == null) {

            return "";

        }

        String trimmed = baseUrl.trim();

        if (trimmed.endsWith("/")) {

            return trimmed.substring(0, trimmed.length() - 1);

        }

        return trimmed;

    }



    @SuppressWarnings("unchecked")
    private String resolveActualModelName(Map<String, Object> providerOverride) {
        String mode = String.valueOf(providerOverride.get("activeMode"));
        if ("online".equals(mode)) {
            Map<String, Object> online = (Map<String, Object>) providerOverride.get("online");
            return String.valueOf(online.get("modelName"));
        }
        Map<String, Object> offline = (Map<String, Object>) providerOverride.get("offline");
        Map<String, Object> ollama = (Map<String, Object>) offline.get("ollama");
        return String.valueOf(ollama.get("modelName"));
    }

    /** Map 中的数值型字段安全转 Long, 兼容 Integer / Long / String / 数字. */
    private static Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Map 中的数值型字段安全转 Integer, 兼容 Integer / Long / String / 数字. */
    private static Integer asInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 时间字段统一转 LocalDateTime, 支持 Timestamp / LocalDateTime / String 格式. */
    private static java.time.LocalDateTime asDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.time.LocalDateTime dt) return dt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof java.util.Date d) return d.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) return null;
        try {
            return java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ex) {
            return null;
        }
    }
}