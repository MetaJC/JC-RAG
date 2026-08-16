package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

/**
 * 模型目录中的服务商节点 VO。
 *
 * <p>id 与 llm_provider_config.provider_name / 前端服务商标识保持一致。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogProviderVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private List<CatalogModelVO> models;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<CatalogModelVO> getModels() { return models; }
    public void setModels(List<CatalogModelVO> models) { this.models = models; }
}
