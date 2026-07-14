package com.jc.rag.admin.module.rbac.department.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jc.rag.common.model.base.BaseVO;

import java.util.List;

/**
 * 部门出参 VO (前端展示用)。
 * <p>包含树形结构字段 children (前端 el-table 懒加载 / tree 控件使用)。
 * <p>创建时间 / 更新时间继承自 BaseVO (已带 @JsonFormat 注解, 不要再重复定义否则会遮蔽注解)。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RbacDepartmentVO extends BaseVO {

    private Long parentId;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String statusText;
    private String remark;
    /**
     * 该部门下的岗位数量 (用于前端展示)
     */
    private Long positionCount;
    /**
     * 是否有子部门 (前端懒加载控制)
     */
    private Boolean hasChildren;
    /**
     * 子部门列表 (树形结构)
     */
    private List<RbacDepartmentVO> children;
    /**
     * 当前用户是否可编辑 (仅 root 接口填充; 用于前端控制公司名称编辑入口)
     */
    private Boolean canEdit;

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
    public String getStatusText() { return statusText; }
    public void setStatusText(String v) { statusText = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { remark = v; }
    public Long getPositionCount() { return positionCount; }
    public void setPositionCount(Long v) { positionCount = v; }
    public Boolean getHasChildren() { return hasChildren; }
    public void setHasChildren(Boolean v) { hasChildren = v; }
    public List<RbacDepartmentVO> getChildren() { return children; }
    public void setChildren(List<RbacDepartmentVO> v) { children = v; }
    public Boolean getCanEdit() { return canEdit; }
    public void setCanEdit(Boolean v) { canEdit = v; }
}
