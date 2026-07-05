package com.jc.rag.admin.module.sys.model.vo;

/**
 * 服务健康项视图。
 * @author JC
 */
public class ServiceHealthItemVO {

    private String name;
    private String status;
    private String endpoint;
    private String description;
    private String detail;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
