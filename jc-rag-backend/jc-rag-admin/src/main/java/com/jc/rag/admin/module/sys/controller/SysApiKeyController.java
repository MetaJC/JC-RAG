package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.sys.model.param.ApiKeySaveParam;
import com.jc.rag.admin.module.sys.model.param.ApiKeyToggleParam;
import com.jc.rag.admin.module.sys.model.vo.ApiKeyVO;
import com.jc.rag.admin.module.sys.service.SysApiKeyService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户自定义 API Key Controller (C 端个人中心)。
 * <p>路径: /api/sys/api-keys
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/api-keys")
@RequiredArgsConstructor
public class SysApiKeyController {

    private final SysApiKeyService sysApiKeyService;

    @GetMapping
    public R<List<ApiKeyVO>> list(
            Authentication auth,
            @RequestParam(required = false) String keyword) {
        return R.ok(sysApiKeyService.listUserApiKeys(currentUserId(auth), keyword));
    }

    @GetMapping("/active")
    public R<ApiKeyVO> getActive(
            @RequestParam(defaultValue = "openai") String provider,
            Authentication auth) {
        return R.ok(sysApiKeyService.getActiveApiKey(currentUserId(auth), provider));
    }

    @PostMapping
    @OperationLog(module = "SYS", action = "SAVE_API_KEY", description = "保存用户 API Key")
    public R<ApiKeyVO> save(@RequestBody ApiKeySaveParam param, Authentication auth) {
        return R.ok(sysApiKeyService.saveApiKey(currentUserId(auth), param));
    }

    @PostMapping("/{id}/toggle")
    @OperationLog(module = "SYS", action = "TOGGLE_API_KEY", description = "切换 API Key 启用状态")
    public R<BaseOpResultVO> toggle(@PathVariable Long id, @RequestBody ApiKeyToggleParam param) {
        return R.ok(sysApiKeyService.toggleApiKey(id, param.getActive() != null && param.getActive()));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "SYS", action = "DELETE_API_KEY", description = "删除 API Key")
    public R<BaseOpResultVO> delete(@PathVariable Long id) {
        return R.ok(sysApiKeyService.deleteApiKey(id));
    }

    private static Long currentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof DbUserDetails u)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return u.getUserId();
    }
}
