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
