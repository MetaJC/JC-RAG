package com.jc.rag.user.security;

import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;

/**
 * 共享用户加载服务 (user 模块)。
 * <p>原位于 common, 因依赖 RbacUserMapper (domain) 迁至 user 模块, 避免模块循环依赖。
 *
 * @author JC
 */
public class CoreUserDetailsService implements UserDetailsService {

    private final RbacUserMapper rbacUserMapper;

    public CoreUserDetailsService(RbacUserMapper rbacUserMapper) {
        this.rbacUserMapper = rbacUserMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Map<String, Object> row = rbacUserMapper.selectAuthInfo(username);
        if (row == null || row.isEmpty()) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }
        String userType = (String) row.get("user_type");
        boolean bypass = "DEVELOPER".equals(userType);
        return new CoreUserDetails(
            ((Number) row.get("id")).longValue(),
            (String) row.get("username"),
            (String) row.get("password_hash"),
            (String) row.get("nickname"),
            userType,
            bypass
        );
    }
}
