package com.jc.rag.admin.module.knowledge.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档解析进度 VO。
 * <p>stages / logs 字段保留 Map 形态, 来源于 Python 端原始 JSON 透传, 不做 DTO 化。
 *
 * @author JC
 */
@Data
public class DocParseProgressVO {
    private Long documentId;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String parseStatus;
    private Integer overallProgress;
    private String currentStage;
    private List<Map<String, Object>> stages;
    private List<Map<String, Object>> logs;
    private LocalDateTime updatedAt;
}
