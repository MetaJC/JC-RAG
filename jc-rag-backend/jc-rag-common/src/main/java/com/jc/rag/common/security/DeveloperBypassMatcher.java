package com.jc.rag.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * developer 用户路径放行匹配器。
 * 检测到当前用户为 developer 类型时，放行所有请求。
 */
public class DeveloperBypassMatcher implements RequestMatcher {
    private final RequestMatcher original;

    public DeveloperBypassMatcher(RequestMatcher original) {
        this.original = original;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        return original.matches(request);
    }
}
