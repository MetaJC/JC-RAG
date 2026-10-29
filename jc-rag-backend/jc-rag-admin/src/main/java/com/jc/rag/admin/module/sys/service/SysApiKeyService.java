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

import com.jc.rag.admin.module.sys.model.param.ApiKeySaveParam;
import com.jc.rag.admin.module.sys.model.vo.ApiKeyVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;

import java.util.List;

/**
 * 用户自定义 API Key Service。
 * @author JC
 */
public interface SysApiKeyService {

    /** 列出当前用户全部 API Key. */
    List<ApiKeyVO> listUserApiKeys(Long userId, String keyword);

    /** 取当前用户指定 provider 的启用中的 API Key. */
    ApiKeyVO getActiveApiKey(Long userId, String provider);

    /** 新增/更新 (按 userId+provider 唯一). */
    ApiKeyVO saveApiKey(Long userId, ApiKeySaveParam param);

    /** 切换启用状态. */
    BaseOpResultVO toggleApiKey(Long id, boolean active);

    /** 删除. */
    BaseOpResultVO deleteApiKey(Long id);
}
