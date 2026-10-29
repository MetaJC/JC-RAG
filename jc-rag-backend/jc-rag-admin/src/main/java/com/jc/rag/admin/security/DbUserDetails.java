/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.admin.security;

import com.jc.rag.common.security.RagPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 业务用户身份信息。
 * <p>authorities = [userType, ROLE_userType, ...permCodes]；
 * developer 用户额外追加 ADMIN / BYPASS_AUTH，用于 SecurityFilterChain 路由级粗粒度放行。
 * <p>登录时由 {@link DbUserDetailsService} 一次性加载 user → role → permission 全部 permCode，
 * 运行时由 {@code PermissionInterceptor} 做 permCode 级别的细粒度鉴权。
 *
 * @author JC
 */
public class DbUserDetails implements RagPrincipal {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final String nickname;
    private final String userType;
    private final List<String> permCodes;

    public DbUserDetails(Long userId, String username, String passwordHash,
                         String nickname, String userType, List<String> permCodes) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.userType = userType;
        this.permCodes = permCodes == null ? Collections.emptyList() : permCodes;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getUserType() {
        return userType;
    }

    /** 当前用户拥有的 permCode 集合（已去重） */
    public List<String> getPermCodes() {
        return permCodes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // 1) 业务身份（userType 自身 + ROLE_ 前缀）
        authorities.add(new SimpleGrantedAuthority(userType));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + userType));
        // 2) developer 兜底：SecurityConfig.hasAnyAuthority 走 ADMIN 通道；BYPASS_AUTH 供拦截器识别跳过
        if ("DEVELOPER".equalsIgnoreCase(userType)) {
            authorities.add(new SimpleGrantedAuthority("ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("BYPASS_AUTH"));
        }
        // 3) 业务 permCode（运行时细粒度鉴权）
        for (String code : permCodes) {
            if (code != null && !code.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority(code));
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
