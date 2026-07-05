package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.sys.model.param.RuntimeConfigUpdateParam;
import com.jc.rag.admin.module.sys.model.vo.RuntimeConfigVO;
import com.jc.rag.admin.module.sys.service.RuntimeConfigService;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 运行时配置 Controller (A 端管理后台 + 系统设置页)。
 * <p>路径: /api/sys/runtime-config
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/runtime-config")
@RequiredArgsConstructor
public class SysConfigController {

    private final RuntimeConfigService runtimeConfigService;

    @GetMapping
    public R<RuntimeConfigVO> get() {
        return R.ok(runtimeConfigService.buildResponse());
    }

    @PostMapping
    @OperationLog(module = "SYS", action = "UPDATE_RUNTIME_CONFIG", description = "更新运行时配置")
    public R<RuntimeConfigVO> update(@RequestBody @Valid RuntimeConfigUpdateParam param) {
        return R.ok(runtimeConfigService.saveRuntimeConfig(param));
    }
}
