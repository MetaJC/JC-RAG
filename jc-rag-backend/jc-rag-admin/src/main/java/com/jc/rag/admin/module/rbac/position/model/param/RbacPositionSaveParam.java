package com.jc.rag.admin.module.rbac.position.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 岗位新增 / 修改 Param。
 *
 * @author JC
 */
public class RbacPositionSaveParam {

    @NotNull(message = "所属部门 id 不能为空")
    private Long departmentId;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 50, message = "岗位名称长度不能超过 50")
    private String name;

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 50, message = "岗位编码长度不能超过 50")
    private String code;

    /**
     * 岗位级别: 1 初级 / 2 中级 / 3 高级
     */
    @NotNull(message = "岗位级别不能为空")
    private Integer level;

    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long v) { departmentId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer v) { level = v; }
    public Integer getSort() { return sort; }
    public void setSort(Integer v) { sort = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { status = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { remark = v; }
}
