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
