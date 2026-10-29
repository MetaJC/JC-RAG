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


package com.jc.rag.domain.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户跨会话画像实体 (L3)。
 * <p>对应 user_memory_profile 表, 每用户唯一一条结构化画像。
 * <p>由 L2 长期事实 (ES jc_chat_memory) 周期性聚合并经 LLM 提炼生成。
 * <p>字段说明: profileJson 必填且非空 JSON (含五段: domain / role / skills / preferences / goals);
 * <p>version 每次更新 +1; factCount 记录本次提取引用的 L2 fact 数; lastExtractedAt 为最近一次成功提取时间。
 *
 * @author JC
 */
@TableName("user_memory_profile")
public class UserMemoryProfileEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID (唯一) */
    @TableField("user_id")
    private Long userId;

    /** 结构化画像 JSON 字符串 (内含 domain/role/skills/preferences/goals 五段) */
    @TableField("profile_json")
    private String profileJson;

    /** 画像版本 (每次更新 +1) */
    @TableField("version")
    private Integer version;

    /** 本次提取引用的 L2 fact 数 */
    @TableField("fact_count")
    private Integer factCount;

    /** 生成画像使用的模型名 (用于排查/审计) */
    @TableField("model_name")
    private String modelName;

    /** 最近一次成功提取时间 */
    @TableField("last_extracted_at")
    private LocalDateTime lastExtractedAt;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { userId = v; }
    public String getProfileJson() { return profileJson; }
    public void setProfileJson(String v) { profileJson = v; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer v) { version = v; }
    public Integer getFactCount() { return factCount; }
    public void setFactCount(Integer v) { factCount = v; }
    public String getModelName() { return modelName; }
    public void setModelName(String v) { modelName = v; }
    public LocalDateTime getLastExtractedAt() { return lastExtractedAt; }
    public void setLastExtractedAt(LocalDateTime v) { lastExtractedAt = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
