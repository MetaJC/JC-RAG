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
