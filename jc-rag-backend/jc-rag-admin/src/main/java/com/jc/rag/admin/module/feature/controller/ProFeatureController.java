package com.jc.rag.admin.module.feature.controller;

import com.jc.rag.common.feature.ProAnalysisResult;
import com.jc.rag.common.feature.ProFeatureService;
import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 高级问答分析 Controller (公开入口, 开源 / 商业版均存在).
 * <p>
 * 调用方无需关心当前运行的是开源版还是商业版, 由 Spring 自动注入正确的实现 Bean.
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/admin/pro-feature")
@RequiredArgsConstructor
public class ProFeatureController {

    private final ProFeatureService proFeatureService;

    /** 高级问答分析. 开源版返回占位结果; 商业版返回增强分析. */
    @GetMapping("/analyze")
    public R<ProAnalysisResult> analyze(@RequestParam(required = false) Long kbId) {
        return R.ok(proFeatureService.analyze(kbId));
    }
}
