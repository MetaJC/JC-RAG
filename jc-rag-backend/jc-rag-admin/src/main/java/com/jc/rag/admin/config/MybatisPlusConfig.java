package com.jc.rag.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 扩展配置：注册分页拦截器。
 * <p>SQL 走 XML，Mapper 方法形参为 {@code IPage<Map> pageXxx(IPage, cond)}，
 * 拦截器自动追加 {@code LIMIT/OFFSET} 并自动执行 {@code SELECT COUNT(1)}。
 * <p>Service 层一行调用即可，禁止手算 offset。
 * @author JC
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最多 2000 条，防止 OFFSET 过大拖垮 DB
        pagination.setMaxLimit(2000L);
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        log.info("[MybatisPlusConfig] 已注册 PaginationInnerInterceptor(DbType.MYSQL, maxLimit=2000)");
        return interceptor;
    }
}
