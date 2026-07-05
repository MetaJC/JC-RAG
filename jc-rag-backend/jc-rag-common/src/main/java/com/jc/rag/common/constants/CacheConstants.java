package com.jc.rag.common.constants;

/**
 * 缓存 key 常量
 * <p>统一管理跨模块使用的 Redis / Caffeine 缓存 key 前缀，避免散落硬编码。
 * <p>命名规则：模块:业务:维度（如 sys:perm:route:v1）
 * @author JC
 */
public final class CacheConstants {

    private CacheConstants() {}

    /** 权限路由表缓存（pattern -> method -> permCode） */
    public static final String PERM_ROUTE_MAP = "sys:perm:route:v1";
    /** 用户权限码缓存（userId -> Set<permCode>） */
    public static final String USER_PERM_CODES = "sys:user:perm-codes:v1";
    /** 用户角色缓存（userId -> Set<roleCode>） */
    public static final String USER_ROLES = "sys:user:roles:v1";

    /** 缓存统一默认过期时间（秒） */
    public static final long DEFAULT_TTL_SECONDS = 300L;
}
