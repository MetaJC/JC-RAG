package com.jc.rag.admin.module.security.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.security.SensitiveFilterEngine;
import com.jc.rag.common.security.SensitiveFilterResult;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.entity.sys.SysSensitiveWordEntity;
import com.jc.rag.admin.module.security.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 敏感词管理 Controller。
 * <ul>
 *   <li>/api/admin/security/words/** : Admin 端 CRUD, 需登录, 写操作需权限 code</li>
 *   <li>/api/internal/security/words/export : 内部接口, 供 Python 拉取, 需固定 Token</li>
 * </ul>
 *
 * @author JC
 */
@RestController
@RequestMapping("/api")
public class SensitiveWordController {

    private final SensitiveWordService service;

    @Autowired
    public SensitiveWordController(SensitiveWordService service) {
        this.service = service;
    }

    // ============================== Admin CRUD ==============================

    @GetMapping("/admin/security/words")
    @PreAuthorize("hasAuthority('security:word:list')")
    public R<IPage<SysSensitiveWordEntity>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String riskLevel) {
        return R.ok(service.page(page, size, keyword, riskLevel));
    }

    @PostMapping("/admin/security/words")
    @PreAuthorize("hasAuthority('security:word:create')")
    public R<SysSensitiveWordEntity> create(@RequestBody SysSensitiveWordEntity entity, Authentication auth) {
        Long uid = currentUserId(auth);
        return R.ok(service.create(entity, uid));
    }

    @PutMapping("/admin/security/words")
    @PreAuthorize("hasAuthority('security:word:update')")
    public R<SysSensitiveWordEntity> update(@RequestBody SysSensitiveWordEntity entity) {
        return R.ok(service.update(entity));
    }

    @DeleteMapping("/admin/security/words/{id}")
    @PreAuthorize("hasAuthority('security:word:delete')")
    public R<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return R.ok();
    }

    @PostMapping("/admin/security/words/{id}/toggle")
    @PreAuthorize("hasAuthority('security:word:update')")
    public R<Void> toggle(@PathVariable Long id) {
        service.toggleEnabled(id);
        return R.ok();
    }

    /**
     * 批量导入 (按行 word|risk|action|category 解析)。
     */
    @PostMapping("/admin/security/words/batch")
    @PreAuthorize("hasAuthority('security:word:create')")
    public R<Map<String, Object>> batchImport(@RequestBody Map<String, Object> body, Authentication auth) {
        @SuppressWarnings("unchecked")
        List<String> lines = (List<String>) body.getOrDefault("lines", Collections.emptyList());
        Long uid = currentUserId(auth);
        int count = service.batchImport(lines, uid);
        return R.ok(Map.of("imported", count));
    }

    // ============================== Internal: 供 Python 拉取 ==============================

    /**
     * 供 Python jc-rag-service 拉取敏感词使用。
     * <p>鉴权: 通过 jc-rag.security.internal-token 配置 (Header: X-Internal-Token), 避免外部直接访问。
     */
    @GetMapping("/internal/security/words/export")
    public R<List<SysSensitiveWordEntity>> exportEnabled() {
        return R.ok(service.exportEnabled());
    }

    private Long currentUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof DbUserDetails u) {
            return u.getUserId();
        }
        return null;
    }
}
