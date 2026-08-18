package com.jc.rag.admin.module.auth.service;

import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 首次启动密码引导服务。
 * <p>背景: Flyway V3 种子哈希为历史遗留固定值, 与文档约定密码不一致,
 * 需在首次启动时用运行时编码器重置为文档密码 (见 startup-guide.md 默认账号表).
 * <p>admin 与 developer 各自独立引导标记: 存量库 admin 已引导过时,
 * 仍可单独补引导 developer, 互不影响.
 *
 * @author JC
 */
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
        bootstrapAdminPassword();
        bootstrapDeveloperPassword();
    }

    /**
     * 首次引导: 重置 admin 密码为文档默认值。
     */
    private void bootstrapAdminPassword() {
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

    /**
     * 首次引导: 重置 developer 兜底账号密码为文档默认值。
     * <p>V3 种子哈希与文档密码 developer@2024 不一致, 且 SystemDataInitializer
     * 因 sys_user 已改名 rbac_user 不再生效, 故在此补齐引导。
     * 独立标记保证已引导过 admin 的存量库仍可补引导 developer。
     */
    private void bootstrapDeveloperPassword() {
        try {
            String applied = sysConfigMapper.selectStringValue("auth.developer.bootstrapApplied");
            if ("1".equals(applied)) {
                return;
            }

            rbacUserMapper.updatePasswordByUsername("developer", passwordEncoder.encode("developer@2024"));
            sysConfigMapper.upsertConfig(
                "auth",
                "auth.developer.bootstrapApplied",
                "1",
                "STRING",
                0,
                "首次引导 developer 兜底账号密码已初始化"
            );
        } catch (Exception ignored) {
            // 数据库尚未就绪时保持启动可用。
        }
    }
}
