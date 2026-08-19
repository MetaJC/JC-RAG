package com.jc.rag.common.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 滑动验证码服务 (共享给 admin / user 端)
 *
 * 两段式:
 *  1. issueChallenge() : 生成 challengeId (2 分钟过期)
 *  2. verifyAndIssueToken(challengeId, durationMs) : 校验行为轨迹, 颁发 captchaToken (5 分钟, 一次性消费)
 *  3. consumeToken(token) : 登录时消费 token
 *
 * @author JC
 */
@Component
public class CaptchaService {

    private static final SecureRandom RNG = new SecureRandom();

    private static final long CHALLENGE_TTL_MS = 2 * 60 * 1000L;
    private static final long TOKEN_TTL_MS = 5 * 60 * 1000L;

    private static final long MIN_DURATION_MS = 200L;
    private static final long MAX_DURATION_MS = 30_000L;

    /** challengeId → 颁发时间 */
    private final Map<String, Long> challenges = new ConcurrentHashMap<>();
    /** captchaToken → 过期时间 */
    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    /**
     * 颁发挑战
     */
    public String issueChallenge() {
        String id = UUID.randomUUID().toString().replace("-", "");
        challenges.put(id, System.currentTimeMillis() + CHALLENGE_TTL_MS);
        cleanup();
        return id;
    }

    /**
     * 校验行为轨迹并颁发 token
     *
     * @param challengeId  挑战 id
     * @param durationMs   拖拽用时 (毫秒)
     * @return [success, message, token]
     */
    public VerifyResult verifyAndIssueToken(String challengeId, long durationMs) {
        if (challengeId == null || challengeId.isEmpty()) {
            return VerifyResult.fail("参数缺失");
        }
        Long expire = challenges.get(challengeId);
        if (expire == null) {
            return VerifyResult.fail("挑战已失效, 请刷新验证码");
        }
        if (System.currentTimeMillis() > expire) {
            challenges.remove(challengeId);
            return VerifyResult.fail("挑战已过期");
        }
        if (durationMs < MIN_DURATION_MS) {
            return VerifyResult.fail("操作过快, 疑似机器人");
        }
        if (durationMs > MAX_DURATION_MS) {
            return VerifyResult.fail("操作超时, 请重试");
        }

        // 校验通过后再消费 challengeId, 失败时允许重试
        challenges.remove(challengeId);
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, System.currentTimeMillis() + TOKEN_TTL_MS);
        return VerifyResult.ok(token, TOKEN_TTL_MS);
    }

    /**
     * 消费 token (一次性)
     */
    public boolean consumeToken(String token) {
        if (token == null || token.isEmpty()) return false;
        Long expire = tokens.remove(token);
        if (expire == null) return false;
        return System.currentTimeMillis() <= expire;
    }

    /**
     * 校验 token 是否有效 (不消费).
     * <p>用于"先做业务预检 (如短信限流), 通过后再 consumeToken"的场景,
     * 避免预检失败时白白烧掉一次性 token.
     */
    public boolean peekToken(String token) {
        if (token == null || token.isEmpty()) return false;
        Long expire = tokens.get(token);
        if (expire == null) return false;
        return System.currentTimeMillis() <= expire;
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        challenges.entrySet().removeIf(e -> now > e.getValue());
        tokens.entrySet().removeIf(e -> now > e.getValue());
    }

    /** 校验结果 */
    public static class VerifyResult {
        public final boolean success;
        public final String message;
        public final String token;
        public final long expireInMs;

        private VerifyResult(boolean success, String message, String token, long expireInMs) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.expireInMs = expireInMs;
        }
        static VerifyResult ok(String token, long expireInMs) {
            return new VerifyResult(true, "ok", token, expireInMs);
        }
        static VerifyResult fail(String message) {
            return new VerifyResult(false, message, null, 0L);
        }
    }
}
