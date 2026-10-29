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


package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.DictItemCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictQueryParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeQueryParam;
import com.jc.rag.admin.module.sys.model.vo.DictItemVO;
import com.jc.rag.admin.module.sys.model.vo.DictTypeVO;
import com.jc.rag.admin.module.sys.service.DictService;
import com.jc.rag.common.web.page.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 字典管理实现 - TODO 业务待实现
 * @author JC
 */
@Slf4j
@Service
public class DictServiceImpl implements DictService {

    // TODO: 业务待实现 - 接入 sys_dict_type / sys_dict_item 表

    @Override
    public PageResponse<DictTypeVO> pageDictTypes(DictTypeQueryParam param) {
        return PageResponse.of(Collections.<DictTypeVO>emptyList(), 0L, param.getPageNum(), param.getPageSize());
    }

    @Override
    public PageResponse<DictItemVO> pageDictItems(DictQueryParam param) {
        return PageResponse.of(Collections.<DictItemVO>emptyList(), 0L, param.getPageNum(), param.getPageSize());
    }

    @Override
    public Long createDictType(DictTypeCreateParam param) {
        // TODO: 业务待实现
        log.info("[TODO] createDictType: {}", param.getTypeCode());
        return 0L;
    }

    @Override
    public Long createDictItem(DictItemCreateParam param) {
        // TODO: 业务待实现
        log.info("[TODO] createDictItem: {}.{}", param.getTypeCode(), param.getItemCode());
        return 0L;
    }
}
