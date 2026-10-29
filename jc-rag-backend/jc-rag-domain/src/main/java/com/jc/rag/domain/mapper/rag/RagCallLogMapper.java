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
import com.jc.rag.domain.entity.rag.RagCallLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * RAG 调用日志 Mapper
 * @author JC
 */
@Mapper
public interface RagCallLogMapper extends BaseMapper<RagCallLog> {

    /**
     * 条件分页查询 (JOIN rbac_user 取 userName, JOIN chat_session 取 sessionName)
     */
    IPage<Map<String, Object>> pageRagCallLogs(IPage<Map<String, Object>> page,
                                               @Param("keyword") String keyword,
                                               @Param("modelName") String modelName,
                                               @Param("userName") String userName);

    /** 全量导出 */
    List<Map<String, Object>> listRagCallLogs(
            @Param("keyword") String keyword,
            @Param("modelName") String modelName,
            @Param("userName") String userName
    );

    /**
     * 条件计数
     */
    long countRagCallLogs(
            @Param("keyword") String keyword,
            @Param("modelName") String modelName,
            @Param("userName") String userName
    );
}
