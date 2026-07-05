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
