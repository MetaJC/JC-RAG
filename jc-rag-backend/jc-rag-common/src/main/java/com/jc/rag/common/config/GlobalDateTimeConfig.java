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


package com.jc.rag.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.jc.rag.common.exception.ParamInvalidException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 全局时间格式统一配置
 * <p>统一所有时间的输入输出格式：
 * <ul>
 *   <li>完整时间: yyyy-MM-dd HH:mm:ss</li>
 *   <li>仅日期: yyyy-MM-dd</li>
 * </ul>
 *
 * @author JC
 */
@Configuration
public class GlobalDateTimeConfig implements WebMvcConfigurer {

    /** 完整时间格式 */
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    /** 仅日期格式 */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    
    /** DateTimeFormatter 实例（线程安全） */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    /**
     * 配置全局 ObjectMapper，统一响应时间格式
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 配置 Java 8 时间类型
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATETIME_FORMATTER));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DATE_FORMATTER));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME_FORMATTER));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
        objectMapper.registerModule(javaTimeModule);
        
        // 配置 Date 类型序列化格式
        objectMapper.setDateFormat(new SimpleDateFormat(DATETIME_FORMAT));
        
        // 禁用日期时间戳格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return objectMapper;
    }

    /**
     * 注册全局参数转换器：String → Date / LocalDateTime / LocalDate
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // String → Date（支持两种格式）
        registry.addConverter(new StringToDateConverter());
        
        // String → LocalDateTime（支持两种格式）
        registry.addConverter(new StringToLocalDateTimeConverter());
        
        // String → LocalDate
        registry.addConverter(new StringToLocalDateConverter());
    }

    /**
     * String → Date 转换器
     * <p>支持格式：yyyy-MM-dd HH:mm:ss、yyyy-MM-dd
     */
    private static class StringToDateConverter implements Converter<String, Date> {
        @Override
        public Date convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            String trimmed = source.trim();
            try {
                // 尝试完整时间格式
                if (trimmed.length() == 19) {
                    return new SimpleDateFormat(DATETIME_FORMAT).parse(trimmed);
                }
                // 尝试仅日期格式
                if (trimmed.length() == 10) {
                    return new SimpleDateFormat(DATE_FORMAT).parse(trimmed);
                }
                // 其他格式尝试解析
                return new SimpleDateFormat(DATETIME_FORMAT).parse(trimmed);
            } catch (ParseException e) {
                throw new ParamInvalidException("common.datetime.parse.failed", source);
            }
        }
    }

    /**
     * String → LocalDateTime 转换器
     * <p>支持格式：yyyy-MM-dd HH:mm:ss、yyyy-MM-dd
     */
    private static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {
        @Override
        public LocalDateTime convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            String trimmed = source.trim();
            try {
                // 尝试完整时间格式
                if (trimmed.length() == 19) {
                    return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
                }
                // 仅日期格式，补充默认时间 00:00:00
                if (trimmed.length() == 10) {
                    return LocalDate.parse(trimmed, DATE_FORMATTER).atStartOfDay();
                }
                return LocalDateTime.parse(trimmed, DATETIME_FORMATTER);
            } catch (Exception e) {
                throw new ParamInvalidException("common.datetime.parse.failed", source);
            }
        }
    }

    /**
     * String → LocalDate 转换器
     */
    private static class StringToLocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            String trimmed = source.trim();
            try {
                // 仅取日期部分
                if (trimmed.length() >= 10) {
                    return LocalDate.parse(trimmed.substring(0, 10), DATE_FORMATTER);
                }
                return LocalDate.parse(trimmed, DATE_FORMATTER);
            } catch (Exception e) {
                throw new ParamInvalidException("common.datetime.parse.failed", source);
            }
        }
    }
}