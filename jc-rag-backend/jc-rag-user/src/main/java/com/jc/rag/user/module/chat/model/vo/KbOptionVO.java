package com.jc.rag.user.module.chat.model.vo;

/**
 * C 端"可选知识库"VO。
 * <p>用于 /api/chat/knowledge-bases 列出 C 端允许选择的知识库 (含默认标记)。
 * <p>isDefault: 1=当前会话的默认知识库, 0=其他可选。
 *
 * @author JC
 */
public class KbOptionVO {
    private Long id;
    private String kbCode;
    private String kbName;
    private String kbDesc;
    private Integer isDefault;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKbCode() { return kbCode; }
    public void setKbCode(String kbCode) { this.kbCode = kbCode; }
    public String getKbName() { return kbName; }
    public void setKbName(String kbName) { this.kbName = kbName; }
    public String getKbDesc() { return kbDesc; }
    public void setKbDesc(String kbDesc) { this.kbDesc = kbDesc; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }

    @Override
    public String toString() {
        return "KbOptionVO{id=" + id + ", kbCode='" + kbCode + "', kbName='" + kbName + "', isDefault=" + isDefault + '}';
    }
}
