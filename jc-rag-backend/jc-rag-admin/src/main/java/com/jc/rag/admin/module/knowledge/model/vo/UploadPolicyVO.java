package com.jc.rag.admin.module.knowledge.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 上传策略 VO。
 * <p>前端在打开上传组件前先拉取本配置, 用于限制文件大小 / 类型 / 提示文案。
 *
 * @author JC
 */
@Data
public class UploadPolicyVO {
    private Long maxFileSizeBytes;
    private Long maxRequestSizeBytes;
    private String maxFileSizeLabel;
    private String maxRequestSizeLabel;
    private List<String> allowedExtensions;
    private String accept;
    private Long parseTimeoutSeconds;
}
