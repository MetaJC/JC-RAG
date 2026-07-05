package com.jc.rag.admin.module.rag.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.rag.model.param.RagAgentQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagAgentSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagAgentVO;
import com.jc.rag.admin.module.rag.service.RagAgentService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Agent 管理 Controller (A 端管理后台)。
 * <p>路径: /api/rag/agent, 与 rbac 范式一致 (类级 + 资源名单数)。
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rag/agent")
@RequiredArgsConstructor
public class RagAgentController {

    private final RagAgentService ragAgentService;

    @GetMapping
    public R<PageResponse<RagAgentVO>> list(RagAgentQueryParam query) {
        return R.ok(ragAgentService.pageList(query));
    }

    @GetMapping("/{id}")
    public R<RagAgentVO> get(@PathVariable Long id) {
        return R.ok(ragAgentService.getById(id));
    }

    @PostMapping
    @OperationLog(module = "RAG", action = "CREATE_AGENT", description = "创建 Agent")
    public R<Long> create(@RequestBody @Valid RagAgentSaveParam param, Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof DbUserDetails u ? u.getUserId() : null;
        return R.ok(ragAgentService.create(param, userId));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "RAG", action = "UPDATE_AGENT", description = "更新 Agent")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RagAgentSaveParam param) {
        return R.ok(ragAgentService.update(id, param));
    }

    @PostMapping("/{id}/publish")
    @OperationLog(module = "RAG", action = "PUBLISH_AGENT", description = "发布 Agent")
    public R<Boolean> publish(@PathVariable Long id) {
        return R.ok(ragAgentService.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    @OperationLog(module = "RAG", action = "UNPUBLISH_AGENT", description = "下线 Agent")
    public R<Boolean> unpublish(@PathVariable Long id) {
        return R.ok(ragAgentService.unpublish(id));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "RAG", action = "DELETE_AGENT", description = "删除 Agent")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(ragAgentService.delete(id));
    }

    /** 兼容旧前端: 直接返回 Map (含 createdAt/publishedAt/skillIds) */
    @GetMapping("/legacy")
    public R<PageResponse<Map<String, Object>>> legacy(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(ragAgentService.pageListAsMap(keyword, status, PageRequest.of(page, size)));
    }

    /**
     * 设置系统默认 Agent (全局唯一). 用户端在未指定 agentId 时强制使用.
     * <p>写入 sys_config: config_group=AGENT, config_key=default_agent_id, config_value=agentId.
     * <p>同一时刻仅允许一个默认 Agent, 设置新值时自动覆盖旧值.
     */
    @PostMapping("/{id}/set-default")
    @OperationLog(module = "RAG", action = "SET_DEFAULT_AGENT", description = "设置默认 Agent")
    public R<Boolean> setDefault(@PathVariable Long id) {
        return R.ok(ragAgentService.setDefault(id));
    }

    /**
     * 取消系统默认 Agent 标识 (清空 default_agent_id 配置, 回到"无默认"状态).
     */
    @PostMapping("/unset-default")
    @OperationLog(module = "RAG", action = "UNSET_DEFAULT_AGENT", description = "取消默认 Agent")
    public R<Boolean> unsetDefault() {
        return R.ok(ragAgentService.unsetDefault());
    }

    /**
     * 查询当前系统默认 Agent ID (无默认返回 null).
     */
    @GetMapping("/default")
    public R<Long> getDefault() {
        return R.ok(ragAgentService.getDefaultId());
    }
}
