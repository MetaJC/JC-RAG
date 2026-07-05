package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.NotificationQueryParam;
import com.jc.rag.admin.module.sys.model.param.NotificationSendParam;
import com.jc.rag.admin.module.sys.model.vo.NotificationVO;
import com.jc.rag.common.web.page.PageResponse;

/**
 * 系统通知 Service
 * @author JC
 */
public interface NotificationService {

    /** 分页查询通知 */
    PageResponse<NotificationVO> pageNotifications(NotificationQueryParam param);

    /**
     * 标记已读
     * @return true=成功 false=失败
     */
    boolean markRead(Long id);

    /** 发送通知 */
    Long send(NotificationSendParam param);
}
