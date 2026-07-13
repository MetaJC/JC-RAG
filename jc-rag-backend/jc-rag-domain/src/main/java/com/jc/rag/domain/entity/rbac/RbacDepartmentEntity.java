package com.jc.rag.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 部门 实体 (RBAC 组织维度)。
 * <p>对应 rbac_department 表, parent_id=0 表示顶级, 形成组织树。
 * <p>code 为业务编码 (唯一), name 为展示名。
 *
 * @author JC
 */
@TableName("rbac_department")
public class RbacDepartmentEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 父部门 id, 0=顶级
     */
    private Long parentId;
    /**
     * 部门名称
     */
    private String name;
    /**
     * 部门编码 (业务引用, 唯一)
     */
    private String code;
    /**
     * 同级排序, 升序
     */
    private Integer sort;
    /**
     * 0=禁用 1=启用
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long v) {
        parentId = v;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String v) {
        code = v;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer v) {
        sort = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String v) {
        remark = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }
}
