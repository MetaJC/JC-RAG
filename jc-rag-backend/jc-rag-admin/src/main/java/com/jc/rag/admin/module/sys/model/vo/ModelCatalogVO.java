package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

/**
 * 模型目录 VO (三层同步机制的统一返回结构)。
 *
 * <p>source 取值:
 * <ul>
 *   <li>builtin - L1 内置目录 (随版本发布)</li>
 *   <li>remote - L2 远程目录 (含远程缓存)</li>
 * </ul>
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelCatalogVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String version;
    private String source;
    private String updatedAt;
    private List<CatalogProviderVO> providers;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public List<CatalogProviderVO> getProviders() { return providers; }
    public void setProviders(List<CatalogProviderVO> providers) { this.providers = providers; }
}
