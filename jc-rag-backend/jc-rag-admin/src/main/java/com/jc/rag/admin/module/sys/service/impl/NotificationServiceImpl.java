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


package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.NotificationQueryParam;
import com.jc.rag.admin.module.sys.model.param.NotificationSendParam;
import com.jc.rag.admin.module.sys.model.vo.NotificationVO;
import com.jc.rag.admin.module.sys.service.NotificationService;
import com.jc.rag.common.web.page.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 系统通知实现 - TODO 业务待实现
 * @author JC
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    // TODO: 业务待实现 - 接入通知表 sys_notification + 站内信/邮件/短信通道

    @Override
    public PageResponse<NotificationVO> pageNotifications(NotificationQueryParam param) {
        return PageResponse.of(Collections.<NotificationVO>emptyList(), 0L, param.getPageNum(), param.getPageSize());
    }

    @Override
    public boolean markRead(Long id) {
        // TODO: 业务待实现
        log.info("[TODO] markRead: {}", id);
        return true;
    }

    @Override
    public Long send(NotificationSendParam param) {
        // TODO: 业务待实现
        log.info("[TODO] send notification: {} -> {}", param.getTitle(), param.getTargetUserType());
        return 0L;
    }
}
