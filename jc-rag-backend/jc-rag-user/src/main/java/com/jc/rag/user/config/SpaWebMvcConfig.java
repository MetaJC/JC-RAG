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

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * SPA 前端路由转发配置。
 * Vue Router history 模式下，浏览器刷新子路由会 404，
 * 通过 ErrorViewResolver 将非 API 的 404 请求转发到 index.html。
 * <p>显式 bean name 避免与 admin 端同名 Configuration 冲突（Pro Edition fat-jar 同时打包 admin+user 时）.
 * @author JC
 */
@Configuration("userSpaWebMvcConfig")
public class SpaWebMvcConfig {

    @Bean("userSpaErrorViewResolver")
    public ErrorViewResolver spaErrorViewResolver() {
        return (request, status, model) -> {
            // 仅对非 API 路径的 404 做转发，API 路径保持原始 404 响应
            if (HttpStatus.NOT_FOUND.equals(status)) {
                String path = request.getRequestURI();
                if (!path.startsWith("/api/") && !path.startsWith("/actuator/")) {
                    return new ModelAndView("forward:/index.html", Map.of());
                }
            }
            return null;
        };
    }
}
