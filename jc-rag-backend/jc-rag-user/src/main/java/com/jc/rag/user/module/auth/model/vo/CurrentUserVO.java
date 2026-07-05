package com.jc.rag.user.module.auth.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 当前登录用户信息 VO。
 * <p>未登录时 authenticated=false, 仅返回该字段。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentUserVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean authenticated;
    private Long userId;
    private String username;
    private String nickname;
    private String userType;
    private Boolean bypassAuth;

    public static CurrentUserVO anonymous() {
        CurrentUserVO vo = new CurrentUserVO();
        vo.setAuthenticated(Boolean.FALSE);
        return vo;
    }

    public static CurrentUserVO of(Long userId, String username, String nickname, String userType, boolean bypassAuth) {
        CurrentUserVO vo = new CurrentUserVO();
        vo.setAuthenticated(Boolean.TRUE);
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setNickname(nickname);
        vo.setUserType(userType);
        vo.setBypassAuth(bypassAuth);
        return vo;
    }

    public Boolean getAuthenticated() { return authenticated; }
    public void setAuthenticated(Boolean authenticated) { this.authenticated = authenticated; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Boolean getBypassAuth() { return bypassAuth; }
    public void setBypassAuth(Boolean bypassAuth) { this.bypassAuth = bypassAuth; }
}
