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
