package com.jc.rag.admin.security.rbac;

import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collection;

/**
 * permCode 路由级鉴权拦截器。
 * <p>拦截 /api/** Controller 请求，按 (pattern, method) 查 PermissionRouteLoader 找到所需 permCode，
 * 再校验当前用户 authorities 中是否含该 permCode。
 *
 * <h4>执行顺序（与白名单）</h4>
 * <ol>
 *   <li>非 Controller 路径（静态资源 404 / actuator）→ 放行</li>
 *   <li>白名单路径（auth/login/health）→ 放行</li>
 *   <li>未登录 / 匿名 → 抛 AccessDeniedException → GlobalExceptionHandler → 401</li>
 *   <li>developer（BYPASS_AUTH authority）→ 放行（仅 developer 跳过；superAdmin 不跳，靠全 permCode 绑定自然通过）</li>
 *   <li>未在路由表注册（loader 0 条 或 路径无 API 类型 perm）→ 放行（保守，依赖 SecurityFilterChain 粗粒度）</li>
 *   <li>用户 authorities 含 required permCode → 放行</li>
 *   <li>否则 → 抛 AccessDeniedException → 403</li>
 * </ol>
 *
 * <h4>superAdmin 不放行说明</h4>
 * <p>superAdmin 不在白名单中，必须走 permCode 校验。super_admin 角色通过
 * {@code SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.role_code='super_admin'}
 * 绑定全部 permCode，因此登录后 authorities 含所有 permCode，校验自然通过。
 * <p>这样做的好处：superAdmin 行为可审计；如需临时收权，解除 super_admin 全量绑定即可。
 *
 * <h4>pattern 来源</h4>
 * <p>优先取 Spring 解析后的 {@code BEST_MATCHING_PATTERN_ATTRIBUTE}（如 /api/sys/user/{id}），
 * 与 PermissionRouteLoader 缓存的 path 对齐；解析失败时退化到 URI 自身。
 *
 * @author JC
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionRouteLoader routeLoader;
    private final RbacDefaultsProperties rbacDefaults;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1) 非 Controller 方法（静态资源转发 / actuator 错误页等）放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 2) 白名单
        String uri = request.getRequestURI();
        if (isWhitelisted(uri)) {
            return true;
        }

        // 3) 登录态
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("未登录或会话已过期");
        }

        // 4) developer 跳过（最高权限，配置开关可关；superAdmin 不跳，靠 permCode 绑定放行）
        if (rbacDefaults.isDeveloperSkipAuth() && hasAuthority(auth, "BYPASS_AUTH")) {
            return true;
        }

        // 5) 解析 pattern（带占位符）
        String pattern = resolvePattern(request);
        String method = request.getMethod();

        // 6) 查 permCode：未注册 = 不需细粒度鉴权
        if (routeLoader.size() == 0) {
            return true; // 路由表未启用（4.1 阶段之前）
        }
        String required = routeLoader.findRequiredPerm(pattern, method);
        if (required == null) {
            return true;
        }

        // 7) permCode 校验（含 superAdmin：其角色绑定了全部 permCode，因此自然通过）
        if (!hasAuthority(auth, required)) {
            log.warn("[鉴权拦截] uri={} method={} required={} user={}",
                uri, method, required, auth.getName());
            throw new AccessDeniedException("无访问权限: " + required);
        }
        return true;
    }

    /** Spring 解析后的 pattern（带占位符），解析失败时退化到 URI 自身 */
    private String resolvePattern(HttpServletRequest request) {
        Object best = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return best != null ? best.toString() : request.getRequestURI();
    }

    private boolean isWhitelisted(String uri) {
        if (uri == null) return true;
        return uri.startsWith("/api/auth/")
            || uri.startsWith("/api/health/")
            || uri.startsWith("/actuator/")
            || uri.startsWith("/internal/")
            || uri.equals("/")
            || uri.equals("/index.html")
            || uri.startsWith("/assets/")
            || uri.equals("/favicon.ico")
            || uri.endsWith(".js")
            || uri.endsWith(".css")
            || uri.endsWith(".png")
            || uri.endsWith(".svg")
            || uri.endsWith(".ico")
            || uri.endsWith(".woff2");
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) return false;
        for (GrantedAuthority a : authorities) {
            if (authority.equalsIgnoreCase(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
