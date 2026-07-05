package com.jc.rag.user.security;

import com.jc.rag.common.security.CoreUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * developer 鐢ㄦ埛璺宠繃鎵€鏈夐壌鏉冪殑杩囨护鍣ㄣ€? * 妫€娴嬪埌褰撳墠鐢ㄦ埛涓?DEVELOPER 绫诲瀷鏃讹紝鐩存帴鏀捐鎵€鏈夎姹傘€? */
public class DeveloperBypassFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CoreUserDetails details && details.isBypassAuth()) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(request, response);
    }
}

