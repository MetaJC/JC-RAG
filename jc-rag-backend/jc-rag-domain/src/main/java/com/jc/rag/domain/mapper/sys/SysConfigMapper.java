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


package com.jc.rag.domain.mapper.sys;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.sys.SysConfigEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统运行时配置 Mapper。
 *
 * @author JC
 */
public interface SysConfigMapper extends BaseMapper<SysConfigEntity> {

    /** 全表加载 key→value 映射 (RuntimeConfigService 启动加载使用) */
    List<Map<String, Object>> listAllKeyValue();

    /** 按 config_key 查单条 config_value */
    String selectStringValue(@Param("configKey") String configKey);

    /** UPSERT 单条配置 */
    int upsertConfig(@Param("configGroup") String configGroup,
                     @Param("configKey") String configKey,
                     @Param("configValue") String configValue,
                     @Param("valueType") String valueType,
                     @Param("encrypted") Integer encrypted,
                     @Param("remark") String remark);
}
