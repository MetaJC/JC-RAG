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


package com.jc.rag.common.feature;

/**
 * 对话评估服务 (商业付费功能扩展点).
 * <p>社区版与付费版各自提供实现, 通过 {@code @ConditionalOnProperty(name = "jc-rag.edition", havingValue = ...)} 激活.</p>
 * <p>调用方注入此接口即可, 无需关心当前运行的是开源版还是商业版.</p>
 *
 * @author JC
 */
public interface ProEvaluationService {

    /**
     * 评估一段对话质量.
     *
     * @param userId  用户 ID
     * @param sessionId 会话 ID
     * @return 评估结果; 社区版若配额为 0 或功能未启用, 返回占位结果
     */
    ProEvaluationResult evaluate(Long userId, Long sessionId);

    /**
     * 查询用户今日已用评估次数.
     *
     * @param userId 用户 ID
     * @return 已用次数
     */
    int getUsedToday(Long userId);

    /**
     * 查询用户每日评估配额.
     *
     * @return 每日配额 (0 = 不可用)
     */
    int getDailyQuota();

    /**
     * 功能是否启用.
     *
     * @return true = 启用, false = 关闭
     */
    boolean isEnabled();

    /**
     * 是否为付费增强版.
     */
    default boolean isProEnhanced() {
        return false;
    }
}
