package com.jc.rag.admin.common.config;

import com.jc.rag.admin.security.rbac.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册 permCode 鉴权拦截器。
 * <p>拦截 /api/** Controller 请求；以下路径在拦截器内自行放行（白名单）。
 * <p>注：SecurityFilterChain 在 Filter 层先做认证 + 粗粒度 hasAnyAuthority 鉴权，
 * 本拦截器在 DispatcherServlet 层做 permCode 级别细粒度鉴权，二者互不冲突。
 *
 * @author JC
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
            .addPathPatterns("/api/**");
    }
}
