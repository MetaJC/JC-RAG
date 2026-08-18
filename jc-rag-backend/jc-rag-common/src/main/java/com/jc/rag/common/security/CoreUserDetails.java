package com.jc.rag.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.List;

/**
 * 核心用户认证对象，通用模块提供。
 * developer 用户将跳过所有鉴权检查。
 */
public class CoreUserDetails implements RagPrincipal {
    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final String nickname;
    private final String userType;  // DEVELOPER, ADMIN, NORMAL
    private final boolean bypassAuth; // developer 专用：跳过所有鉴权

    public CoreUserDetails(Long userId, String username, String passwordHash, String nickname, String userType, boolean bypassAuth) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.userType = userType;
        this.bypassAuth = bypassAuth;
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getUserType() { return userType; }
    public boolean isBypassAuth() { return bypassAuth; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (bypassAuth) return List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER"), new SimpleGrantedAuthority("BYPASS_AUTH"));
        if ("ADMIN".equals(userType)) return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ADMIN"));
        return List.of(new SimpleGrantedAuthority("ROLE_" + userType));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
