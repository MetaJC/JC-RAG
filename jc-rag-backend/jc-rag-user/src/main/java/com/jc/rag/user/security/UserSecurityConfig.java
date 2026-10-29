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


package com.jc.rag.user.security;

import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.user.security.CoreUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc.rag.user.security.DeveloperBypassFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.LinkedHashMap;
import java.util.List;

@Configuration
@EnableWebSecurity
public class UserSecurityConfig {

    private final ObjectMapper objectMapper;

    public UserSecurityConfig(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Bean
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {
        // 显式声明 HttpSession 仓库, 否则 Spring Security 6 默认走 RequestAttributeSecurityContextRepository,
        // 会读不到 AuthController 写入 session 的 SecurityContext, 导致登录态丢失。
        org.springframework.security.web.context.HttpSessionSecurityContextRepository securityContextRepository =
            new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository))
            .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, authEx) -> {
                res.setContentType("application/json;charset=utf-8");
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                var map = new LinkedHashMap<String, Object>();
                map.put("error", "unauthorized");
                map.put("message", "请先登录");
                objectMapper.writeValue(res.getOutputStream(), map);
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/api/auth/login", "/api/auth/register", "/api/health/**").permitAll()
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/assets/**",
                    "/favicon.ico",
                    "/*.js",
                    "/*.css",
                    "/*.png",
                    "/*.svg",
                    "/*.ico",
                    "/*.woff2"
                ).permitAll()
                .requestMatchers("/api/user/**", "/api/chat/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .logout(l -> l.disable())
            .addFilterBefore(new DeveloperBypassFilter(), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(CoreUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

