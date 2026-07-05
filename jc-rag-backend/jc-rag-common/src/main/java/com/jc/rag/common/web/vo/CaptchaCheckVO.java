package com.jc.rag.common.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 滑动验证码 check 响应 VO (admin/user 通用)。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaptchaCheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean success;
    private String token;
    private String message;
    private String captchaToken;
    private Long expireInMs;

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
    public Long getExpireInMs() { return expireInMs; }
    public void setExpireInMs(Long expireInMs) { this.expireInMs = expireInMs; }
}
