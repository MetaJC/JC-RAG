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

import java.util.Map;

/**
 * 运行时配置视图 (合并 application.yml + sys_config 动态表)。
 * @author JC
 */
public class RuntimeConfigVO {

    private Map<String, Object> app;
    private Map<String, Object> model;
    private Map<String, Object> rag;
    private Map<String, Object> service;

    public Map<String, Object> getApp() { return app; }
    public void setApp(Map<String, Object> app) { this.app = app; }
    public Map<String, Object> getModel() { return model; }
    public void setModel(Map<String, Object> model) { this.model = model; }
    public Map<String, Object> getRag() { return rag; }
    public void setRag(Map<String, Object> rag) { this.rag = rag; }
    public Map<String, Object> getService() { return service; }
    public void setService(Map<String, Object> service) { this.service = service; }
}
