package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.sys.model.param.SystemSettingParam;
import com.jc.rag.admin.module.sys.model.vo.SystemSettingVO;
import com.jc.rag.admin.module.sys.service.SysSettingService;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置 Controller (系统管理 → 系统设置页).
 * <p>路径: /api/sys/settings; 设置项落 sys_config 表,
 * 用户端登录页经 /api/auth/site-config 读取生效.
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/sys/settings")
@RequiredArgsConstructor
public class SysSettingController {

    private final SysSettingService sysSettingService;

    @GetMapping
    public R<SystemSettingVO> get() {
        return R.ok(sysSettingService.getSettings());
    }

    @PutMapping
    @OperationLog(module = "SYS", action = "UPDATE_SYS_SETTINGS", description = "更新系统设置")
    public R<SystemSettingVO> save(@Valid @RequestBody SystemSettingParam param) {
        return R.ok(sysSettingService.saveSettings(param));
    }
}
