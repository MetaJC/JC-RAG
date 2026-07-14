package com.jc.rag.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户-部门关联实体 (多对多)。
 * <p>对应 rbac_user_department 表, 一行表示一个用户被加入一个部门。
 * <p>isPrimary=1 表示主部门, 用于列表展示 / 默认归属, 同步冗余到 rbac_user.department_id。
 * <p>同一 (user_id, department_id) 组合唯一, 由数据库唯一索引保证。
 *
 * @author JC
 */
@TableName("rbac_user_department")
public class RbacUserDepartmentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("department_id")
    private Long departmentId;

    @TableField("is_primary")
    private Integer isPrimary;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long v) {
        userId = v;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long v) {
        departmentId = v;
    }

    public Integer getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Integer v) {
        isPrimary = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
