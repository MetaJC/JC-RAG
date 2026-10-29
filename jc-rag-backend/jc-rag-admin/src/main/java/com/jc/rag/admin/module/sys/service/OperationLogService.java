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


package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.OperationLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.OperationLogVO;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import com.jc.rag.common.web.page.PageResponse;

/**
 * 操作日志 Service。
 * <p>供 AOP 切面及业务代码异步记录操作日志；实现位于 {@code impl/OperationLogServiceImpl}。
 *
 * @author JC
 */
public interface OperationLogService {

    /** 异步持久化单条日志（AOP 切面调用） */
    void saveAsync(SysOperationLog entity);

    /** 异步记录一条简化日志（业务代码便捷调用） */
    void log(String module, String action, String description, String status);

    /** 分页查询操作日志 (前端管理页使用) */
    PageResponse<OperationLogVO> pageList(OperationLogQueryParam filter);
}
