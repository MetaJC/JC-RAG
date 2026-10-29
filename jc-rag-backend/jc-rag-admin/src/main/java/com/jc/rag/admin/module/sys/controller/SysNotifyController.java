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

import com.jc.rag.admin.module.sys.model.param.NotificationQueryParam;
import com.jc.rag.admin.module.sys.model.param.NotificationSendParam;
import com.jc.rag.admin.module.sys.model.vo.NotificationVO;
import com.jc.rag.admin.module.sys.service.NotificationService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知 Controller (统一命名: SysNotifyController)。
 * <p>路径: /api/sys/notify, 与 rbac/rag 范式一致; 老路径 /api/sys/notification 通过兼容 Controller 保留。
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/notify")
@RequiredArgsConstructor
public class SysNotifyController {

    private final NotificationService notificationService;

    @GetMapping
    public R<PageResponse<NotificationVO>> page(NotificationQueryParam param) {
        return R.ok(notificationService.pageNotifications(param));
    }

    @PostMapping("/{id}/read")
    public R<Boolean> markRead(@PathVariable Long id) {
        return R.ok(notificationService.markRead(id));
    }

    @PostMapping("/send")
    public R<Long> send(@Valid @RequestBody NotificationSendParam param) {
        return R.ok(notificationService.send(param));
    }
}
