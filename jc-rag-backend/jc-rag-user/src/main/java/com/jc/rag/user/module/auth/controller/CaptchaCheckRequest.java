package com.jc.rag.user.module.auth.controller;

import java.io.Serializable;

/**
 * Captcha check 请求参数 (user 端)。
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
