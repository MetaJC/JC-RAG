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
 * 角色-权限关联实体。
 * <p>dataScope：数据权限范围（1=全部 2=本部门 3=本人 4=自定义），用于后续 AOP 数据过滤。
 *
 * @author JC
 */
@TableName("rbac_role_permission")
public class RbacRolePermissionEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 角色 ID
     */
    private Long roleId;
    /**
     * 权限 ID
     */
    private Long permissionId;
    /**
     * 数据权限范围 (1 全部 / 2 本部门 / 3 本人 / 4 自定义)
     */
    private Integer dataScope;
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

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long v) {
        roleId = v;
    }

    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long v) {
        permissionId = v;
    }

    public Integer getDataScope() {
        return dataScope;
    }

    public void setDataScope(Integer v) {
        dataScope = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
