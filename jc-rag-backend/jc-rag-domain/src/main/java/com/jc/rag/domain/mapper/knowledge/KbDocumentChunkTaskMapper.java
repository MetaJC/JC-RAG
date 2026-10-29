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


package com.jc.rag.domain.mapper.knowledge;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbDocumentChunkTaskEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 文档切块任务 Mapper。
 * @author JC
 */
public interface KbDocumentChunkTaskMapper extends BaseMapper<KbDocumentChunkTaskEntity> {

    /**
     * 插入初始任务
     */
    int insertInitial(@Param("documentId") Long documentId,
                      @Param("taskType") String taskType,
                      @Param("taskStatus") String taskStatus);

    /**
     * UPSERT: 重置任务 (retry_count+1, 清空时间)
     */
    int resetForReprocess(@Param("documentId") Long documentId,
                          @Param("taskType") String taskType,
                          @Param("taskStatus") String taskStatus);

    /**
     * 标记任务运行中
     */
    int markRunning(@Param("documentId") Long documentId);

    /**
     * 完成任务 (SUCCESS / FAILED)
     */
    int complete(@Param("documentId") Long documentId,
                 @Param("status") String status,
                 @Param("errorMessage") String errorMessage);

    /**
     * 查询任务当前状态
     */
    String selectTaskStatus(@Param("documentId") Long documentId);
}
