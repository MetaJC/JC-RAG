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
