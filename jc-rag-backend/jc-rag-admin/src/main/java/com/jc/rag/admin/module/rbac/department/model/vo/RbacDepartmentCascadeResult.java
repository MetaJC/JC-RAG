package com.jc.rag.admin.module.rbac.department.model.vo;

import java.util.List;

/**
 * 部门级联操作结果 (启停 / 移动 等可能级联影响后端的操作).
 * <p>前端可基于 affectedNames 展示具体哪些子部门被影响.
 *
 * @author JC
 */
public class RbacDepartmentCascadeResult {

    /**
     * 触发级联的根部门 id
     */
    private Long rootId;
    /**
     * 触发级联的根部门名称
     */
    private String rootName;
    /**
     * 最终生效的状态 (0=禁用 1=启用)
     */
    private Integer status;
    /**
     * 受影响的部门总数 (含根 + 后代)
     */
    private Integer affectedCount;
    /**
     * 受影响的部门名称列表 (按 id 升序, 便于前端按层级顺序展示)
     */
    private List<String> affectedNames;

    public Long getRootId() { return rootId; }
    public void setRootId(Long v) { rootId = v; }
    public String getRootName() { return rootName; }
    public void setRootName(String v) { rootName = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { status = v; }
    public Integer getAffectedCount() { return affectedCount; }
    public void setAffectedCount(Integer v) { affectedCount = v; }
    public List<String> getAffectedNames() { return affectedNames; }
    public void setAffectedNames(List<String> v) { affectedNames = v; }
}
