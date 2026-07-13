package com.jc.rag.admin.module.rbac.position.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jc.rag.common.model.base.BaseVO;

/**
 * 岗位出参 VO (前端展示用)。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RbacPositionVO extends BaseVO {

    private Long departmentId;
    private String departmentName;
    private String name;
    private String code;
    private Integer level;
    private String levelText;
    private Integer sort;
    private Integer status;
    private String statusText;
    private String remark;

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long v) { departmentId = v; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String v) { departmentName = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
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
}
