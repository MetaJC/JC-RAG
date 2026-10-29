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
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户-岗位关联实体 (多对多)。
 * <p>对应 rbac_user_position 表, 一行表示一个用户被赋予一个岗位。
 * <p>isPrimary=1 表示主岗位 (用于列表展示 / 默认归属, 第二阶段使用).
 * <p>同一 (user_id, position_id) 组合唯一, 由数据库唯一索引保证.
 *
 * @author JC
 */
@TableName("rbac_user_position")
public class RbacUserPositionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("position_id")
    private Long positionId;

    @TableField("is_primary")
    private Integer isPrimary;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public Long getPositionId() { return positionId; }
    public void setPositionId(Long v) { positionId = v; }
    public Integer getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Integer v) { isPrimary = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
}
