package com.jc.rag.admin.module.auth.controller;

import java.io.Serializable;

/**
 * Captcha check 请求参数 (admin 端)。
 * <p>前端拼图坐标+耗时提交到后端校验。
 *
 * @author JC
 */
public class CaptchaCheckRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String challengeId;
    private Long durationMs;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}
