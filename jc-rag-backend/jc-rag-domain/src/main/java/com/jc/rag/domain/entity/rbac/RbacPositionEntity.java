package com.jc.rag.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 岗位 实体 (RBAC 组织维度)。
 * <p>对应 rbac_position 表, level 1=初级 / 2=中级 / 3=高级。
 * <p>岗位用于补充角色 (rbac_role) 的组织维度, 第二阶段 (Pro 文档级权限) 复用。
 * <p>岗位与部门解耦: 不再绑定 rbac_department, 由 rbac_user_department 维护用户与部门的归属。
 *
 * @author JC
 */
@TableName("rbac_position")
public class RbacPositionEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 岗位名称
     */
    private String name;
    /**
     * 显示名 (简称 / 英文名, 例如: 岗位名=人力资源, 显示名=HR)
     */
    private String displayName;
    /**
     * 岗位级别: 1=初级 2=中级 3=高级
     */
    private Integer level;
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

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String v) {
        displayName = v;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer v) {
        level = v;
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
