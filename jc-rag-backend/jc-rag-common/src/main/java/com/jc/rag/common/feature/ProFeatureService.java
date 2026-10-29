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
 * 高级问答分析服务 (商业付费功能扩展点).
 * <p>
 * 开源版与商业版各自提供实现, 通过 {@code @ConditionalOnProperty(name = "jc-rag.edition", havingValue = ...)} 激活.
 * 调用方 (Controller / Service) 注入此接口即可, 无需关心当前运行的开源版还是商业版.
 *
 * @author JC
 */
public interface ProFeatureService {

    /**
     * 高级问答分析. 开源版返回基础统计; 商业版返回增强分析结果.
     *
     * @param kbId 知识库 ID (可空, 表示全局)
     * @return 分析结果
     */
    ProAnalysisResult analyze(Long kbId);

    /**
     * 是否为商业增强版.
     *
     * @return true = 商业版, false = 开源版
     */
    default boolean isProEnhanced() {
        return false;
    }
}
