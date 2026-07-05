package com.jc.rag.admin.module.user.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.user.model.param.UserCustomAgentQueryParam;
import com.jc.rag.admin.module.user.model.vo.UserCustomAgentAdminVO;
import com.jc.rag.admin.module.user.service.AdminUserCustomAgentService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin 端用户智能体 Controller.
 * <p>用途: 后台审计/管理/查看 C 端用户自建的智能体 (只读 + 强制删除).
 * <p>规则: Admin 不可编辑用户数据 (内容归用户所有); 如需干预, 仅做强制删除并附审计日志.
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/user-custom-agent")
@RequiredArgsConstructor
public class AdminUserCustomAgentController {

    private final AdminUserCustomAgentService adminUserCustomAgentService;

    @GetMapping
    public R<PageResponse<UserCustomAgentAdminVO>> list(UserCustomAgentQueryParam query) {
        return R.ok(adminUserCustomAgentService.pageList(query));
    }

    @GetMapping("/{id}")
    public R<UserCustomAgentAdminVO> get(@PathVariable Long id) {
        return R.ok(adminUserCustomAgentService.getById(id));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "USER_AGENT", action = "FORCE_DELETE", description = "Admin 强制删除用户智能体")
    public R<Boolean> forceDelete(@PathVariable Long id) {
        return R.ok(adminUserCustomAgentService.forceDelete(id));
    }

    /**
     * 启用/禁用用户智能体 (admin 端审计性管控, 不修改内容).
     * <p>请求体: {"status": 0|1}
     */
    @PutMapping("/{id}/status")
    @OperationLog(module = "USER_AGENT", action = "UPDATE_STATUS", description = "Admin 启停用户智能体")
    public R<Boolean> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return R.ok(adminUserCustomAgentService.updateStatus(id, body == null ? null : body.get("status")));
    }
}
