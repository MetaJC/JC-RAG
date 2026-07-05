package com.jc.rag.admin.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.domain.mapper.rbac.RbacPermissionMapper;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 业务用户加载。
 * <p>登录时一次性加载 user → role → permission 全部 permCode，
 * 后续由 Spring Security authorities 缓存到 SecurityContext，避免每次请求再查库。
 *
 * @author JC
 */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final RbacUserMapper sysUserMapper;
    private final RbacPermissionMapper sysPermissionMapper;

    public DbUserDetailsService(RbacUserMapper sysUserMapper, RbacPermissionMapper sysPermissionMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        RbacUserEntity user = sysUserMapper.selectOne(
            new QueryWrapper<RbacUserEntity>()
                .eq("username", username)
                .eq("status", 1)
                .eq("deleted", 0)
                .last("LIMIT 1")
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 一次性加载用户拥有的全部 permCode（去重，O(1) 后无需再查）
        List<String> permCodes = sysPermissionMapper.findPermCodesByUserId(user.getId());
        return new DbUserDetails(
            user.getId(),
            user.getUsername(),
            user.getPasswordHash(),
            user.getNickname(),
            user.getUserType(),
            permCodes
        );
    }
}
