package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

/**
 * 模型目录中的单个模型项 VO。
 *
 * <p>type 取值: chat/embedding/rerank/image/video/audio。
 * <p>inputs/outputs 模态取值: text/image/video/audio;
 * features 取值: tool-call/thinking/web-search/structured-output/coding/cache。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogModelVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String type;
    /** 支持的输入模态列表 (text/image/video/audio) */
    private List<String> inputs;
    /** 支持的输出模态列表 (text/image/video/audio) */
    private List<String> outputs;
    /** 功能特性列表 (tool-call/thinking/web-search/structured-output/coding/cache) */
    private List<String> features;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<String> getInputs() { return inputs; }
    public void setInputs(List<String> inputs) { this.inputs = inputs; }
    public List<String> getOutputs() { return outputs; }
    public void setOutputs(List<String> outputs) { this.outputs = outputs; }
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
}
