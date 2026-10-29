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


package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.module.sys.model.param.OperationLogQueryParam;
import com.jc.rag.admin.module.sys.model.vo.OperationLogVO;
import com.jc.rag.admin.module.sys.service.OperationLogService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询 Controller (兼容层)。
 * <p>新统一入口为 {@code SysLogController} (路径: /api/sys/log/operation); 本 Controller 保留旧路径以保证前端平滑过渡。
 * @deprecated 请迁移至 {@code SysLogController}。
 * @author JC
 */
@Slf4j
@Deprecated
@RestController
@RequestMapping("/api/sys/operation-log")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    public R<PageResponse<OperationLogVO>> page(OperationLogQueryParam filter) {
        return R.ok(operationLogService.pageList(filter));
    }
}
