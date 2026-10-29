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


package com.jc.rag.admin.module.feature.service.impl;

import com.jc.rag.common.feature.ProAnalysisResult;
import com.jc.rag.common.feature.ProFeatureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * ProFeatureService 开源版默认实现.
 * <p>
 * 激活条件: {@code jc-rag.edition = oss} 或未配置 (默认).
 * 商业版 ({@code jc-rag.edition = pro}) 由 jc-rag-pro 模块的 ProFeatureServiceProImpl 覆盖本 Bean.
 *
 * @author JC
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "jc-rag.edition", havingValue = "oss", matchIfMissing = true)
@ConditionalOnMissingBean(name = "proFeatureServiceProImpl")
public class ProFeatureServiceOssImpl implements ProFeatureService {

    @Override
    public ProAnalysisResult analyze(Long kbId) {
        log.debug("[OSS] basic pro-feature analysis, kbId={}", kbId);
        // 开源版: 仅返回占位结果, 提示升级到商业版
        return ProAnalysisResult.builder()
            .totalSessions(0L)
            .avgTurns(0.0)
            .topKeywords(Collections.emptyList())
            .satisfactionScore(0)
            .proEnhanced(false)
            .build();
    }
}
