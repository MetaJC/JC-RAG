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
