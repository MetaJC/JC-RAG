package com.jc.rag.admin.module.knowledge.controller;

import com.jc.rag.admin.module.knowledge.model.vo.KnowledgeBaseVO;
import com.jc.rag.admin.module.knowledge.service.KnowledgeBaseService;
import com.jc.rag.common.web.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统级知识库配置接口 (C 端默认知识库)。
 * <p>仅超管 / developer 可访问。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/admin/system/kb-default")
public class SystemKbDefaultController {

    private final KnowledgeBaseService knowledgeBaseService;

    public SystemKbDefaultController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /** 获取当前 C 端默认知识库 */
    @GetMapping
    public R<KnowledgeBaseVO> get() {
        return R.ok(knowledgeBaseService.getDefaultKnowledgeBase());
    }

    /** 设置 C 端默认知识库 */
    @PutMapping
    public R<KnowledgeBaseVO> set(@RequestParam Long kbId) {
        return R.ok(knowledgeBaseService.setDefaultKnowledgeBase(kbId));
    }

    /** 清除 C 端默认知识库 */
    @DeleteMapping
    public R<Void> clear() {
        knowledgeBaseService.clearDefaultKnowledgeBase();
        return R.ok();
    }
}
