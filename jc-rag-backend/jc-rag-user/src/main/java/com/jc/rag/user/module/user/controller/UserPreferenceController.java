package com.jc.rag.user.module.user.controller;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.entity.chat.UserMemoryProfileEntity;
import com.jc.rag.user.module.user.model.param.MemoryFileSaveParam;
import com.jc.rag.user.module.user.model.param.UserPreferenceUpdateParam;
import com.jc.rag.user.module.user.model.vo.MemoryFileVO;
import com.jc.rag.user.module.user.model.vo.UserPreferenceVO;
import com.jc.rag.user.module.user.service.UserMemoryProfileService;
import com.jc.rag.user.module.user.service.UserPreferenceService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前用户偏好设置 Controller (C 端, 齿轮 - 偏好设置弹窗)。
 * <p>路径: /api/user/preference
 * <p>第一阶段实现 3 项: memoryFilePath / defaultModelCode / chatStyle
 * <p>记忆文件管理端点: {@code /api/user/preference/memory-file} (GET 读 / PUT 写)
 * <p>L2 长期记忆 (我的记忆页): {@code /api/user/preference/memory/list|delete|clear}, 转发到 jc-rag-service (Python).
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/user/preference")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final UserMemoryProfileService userMemoryProfileService;
    private final ObjectMapper objectMapper;

    @Value("${service.python.rag.base-url:http://localhost:8001}")
    private String ragBaseUrl;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).version(HttpClient.Version.HTTP_1_1).build();

    @GetMapping
    public R<UserPreferenceVO> get(Authentication auth) {
        return R.ok(userPreferenceService.load(currentUserId(auth)));
    }

    @PutMapping
    public R<UserPreferenceVO> update(@Valid @RequestBody UserPreferenceUpdateParam param, Authentication auth) {
        return R.ok(userPreferenceService.update(currentUserId(auth), param));
    }

    /**
     * 读取记忆文件 (Markdown).
     * <p>文件不存在时, 后端会自动初始化默认模板并返回 ({@code autoInitialized=true}).
     */
    @GetMapping("/memory-file")
    public R<MemoryFileVO> getMemoryFile(Authentication auth) {
        return R.ok(userPreferenceService.readMemoryFile(currentUserId(auth)));
    }

    /**
     * 保存记忆文件 (Markdown).
     * <p>写入磁盘真实路径 (路径来自当前用户偏好). 后端会自动创建父目录.
     */
    @PutMapping("/memory-file")
    public R<MemoryFileVO> saveMemoryFile(@Valid @RequestBody MemoryFileSaveParam param, Authentication auth) {
        return R.ok(userPreferenceService.writeMemoryFile(currentUserId(auth), param));
    }

    /* ----------- L2 长期记忆 (我的记忆页) ----------- */

    /**
     * 列本用户所有 active 长期记忆. 转发到 Python /chat/memory/list.
     */
    @GetMapping("/memory/list")
    public R<Map<String, Object>> listMemory(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "50") int size,
        Authentication auth
    ) {
        Long userId = currentUserId(auth);
        try {
            Map<String, Object> query = new LinkedHashMap<>();
            query.put("userId", String.valueOf(userId));
            query.put("page", page);
            query.put("size", size);
            String url = ragBaseUrl + "/chat/memory/list?userId=" + userId + "&page=" + page + "&size=" + size;
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                log.warn("listMemory Python status={} body={}", resp.statusCode(), resp.body());
                return R.ok(Map.of("status", "FAILED", "total", 0, "items", java.util.List.of(), "page", page, "size", size));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(resp.body(), Map.class);
            return R.ok(result);
        } catch (Exception e) {
            log.warn("listMemory failed: userId={}, err={}", userId, e.getMessage());
            return R.ok(Map.of("status", "FAILED", "message", e.getMessage(), "total", 0, "items", java.util.List.of(), "page", page, "size", size));
        }
    }

    /**
     * 软删单条记忆. 转发到 Python /chat/memory/delete.
     */
    @PostMapping("/memory/delete")
    public R<Map<String, Object>> deleteMemory(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = currentUserId(auth);
        String memoryId = body == null ? null : String.valueOf(body.getOrDefault("memoryId", "")).trim();
        if (memoryId == null || memoryId.isEmpty() || "null".equals(memoryId)) {
            return R.ok(Map.of("status", "FAILED", "message", "memoryId 必填"));
        }
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("userId", String.valueOf(userId));
            req.put("memoryId", memoryId);
            String bodyStr = objectMapper.writeValueAsString(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(ragBaseUrl + "/chat/memory/delete"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = HTTP_CLIENT.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                return R.ok(Map.of("status", "FAILED", "message", "Python status " + resp.statusCode()));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(resp.body(), Map.class);
            return R.ok(result);
        } catch (Exception e) {
            log.warn("deleteMemory failed: userId={}, err={}", userId, e.getMessage());
            return R.ok(Map.of("status", "FAILED", "message", e.getMessage()));
        }
    }

    /**
     * 清空本用户所有 active 长期记忆. 转发到 Python /chat/memory/clear.
     */
    @PostMapping("/memory/clear")
    public R<Map<String, Object>> clearMemory(@RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        Long userId = currentUserId(auth);
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("userId", String.valueOf(userId));
            String bodyStr = objectMapper.writeValueAsString(req);
            HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(ragBaseUrl + "/chat/memory/clear"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = HTTP_CLIENT.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                return R.ok(Map.of("status", "FAILED", "message", "Python status " + resp.statusCode()));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(resp.body(), Map.class);
            return R.ok(result);
        } catch (Exception e) {
            log.warn("clearMemory failed: userId={}, err={}", userId, e.getMessage());
            return R.ok(Map.of("status", "FAILED", "message", e.getMessage()));
        }
    }

    /* ----------- L3 跨会话画像 (我的记忆页) ----------- */

    /**
     * 取本用户 L3 画像 (无画像返回 null).
     */
    @GetMapping("/memory/profile")
    public R<Map<String, Object>> getProfile(Authentication auth) {
        Long userId = currentUserId(auth);
        UserMemoryProfileEntity p = userMemoryProfileService.getProfile(userId);
        if (p == null) {
            return R.ok(Map.of("exists", false, "profile", (Object) null));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("exists", true);
        body.put("version", p.getVersion());
        body.put("factCount", p.getFactCount());
        body.put("modelName", p.getModelName());
        body.put("lastExtractedAt", p.getLastExtractedAt() == null ? null : p.getLastExtractedAt().toString());
        body.put("updatedAt", p.getUpdatedAt() == null ? null : p.getUpdatedAt().toString());
        Object parsed;
        try {
            parsed = objectMapper.readValue(p.getProfileJson(), new TypeReference<Object>() {});
        } catch (Exception ex) {
            log.warn("getProfile: invalid profile_json for userId={}, err={}", userId, ex.getMessage());
            parsed = null;
        }
        body.put("profile", parsed);
        return R.ok(body);
    }

    /**
     * 重置 (清空) 本用户 L3 画像: 直接物理删除 user_memory_profile 行.
     * <p>下一次 maybeExtractProfile 触发时会重建.
     */
    @DeleteMapping("/memory/profile")
    public R<Map<String, Object>> deleteProfile(Authentication auth) {
        Long userId = currentUserId(auth);
        try {
            int n = userMemoryProfileService.resetProfile(userId);
            log.info("deleteProfile: userId={}, removed={}", userId, n);
            return R.ok(Map.of("status", "SUCCESS", "removed", n));
        } catch (Exception e) {
            log.warn("deleteProfile failed: userId={}, err={}", userId, e.getMessage());
            return R.ok(Map.of("status", "FAILED", "message", e.getMessage()));
        }
    }

    /* ----------- 被遗忘权 (GDPR / 个保法) ----------- */

    /**
     * 一键忘记我: 物理删除本用户全部对话数据 (L0+L1+L2+L3 + 偏好 + 记忆文件).
     * <p>账号本身保留, 用户可重新登录, 只是历史数据全清.
     * <p>本端点不可逆, 前端需做"输用户名 + 二次确认"才能调.
     */
    @PostMapping("/forget-me")
    public R<Map<String, Object>> forgetMe(Authentication auth) {
        Long userId = currentUserId(auth);
        Map<String, Object> result = userPreferenceService.forgetMe(userId);
        return R.ok(result);
    }

    private static Long currentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CoreUserDetails cu)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return cu.getUserId();
    }
}
