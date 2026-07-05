package com.jc.rag.user.module.feature.controller;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.feature.EvaluationStatusVO;
import com.jc.rag.common.feature.ProEvaluationResult;
import com.jc.rag.common.feature.ProEvaluationService;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.entity.chat.UserDialogueEvaluationEntity;
import com.jc.rag.domain.mapper.chat.UserDialogueEvaluationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话评估 - C 端公开入口.
 * <p>路径: /api/evaluation/**</p>
 * <p>用户端可查询状态、提交评估、查看历史.</p>
 * <p>真实评估由 {@link ProEvaluationService} 实现, 社区版返回占位结果 + 提示升级.</p>
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/evaluation")
@RequiredArgsConstructor
public class PublicEvaluationController {

    private final ProEvaluationService proEvaluationService;
    private final UserDialogueEvaluationMapper evaluationMapper;

    /**
     * 查询功能状态 (前端按钮显示依据).
     */
    @GetMapping("/status")
    public R<EvaluationStatusVO> status(Authentication auth) {
        Long userId = currentUserId(auth);
        int quota = proEvaluationService.getDailyQuota();
        int used = proEvaluationService.getUsedToday(userId);
        boolean enabled = proEvaluationService.isEnabled();
        boolean pro = proEvaluationService.isProEnhanced();

        String tip;
        if (!enabled) {
            tip = "该功能已被管理员关闭";
        } else if (quota == 0) {
            tip = pro ? "今日配额已用完" : "该功能为付费版专属, 升级到 JC RAG Pro 解锁无限评估";
        } else if (used >= quota) {
            tip = "今日配额已用完 (" + used + "/" + quota + ")";
        } else {
            tip = "今日剩余 " + (quota - used) + " 次 (共 " + quota + " 次)";
        }

        return R.ok(EvaluationStatusVO.builder()
            .enabled(enabled)
            .dailyQuota(quota)
            .usedToday(used)
            .remaining(Math.max(0, quota - used))
            .proEnhanced(pro)
            .tip(tip)
            .build());
    }

    /**
     * 触发一次对话评估.
     * <p>社区版: 总是返回占位结果, 即便管理员改了 config (因社区版 Bean 强制 quota=0).</p>
     * <p>付费版: 真实调用 LLM 评估, 写入 user_dialogue_evaluation.</p>
     */
    @PostMapping("/evaluate")
    public R<ProEvaluationResult> evaluate(@RequestParam Long sessionId, Authentication auth) {
        Long userId = currentUserId(auth);

        // 配额检查
        int quota = proEvaluationService.getDailyQuota();
        int used = proEvaluationService.getUsedToday(userId);
        if (quota == 0) {
            return R.ok(proEvaluationService.evaluate(userId, sessionId));
        }
        if (used >= quota) {
            throw new AssertException(ResultCode.QUOTA_EXCEEDED, "evaluation.quota.exhausted");
        }

        ProEvaluationResult result = proEvaluationService.evaluate(userId, sessionId);
        // 记录到 user_dialogue_evaluation
        try {
            UserDialogueEvaluationEntity e = new UserDialogueEvaluationEntity();
            e.setUserId(userId);
            e.setSessionId(sessionId);
            e.setScore(result.getScore() == null ? 0 : result.getScore());
            e.setComment(result.getComment());
            e.setStrengths(toJsonArray(result.getStrengths()));
            e.setImprovements(toJsonArray(result.getImprovements()));
            e.setModelName(result.getModelName());
            e.setProEnhanced(result.isProEnhanced() ? 1 : 0);
            e.setCreatedAt(java.time.LocalDateTime.now());
            evaluationMapper.insert(e);
        } catch (Exception ex) {
            log.warn("[Evaluation] save record failed: userId={}, sessionId={}, err={}", userId, sessionId, ex.getMessage());
        }
        return R.ok(result);
    }

    /**
     * 查询本用户评估历史 (最近 N 条).
     */
    @GetMapping("/history")
    public R<List<ProEvaluationResult>> history(@RequestParam(defaultValue = "20") Integer limit, Authentication auth) {
        Long userId = currentUserId(auth);
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        List<UserDialogueEvaluationEntity> records = evaluationMapper.selectByUserId(userId, safeLimit);
        if (records == null) records = Collections.emptyList();
        List<ProEvaluationResult> vos = records.stream().map(this::toVO).collect(Collectors.toList());
        return R.ok(vos);
    }

    private ProEvaluationResult toVO(UserDialogueEvaluationEntity e) {
        return ProEvaluationResult.builder()
            .score(e.getScore())
            .comment(e.getComment())
            .strengths(fromJsonArray(e.getStrengths()))
            .improvements(fromJsonArray(e.getImprovements()))
            .modelName(e.getModelName())
            .proEnhanced(e.getProEnhanced() != null && e.getProEnhanced() == 1)
            .build();
    }

    private String toJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(items.get(i).replace("\"", "\\\"")).append("\"");
        }
        return sb.append("]").toString();
    }

    private List<String> fromJsonArray(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) return Collections.emptyList();
        try {
            // 简单解析: 去掉 [ ] 和 ", 处理基本转义
            String inner = json.trim();
            if (inner.startsWith("[")) inner = inner.substring(1);
            if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
            if (inner.isBlank()) return Collections.emptyList();
            String[] parts = inner.split(",");
            java.util.List<String> out = new java.util.ArrayList<>();
            for (String p : parts) {
                String s = p.trim();
                if (s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
                s = s.replace("\\\"", "\"").replace("\\\\", "\\");
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private static Long currentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CoreUserDetails cu)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return cu.getUserId();
    }
}
