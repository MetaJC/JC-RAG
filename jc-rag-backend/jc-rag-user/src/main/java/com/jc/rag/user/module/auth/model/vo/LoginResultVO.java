package com.jc.rag.user.module.auth.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 登录结果 VO。
 * <p>success=true 时返回 user 信息; success=false 时返回 code/message/failCount/needCaptcha。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean success;
    private Long userId;
    private String username;
    private String nickname;
    private String userType;
    private String code;
    private String message;
    private Integer failCount;
    private Boolean needCaptcha;

    public static LoginResultVO ok(Long userId, String username, String nickname, String userType) {
        LoginResultVO vo = new LoginResultVO();
        vo.setSuccess(Boolean.TRUE);
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setUserType(userType);
        vo.setNeedCaptcha(Boolean.FALSE);
        vo.setFailCount(0);
        return vo;
    }

    public static LoginResultVO fail(String code, String message, int failCount) {
        LoginResultVO vo = new LoginResultVO();
        vo.setSuccess(Boolean.FALSE);
        vo.setCode(code);
        vo.setMessage(message);
        vo.setFailCount(failCount);
        vo.setNeedCaptcha(failCount >= 3);
        return vo;
    }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
    public Boolean getNeedCaptcha() { return needCaptcha; }
    public void setNeedCaptcha(Boolean needCaptcha) { this.needCaptcha = needCaptcha; }
}
