package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.sys.model.param.ModelProviderSaveParam;
import com.jc.rag.admin.module.sys.model.param.ModelProviderTestParam;
import com.jc.rag.admin.module.sys.service.ModelProviderConfigService;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/model/providers")
@RequiredArgsConstructor
public class ModelProviderConfigController {
    private final ModelProviderConfigService service;
    @GetMapping public R<?> list() { return R.ok(service.list()); }
    @PostMapping @OperationLog(module = "MODEL", action = "SAVE_PROVIDER", description = "保存模型服务商配置")
    public R<?> save(@RequestBody @Valid ModelProviderSaveParam param) { return R.ok(service.save(param)); }
    @DeleteMapping("/{id}") @OperationLog(module = "MODEL", action = "DELETE_PROVIDER", description = "删除模型服务商配置")
    public R<Void> delete(@PathVariable Long id) { service.delete(id); return R.ok(); }
    @PostMapping("/{id}/models/refresh") public R<?> refreshModels(@PathVariable Long id) { return R.ok(service.refreshModels(id)); }
    @PostMapping("/test") public R<?> test(@RequestBody @Valid ModelProviderTestParam param) { return R.ok(service.testConnection(param)); }
    @PostMapping("/{id}/activate") @OperationLog(module = "MODEL", action = "ACTIVATE_PROVIDER", description = "启用主模型服务")
    public R<Void> activate(@PathVariable Long id) { service.activate(id); return R.ok(); }
}
