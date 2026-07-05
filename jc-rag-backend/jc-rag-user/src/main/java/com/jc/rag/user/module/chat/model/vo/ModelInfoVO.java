package com.jc.rag.user.module.chat.model.vo;

import java.util.List;

/**
 * C 端"当前生效模型"VO。
 * <p>用于前端判断多模态 / 工具调用 / 思考链等能力。
 * <ul>
 *   <li>activeMode: online / offline</li>
 *   <li>modelName: 当前模型标识</li>
 *   <li>capabilities: 模型能力标签 (vision / tools / thinking / completion)</li>
 *   <li>supportsVision: 便捷布尔, = capabilities.contains("vision")</li>
 * </ul>
 *
 * @author JC
 */
public class ModelInfoVO {
    private String activeMode;
    private String modelName;
    private List<String> capabilities;
    private Boolean supportsVision;

    public String getActiveMode() { return activeMode; }
    public void setActiveMode(String activeMode) { this.activeMode = activeMode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
    public Boolean getSupportsVision() { return supportsVision; }
    public void setSupportsVision(Boolean supportsVision) { this.supportsVision = supportsVision; }

    @Override
    public String toString() {
        return "ModelInfoVO{activeMode='" + activeMode + "', modelName='" + modelName + "', supportsVision=" + supportsVision + '}';
    }
}
