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
