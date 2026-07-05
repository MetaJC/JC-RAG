package com.jc.rag.admin.module.auth.service;

import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthBootstrapService implements ApplicationRunner {

    private final RbacUserMapper rbacUserMapper;
    private final SysConfigMapper sysConfigMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthBootstrapService(RbacUserMapper rbacUserMapper,
                                SysConfigMapper sysConfigMapper,
                                PasswordEncoder passwordEncoder) {
        this.rbacUserMapper = rbacUserMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String applied = sysConfigMapper.selectStringValue("auth.admin.bootstrapApplied");
            if ("1".equals(applied)) {
                return;
            }

            rbacUserMapper.updatePasswordByUsername("admin", passwordEncoder.encode("123456"));
            sysConfigMapper.upsertConfig(
                "auth",
                "auth.admin.bootstrapApplied",
                "1",
                "STRING",
                0,
                "首次引导 admin 密码已初始化"
            );
        } catch (Exception ignored) {
            // 如果数据库尚未初始化，保持启动可用，待建表后再由用户处理。
        }
    }
}
