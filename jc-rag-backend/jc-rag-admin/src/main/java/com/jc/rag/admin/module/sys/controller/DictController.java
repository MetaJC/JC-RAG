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


package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.module.sys.model.param.DictItemCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictQueryParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeQueryParam;
import com.jc.rag.admin.module.sys.model.vo.DictItemVO;
import com.jc.rag.admin.module.sys.model.vo.DictTypeVO;
import com.jc.rag.admin.module.sys.service.DictService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 字典管理 Controller - TODO 业务待实现
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/types")
    public R<PageResponse<DictTypeVO>> pageTypes(DictTypeQueryParam param) {
        return R.ok(dictService.pageDictTypes(param));
    }

    @GetMapping("/items")
    public R<PageResponse<DictItemVO>> pageItems(DictQueryParam param) {
        return R.ok(dictService.pageDictItems(param));
    }

    @PostMapping("/types")
    public R<Long> createType(@RequestBody DictTypeCreateParam param) {
        return R.ok(dictService.createDictType(param));
    }

    @PostMapping("/items")
    public R<Long> createItem(@RequestBody DictItemCreateParam param) {
        return R.ok(dictService.createDictItem(param));
    }
}
