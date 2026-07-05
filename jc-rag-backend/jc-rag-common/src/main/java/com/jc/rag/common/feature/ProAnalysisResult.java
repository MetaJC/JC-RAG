package com.jc.rag.common.feature;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 高级问答分析结果 (商业付费功能).
 *
 * @author JC
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProAnalysisResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总会话数 */
    private Long totalSessions;

    /** 平均会话轮次 */
    private Double avgTurns;

    /** 高频提问关键词 Top10 */
    private List<String> topKeywords;

    /** 用户满意度评分 (0-100, 商业版基于多轮反馈计算; 开源版恒为 0) */
    private Integer satisfactionScore;

    /** 是否为商业增强结果 */
    private boolean proEnhanced;
}
