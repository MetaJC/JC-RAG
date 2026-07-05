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
