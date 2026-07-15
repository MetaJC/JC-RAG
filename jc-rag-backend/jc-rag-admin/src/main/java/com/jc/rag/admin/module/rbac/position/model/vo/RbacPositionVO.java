package com.jc.rag.admin.module.rbac.position.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jc.rag.common.model.base.BaseVO;

import java.time.LocalDateTime;

/**
 * 岗位出参 VO (前端展示用)。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RbacPositionVO extends BaseVO {

    private String name;
    private String displayName;
    private Integer level;
    private String levelText;
    private Integer sort;
    private Integer status;
    private String statusText;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { displayName = v; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer v) { level = v; }
    public String getLevelText() { return levelText; }
    public void setLevelText(String v) { levelText = v; }
    public Integer getSort() { return sort; }
    public void setSort(Integer v) { sort = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { status = v; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String v) { statusText = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { remark = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
