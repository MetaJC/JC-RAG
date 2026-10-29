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

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统操作日志 Mapper
 * @author JC
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 条件计数
     */
    long countOperationLogs(@Param("moduleName") String moduleName,
                            @Param("operationType") String operationType,
                            @Param("username") String username,
                            @Param("status") String status,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    /**
     * 条件分页查询
     */
    IPage<SysOperationLog> pageOperationLogs(IPage<SysOperationLog> page,
                                             @Param("moduleName") String moduleName,
                                             @Param("operationType") String operationType,
                                             @Param("username") String username,
                                             @Param("status") String status,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 全量导出 (无分页)
     */
    List<SysOperationLog> listOperationLogs(@Param("moduleName") String moduleName,
                                             @Param("operationType") String operationType,
                                             @Param("username") String username,
                                             @Param("status") String status,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);
}
