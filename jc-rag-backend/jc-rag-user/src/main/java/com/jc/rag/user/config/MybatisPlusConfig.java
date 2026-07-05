package com.jc.rag.user.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 扩展配置：注册分页拦截器。
 * <p>与 admin 端保持一致。Service 一行调用即可，禁止手算 offset。
 * <p>显式 bean name 避免与 admin 端同名 Configuration 冲突（Pro Edition fat-jar 同时打包 admin+user 时）.
 * @author JC
 */
@Slf4j
@Configuration("userMybatisPlusConfig")
public class MybatisPlusConfig {

    @Bean("userMybatisPlusInterceptor")
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(2000L);
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        log.info("[User/MybatisPlusConfig] 已注册 PaginationInnerInterceptor(DbType.MYSQL, maxLimit=2000)");
        return interceptor;
    }
}
