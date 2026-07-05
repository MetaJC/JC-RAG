package com.jc.rag.user;

import com.jc.rag.admin.module.feature.service.impl.ProEvaluationServiceOssImpl;
import com.jc.rag.common.SystemDataInitializer;
import com.jc.rag.common.feature.ProEvaluationService;
import com.jc.rag.domain.mapper.chat.UserDialogueEvaluationMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.user.security.CoreUserDetailsService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 社区版 user 端启动入口.
 *
 * <p>扫描范围: {@code com.jc.rag.user} + {@code com.jc.rag.common}.
 * <strong>不再扫描 {@code com.jc.rag.admin}</strong> — 避免 admin 的 SecurityConfig 与 user 的 UserSecurityConfig 同时
 * 加载为 2 个 SecurityFilterChain bean 冲突.</p>
 *
 * <p>虽然 pom 引入了 {@code jc-rag-admin} 依赖 (为了拿到 ProEvaluationServiceOssImpl), 但通过本类 @Bean 显式注册,
 * 不走组件扫描, 避免扫描到 admin 的 SecurityConfig / 控制器 / 服务.</p>
 *
 * @author JC
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.jc.rag.user", "com.jc.rag.common"})
@MapperScan({"com.jc.rag.common.mapper", "com.jc.rag.domain.mapper", "com.jc.rag.user.module.*.mapper"})
public class UserApplication {
    public static void main(String[] args) {
        com.jc.rag.common.banner.JcRagStartupBanner.markStart();
        SpringApplication.run(UserApplication.class, args);
    }

    @Bean
    public CoreUserDetailsService coreUserDetailsService(RbacUserMapper rbacUserMapper) {
        return new CoreUserDetailsService(rbacUserMapper);
    }

    @Bean
    public SystemDataInitializer systemDataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return new SystemDataInitializer(jdbcTemplate, passwordEncoder);
    }

    /**
     * 显式注册社区版 ProEvaluationService 实现.
     * <p>原由 com.jc.rag.admin 的 ProEvaluationServiceOssImpl 通过 @ComponentScan 自动注册.
     * 但 user 模块不扫描 com.jc.rag.admin (避免 SecurityConfig 冲突), 故改用 @Bean 显式注册.</p>
     */
    @Bean
    public ProEvaluationService proEvaluationServiceOss(JdbcTemplate jdbcTemplate, UserDialogueEvaluationMapper evaluationMapper) {
        return new ProEvaluationServiceOssImpl(jdbcTemplate, evaluationMapper);
    }
}

