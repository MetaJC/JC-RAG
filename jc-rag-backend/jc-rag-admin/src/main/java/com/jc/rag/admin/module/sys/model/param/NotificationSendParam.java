package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 发送通知请求参数 (admin)。
 * @author JC
 */
public class NotificationSendParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "title 不能为空")
    private String title;

    @NotBlank(message = "content 不能为空")
    private String content;

    private String targetUserType;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTargetUserType() { return targetUserType; }
    public void setTargetUserType(String targetUserType) { this.targetUserType = targetUserType; }
}
