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


package com.jc.rag.domain.mapper.rag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rag.RagPromptEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Prompt Mapper。
 * @author JC
 */
public interface RagPromptMapper extends BaseMapper<RagPromptEntity> {

    long countPrompts(@Param("scope") String scope, @Param("keyword") String keyword);

    IPage<RagPromptEntity> pagePrompts(IPage<RagPromptEntity> page,
                                       @Param("scope") String scope,
                                       @Param("keyword") String keyword);

    List<RagPromptEntity> listPrompts(
            @Param("scope") String scope,
            @Param("keyword") String keyword
    );

    Map<String, Object> getPrompt(@Param("id") Long id);

    int insertPrompt(Map<String, Object> row);

    int updatePrompt(@Param("id") Long id, @Param("row") Map<String, Object> row);

    int deletePrompt(@Param("id") Long id);
}
