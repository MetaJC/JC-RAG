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


package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Ollama 模型列表响应 VO (admin)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaModelVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String baseUrl;
    private List<Map<String, Object>> models;
    /** SUCCESS / FAILED: 查询是否成功 (失败时前端可提示, 不再静默空列表) */
    private String status;
    /** 失败原因 (status=FAILED 时携带) */
    private String message;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public List<Map<String, Object>> getModels() { return models; }
    public void setModels(List<Map<String, Object>> models) { this.models = models; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
