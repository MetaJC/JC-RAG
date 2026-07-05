package com.jc.rag.admin.module.knowledge.controller;

import com.jc.rag.admin.module.knowledge.model.param.KbAuthSaveParam;
import com.jc.rag.admin.module.knowledge.model.vo.KbAuthDetailVO;
import com.jc.rag.admin.module.knowledge.service.KbAuthService;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库授权管理接口。
 * <p>对 knowledge-base 业务模块的二级入口, 路径仍挂载在 /api/knowledge-bases/{kbId}/auth,
 * <p>避免对一级 menu 造成结构破坏。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/knowledge-bases/{kbId}/auth")
public class KbAuthController {

    private final KbAuthService kbAuthService;

    public KbAuthController(KbAuthService kbAuthService) {
        this.kbAuthService = kbAuthService;
    }

    /** 获取知识库授权详情 (全量角色 + 全量 ADMIN 用户 + 当前选中状态) */
    @GetMapping
    public R<KbAuthDetailVO> getAuthDetail(@PathVariable Long kbId) {
        return R.ok(kbAuthService.getAuthDetail(kbId));
    }

    /** 全量替换保存知识库授权 */
    @PostMapping
    public R<Void> saveAuth(@PathVariable Long kbId, @Valid @RequestBody KbAuthSaveParam param) {
        kbAuthService.saveAuth(kbId, param);
        return R.ok();
    }
}
