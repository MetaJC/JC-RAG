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
 * 系统资源 (CPU/GPU/RAM) 响应 VO (admin)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SystemResourceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Object> cpu;
    private List<Map<String, Object>> gpus;
    @com.fasterxml.jackson.annotation.JsonProperty("ram")
    private Map<String, Object> memory;
    private Map<String, Object> disk;

    public Map<String, Object> getCpu() { return cpu; }
    public void setCpu(Map<String, Object> cpu) { this.cpu = cpu; }
    public List<Map<String, Object>> getGpus() { return gpus; }
    public void setGpus(List<Map<String, Object>> gpus) { this.gpus = gpus; }
    public Map<String, Object> getMemory() { return memory; }
    public void setMemory(Map<String, Object> memory) { this.memory = memory; }
    public Map<String, Object> getDisk() { return disk; }
    public void setDisk(Map<String, Object> disk) { this.disk = disk; }
}
