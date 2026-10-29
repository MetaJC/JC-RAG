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


package com.jc.rag.admin.module.edition.controller;

import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 版本信息 Controller (开源 / 商业版均存在).
 * <p>
 * 前端启动时调用此接口校验构建期版本与运行期后端版本是否一致.
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/admin/edition")
@RequiredArgsConstructor
public class EditionController {

    @Value("${jc-rag.edition:oss}")
    private String edition;

    @Value("${jc-rag.edition-label:Community Edition}")
    private String editionLabel;

    /** 返回当前后端运行版本. */
    @GetMapping
    public R<Map<String, String>> getEdition() {
        Map<String, String> data = new HashMap<>();
        data.put("edition", edition);
        data.put("label", editionLabel);
        return R.ok(data);
    }
}
