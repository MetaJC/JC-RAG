package com.jc.rag.common.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * 统一登录身份接口 (双版本共享).
 * <p>admin 端使用 DbUserDetails, user 端使用 CoreUserDetails,
 * 跨模块组件 (如商业版切面) 无法绑定具体实现类, 统一通过本接口
 * 获取用户 ID 与用户类型.
 *
 * @author JC
 */
public interface RagPrincipal extends UserDetails {

    /**
     * 获取用户 ID.
     *
     * @return 用户 ID
     */
    Long getUserId();

    /**
     * 获取用户类型 (DEVELOPER / ADMIN / NORMAL).
     *
     * @return 用户类型
     */
    String getUserType();
}
