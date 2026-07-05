package com.jc.rag.user.config;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * SPA 前端路由转发配置。
 * Vue Router history 模式下，浏览器刷新子路由会 404，
 * 通过 ErrorViewResolver 将非 API 的 404 请求转发到 index.html。
 * <p>显式 bean name 避免与 admin 端同名 Configuration 冲突（Pro Edition fat-jar 同时打包 admin+user 时）.
 * @author JC
 */
@Configuration("userSpaWebMvcConfig")
public class SpaWebMvcConfig {

    @Bean("userSpaErrorViewResolver")
    public ErrorViewResolver spaErrorViewResolver() {
        return (request, status, model) -> {
            // 仅对非 API 路径的 404 做转发，API 路径保持原始 404 响应
            if (HttpStatus.NOT_FOUND.equals(status)) {
                String path = request.getRequestURI();
                if (!path.startsWith("/api/") && !path.startsWith("/actuator/")) {
                    return new ModelAndView("forward:/index.html", Map.of());
                }
            }
            return null;
        };
    }
}
