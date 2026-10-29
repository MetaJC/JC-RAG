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
