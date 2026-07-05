package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.sys.model.param.DemoConfigUpdateParam;
import com.jc.rag.admin.module.sys.model.param.RoleQuotaSaveParam;
import com.jc.rag.admin.module.sys.model.vo.DemoConfigVO;
import com.jc.rag.admin.module.sys.model.vo.QuotaCheckVO;
import com.jc.rag.admin.module.sys.model.vo.RoleQuotaVO;
import com.jc.rag.admin.module.sys.service.SysDemoModeService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 演示模式 / 角色配额 Controller。
 * <p>A 端管理: /api/sys/demo-config, /api/sys/role-quotas
 * <p>C 端用户: /api/sys/quota/check
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys")
@RequiredArgsConstructor
public class SysDemoModeController {

    private final SysDemoModeService sysDemoModeService;

    /* ============== A 端管理 ============== */

    @GetMapping("/demo-config")
    public R<DemoConfigVO> getDemoConfig() {
        return R.ok(sysDemoModeService.getDemoConfig());
    }

    @PutMapping("/demo-config")
    @OperationLog(module = "SYS", action = "UPDATE_DEMO_CONFIG", description = "更新演示模式配置")
    public R<DemoConfigVO> updateDemoConfig(@RequestBody @Valid DemoConfigUpdateParam request) {
        return R.ok(sysDemoModeService.updateDemoConfig(request));
    }

    @GetMapping("/role-quotas")
    public R<List<RoleQuotaVO>> listRoleQuotas() {
        return R.ok(sysDemoModeService.listRoleQuotas());
    }

    @PostMapping("/role-quotas")
    @OperationLog(module = "SYS", action = "SAVE_ROLE_QUOTA", description = "保存角色配额")
    public R<BaseOpResultVO> saveRoleQuota(@RequestBody @Valid RoleQuotaSaveParam request) {
        return R.ok(sysDemoModeService.saveRoleQuota(request));
    }

    /* ============== C 端用户 ============== */

    @GetMapping("/quota/check")
    public R<QuotaCheckVO> checkQuota(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DbUserDetails u)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return R.ok(sysDemoModeService.checkQuota(u.getUserId()));
    }
}
