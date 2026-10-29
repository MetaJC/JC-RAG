/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


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
