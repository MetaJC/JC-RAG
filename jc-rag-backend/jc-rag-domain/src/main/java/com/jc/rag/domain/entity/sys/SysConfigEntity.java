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


package com.jc.rag.domain.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 系统运行时配置 实体。
 * <p>对应 sys_config 表, 存储可热更新的运行期配置 (configGroup/configKey/configValue 三元组)。
 *
 * @author JC
 */
@TableName("sys_config")
public class SysConfigEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 配置分组 (如 LLM / OSS / SECURITY)
     */
    @TableField("config_group")
    private String configGroup;
    /**
     * 配置 Key (组内唯一)
     */
    @TableField("config_key")
    private String configKey;
    /**
     * 配置 Value (字符串, 由 valueType 决定解析方式)
     */
    @TableField("config_value")
    private String configValue;
    /**
     * Value 类型 (STRING / NUMBER / BOOLEAN / JSON)
     */
    @TableField("value_type")
    private String valueType;
    /**
     * 是否加密存储 (1 加密 / 0 明文)
     */
    private Integer encrypted;
    /**
     * 备注说明
     */
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String v) {
        configGroup = v;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String v) {
        configKey = v;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String v) {
        configValue = v;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String v) {
        valueType = v;
    }

    public Integer getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Integer v) {
        encrypted = v;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String v) {
        remark = v;
    }
}
