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


package com.jc.rag.common.web.page;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 时间格式化工具类
 * <p>统一使用 {@link DateTimeFormatter}（线程安全），支持两种格式：
 * <ul>
 *   <li>完整时间: yyyy-MM-dd HH:mm:ss</li>
 *   <li>仅日期: yyyy-MM-dd</li>
 * </ul>
 * <p>admin / user 两侧共用。
 *
 * @author JC
 */
public final class TimeFormatUtil {

    /** 完整时间格式 */
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    /** 仅日期格式 */
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    /** DateTimeFormatter 实例（线程安全） */
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern(DATE_FORMAT);
    
    /** SimpleDateFormat 实例（用于 Date 类型，非线程安全，每次创建新实例） */

    private TimeFormatUtil() {}

    // ==================== 格式化方法 ====================

    /**
     * Timestamp → String（yyyy-MM-dd HH:mm:ss）
     */
    public static String format(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toLocalDateTime().format(DATETIME_FMT);
    }

    /**
     * Date → String（yyyy-MM-dd HH:mm:ss）
     */
    public static String format(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATETIME_FORMAT).format(date);
    }

    /**
     * LocalDateTime → String（yyyy-MM-dd HH:mm:ss）
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FMT);
    }

    /**
     * LocalDate → String（yyyy-MM-dd）
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FMT);
    }

    /**
     * Date → String（仅日期 yyyy-MM-dd）
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_FORMAT).format(date);
    }

    // ==================== 解析方法 ====================

    /**
     * String → Date
     * <p>支持格式：yyyy-MM-dd HH:mm:ss、yyyy-MM-dd
     */
    public static Date parse(String dateStr) throws ParseException {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = dateStr.trim();
        if (trimmed.length() == 19) {
            return new SimpleDateFormat(DATETIME_FORMAT).parse(trimmed);
        }
        if (trimmed.length() == 10) {
            return new SimpleDateFormat(DATE_FORMAT).parse(trimmed);
        }
        return new SimpleDateFormat(DATETIME_FORMAT).parse(trimmed);
    }

    /**
     * String → LocalDateTime
     * <p>支持格式：yyyy-MM-dd HH:mm:ss、yyyy-MM-dd
     */
    public static LocalDateTime parseToLocalDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = dateStr.trim();
        if (trimmed.length() == 19) {
            return LocalDateTime.parse(trimmed, DATETIME_FMT);
        }
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed, DATE_FMT).atStartOfDay();
        }
        return LocalDateTime.parse(trimmed, DATETIME_FMT);
    }

    /**
     * String → LocalDate
     */
    public static LocalDate parseToLocalDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        String trimmed = dateStr.trim();
        if (trimmed.length() >= 10) {
            return LocalDate.parse(trimmed.substring(0, 10), DATE_FMT);
        }
        return LocalDate.parse(trimmed, DATE_FMT);
    }

    // ==================== 类型转换方法 ====================

    /**
     * Date → LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * LocalDateTime → Date
     */
    public static Date toDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDate → Date
     */
    public static Date toDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Timestamp → LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toLocalDateTime();
    }

    /**
     * LocalDateTime → Timestamp
     */
    public static Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Timestamp.valueOf(dateTime);
    }

    /**
     * Date → Timestamp
     */
    public static Timestamp toTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }
}