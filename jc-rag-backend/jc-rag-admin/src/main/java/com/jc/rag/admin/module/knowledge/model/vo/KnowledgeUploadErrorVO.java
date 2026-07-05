package com.jc.rag.admin.module.knowledge.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 知识库上传异常响应 VO。
 * <p>前端可依据 code 判定业务失败，并按 maxFileSizeBytes/maxRequestSizeBytes 给出友好提示。
 *
 * @author JC
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeUploadErrorVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 错误大类，如 "上传文件超出限制" */
    private String error;

    /** 详细错误信息 */
    private String message;

    /** 单文件上限字节数 */
    private Long maxFileSizeBytes;

    /** 单次上传总量上限字节数 */
    private Long maxRequestSizeBytes;

    /** 单文件上限展示文本 */
    private String maxFileSizeLabel;

    /** 单次上传总量上限展示文本 */
    private String maxRequestSizeLabel;
}
