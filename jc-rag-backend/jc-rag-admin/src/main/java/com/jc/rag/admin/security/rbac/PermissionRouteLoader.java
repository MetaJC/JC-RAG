package com.jc.rag.admin.security.rbac;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.domain.mapper.rbac.RbacPermissionMapper;
import com.jc.rag.domain.entity.rbac.RbacPermissionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 路由 → permCode 路由表加载器。
 * <p>从 sys_permission 表加载 permType='API' AND status=1 的记录，
 * 以 url_pattern 为 key、method 为副 key 缓存 permCode。
 * <p>线程安全：volatile 引用 + 不可变 Map 实例，reload 时整体替换。
 * <p>字段分工：
 * <ul>
 *   <li>{@code path}：前端菜单路径（/admin/dashboard），前端路由用</li>
 *   <li>{@code url_pattern}：Controller 注解 path（/api/sys/user/{id}），本加载器用</li>
 * </ul>
 *
 * @author JC
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionRouteLoader implements ApplicationRunner {

    private final RbacPermissionMapper permissionMapper;

    /** pattern -> { METHOD -> permCode }，整体不可变，reload 时整体替换 */
    private final AtomicReference<Map<String, Map<String, String>>> routePermRef =
        new AtomicReference<>(Collections.emptyMap());

    @Override
    public void run(ApplicationArguments args) {
        reload();
    }

    /** 重新加载路由表（管理员改 permCode 后可手动调用） */
    public synchronized void reload() {
        List<RbacPermissionEntity> apis = permissionMapper.selectList(
            new QueryWrapper<RbacPermissionEntity>()
                .eq("perm_type", "API")
                .eq("status", 1)
                .isNotNull("url_pattern")
                .isNotNull("method")
        );
        Map<String, Map<String, String>> map = new HashMap<>();
        for (RbacPermissionEntity p : apis) {
            if (p.getUrlPattern() == null || p.getMethod() == null) continue;
            map.computeIfAbsent(p.getUrlPattern(), k -> new HashMap<>())
                .put(p.getMethod().toUpperCase(), p.getPermCode());
        }
        routePermRef.set(Collections.unmodifiableMap(map));
        log.info("[PermissionRouteLoader] 加载 API 路由表: {} 条", apis.size());
    }

    /**
     * 查 (pattern, method) 对应的 permCode，未注册返回 null。
     * @param pathPattern Controller 注解的 path（带占位符 /api/sys/user/{id}）
     * @param method HTTP method (GET/POST/...)
     * @return permCode | null
     */
    public String findRequiredPerm(String pathPattern, String method) {
        if (pathPattern == null || method == null) return null;
        Map<String, String> byMethod = routePermRef.get().get(pathPattern);
        if (byMethod == null) return null;
        return byMethod.get(method.toUpperCase());
    }

    /** 当前已加载的路由条数 */
    public int size() {
        return routePermRef.get().size();
    }
}
