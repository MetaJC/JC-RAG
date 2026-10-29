/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 角色 实体。
 * <p>对应 rbac_role 表, roleCode 业务编码 (SUPER_ADMIN/USER/GUEST 等), roleName 展示名。
 *
 * @author JC
 */
@TableName("rbac_role")
public class RbacRoleEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 角色业务编码 (代码引用, 不可重复)
     */
    private String roleCode;
    /**
     * 角色展示名
     */
    private String roleName;
    /**
     * 角色描述
     */
    private String roleDesc;
    /**
     * 启用状态 (1 启用 / 0 禁用)
     */
    private Integer status;
    /**
     * 是否允许登录管理端 (0 否 / 1 是)
     * <p>普通角色默认 0, super_admin 强制 1; 登录管理端校验的唯一开关
     */
    private Integer isAdmin;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String v) {
        roleCode = v;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String v) {
        roleName = v;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public void setRoleDesc(String v) {
        roleDesc = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public Integer getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Integer v) {
        isAdmin = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
