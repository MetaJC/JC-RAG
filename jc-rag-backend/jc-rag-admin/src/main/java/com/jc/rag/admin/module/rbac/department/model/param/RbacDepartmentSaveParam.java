package com.jc.rag.admin.module.rbac.department.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 部门新增 / 修改 Param。
 *
 * @author JC
 */
public class RbacDepartmentSaveParam {

    /**
     * 父部门 id (0=顶级), 修改时可空 (保持原值)
     */
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过 50")
    private String name;

    /**
     * 部门编码 (前端不再展示/管理, 后端保留字段兼容历史数据, 允许为空)
     */
    @Size(max = 50, message = "部门编码长度不能超过 50")
    private String code;

    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { parentId = v; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getCode() { return code; }
    public void setCode(String v) { code = v; }
    public Integer getSort() { return sort; }
    public void setSort(Integer v) { sort = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { status = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { remark = v; }
}
