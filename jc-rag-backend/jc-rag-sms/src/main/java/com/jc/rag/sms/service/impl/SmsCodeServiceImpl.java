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


package com.jc.rag.sms.service.impl;

import com.jc.rag.sms.config.SmsProperties;
import com.jc.rag.sms.enums.SmsCodeLength;
import com.jc.rag.sms.enums.SmsCodeType;
import com.jc.rag.sms.service.SmsCodeService;
import com.jc.rag.sms.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * 短信验证码生命周期实现 (Redis 存储).
 * <p>Redis Key 约定:
 * <ul>
 *   <li>{@code jc:sms:limit:{phone}} — 发送限流标记, TTL=发送间隔, setIfAbsent 原子占位</li>
 *   <li>{@code jc:sms:code:{phone}} — 验证码明文, TTL=验证码有效期, 校验通过后立即删除</li>
 * </ul>
 * <p>开发测试后门: jc-rag.sms.test-enabled=true 时输入固定 test-code 直接放行,
 * 仅限开发环境, 生产必须关闭 (默认关闭).
 *
 * @author JC
 */
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeServiceImpl.class);

    /** 发送限流 Key 前缀 */
    private static final String LIMIT_KEY_PREFIX = "jc:sms:limit:";

    /** 验证码 Key 前缀 */
    private static final String CODE_KEY_PREFIX = "jc:sms:code:";

    private final SmsService smsService;
    private final SmsProperties properties;
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public SmsCodeServiceImpl(SmsService smsService, SmsProperties properties, StringRedisTemplate redis) {
        this.smsService = smsService;
        this.properties = properties;
        this.redis = redis;
    }

    @Override
    public long sendCode(String phone) {
        return sendCode(phone, SmsCodeLength.DEFAULT, SmsCodeType.DEFAULT);
    }

    @Override
    public long sendCode(String phone, SmsCodeLength length) {
        return sendCode(phone, length, SmsCodeType.DEFAULT);
    }

    @Override
    public long sendCode(String phone, SmsCodeLength length, SmsCodeType type) {
        if (length == null || type == null) {
            throw new IllegalArgumentException("验证码长度/字符类型枚举不能为空");
        }
        String limitKey = LIMIT_KEY_PREFIX + phone;
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(limitKey, "1", Duration.ofSeconds(properties.getSendIntervalSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            Long ttl = redis.getExpire(limitKey);
            return ttl == null || ttl < 0 ? properties.getSendIntervalSeconds() : ttl;
        }
        String code = generateCode(length, type);
        long ttlMinutes = Math.max(1, properties.getCodeTtlSeconds() / 60);
        try {
            smsService.sendCode(phone, code, ttlMinutes);
        } catch (RuntimeException e) {
            // 渠道失败: 释放限流占位, 允许立即重试
            redis.delete(limitKey);
            throw e;
        }
        redis.opsForValue().set(CODE_KEY_PREFIX + phone, code, Duration.ofSeconds(properties.getCodeTtlSeconds()));
        return 0L;
    }

    /**
     * 按长度与字符类型生成验证码 (SecureRandom, 字母数字码全大写).
     */
    private String generateCode(SmsCodeLength length, SmsCodeType type) {
        String charset = type.getCharset();
        StringBuilder sb = new StringBuilder(length.getDigits());
        for (int i = 0; i < length.getDigits(); i++) {
            sb.append(charset.charAt(random.nextInt(charset.length())));
        }
        return sb.toString();
    }

    @Override
    public boolean verifyAndConsume(String phone, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        // 开发测试后门: 仅 test-enabled=true 时生效, 不消费真实验证码
        if (properties.isTestEnabled() && code.equals(properties.getTestCode())) {
            log.warn("[SMS] 测试后门验证码命中 (test-enabled=true): phone={}", phone);
            return true;
        }
        String key = CODE_KEY_PREFIX + phone;
        String cached = redis.opsForValue().get(key);
        // 字母数字码忽略大小写比对 (存储为大写), 纯数字码不受影响
        if (cached != null && cached.equalsIgnoreCase(code.trim())) {
            redis.delete(key);
            return true;
        }
        return false;
    }

    @Override
    public long getSendIntervalSeconds() {
        return properties.getSendIntervalSeconds();
    }

    @Override
    public long getRemainingInterval(String phone) {
        Long ttl = redis.getExpire(LIMIT_KEY_PREFIX + phone);
        return ttl == null || ttl < 0 ? 0L : ttl;
    }
}
