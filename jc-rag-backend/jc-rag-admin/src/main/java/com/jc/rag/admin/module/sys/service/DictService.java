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


package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.DictItemCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictQueryParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeQueryParam;
import com.jc.rag.admin.module.sys.model.vo.DictItemVO;
import com.jc.rag.admin.module.sys.model.vo.DictTypeVO;
import com.jc.rag.common.web.page.PageResponse;

/**
 * 字典管理 Service
 * @author JC
 */
public interface DictService {

    PageResponse<DictTypeVO> pageDictTypes(DictTypeQueryParam param);

    PageResponse<DictItemVO> pageDictItems(DictQueryParam param);

    Long createDictType(DictTypeCreateParam param);

    Long createDictItem(DictItemCreateParam param);
}
