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


package com.jc.rag.user.module.user.service;

import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.user.module.user.model.param.UserApiKeySaveParam;
import com.jc.rag.user.module.user.model.vo.UserApiKeyVO;

import java.util.List;

/**
 * 用户 API Key 业务接口
 *
 * @author JC
 */
public interface UserApiKeyService {

    /**
     * 查询当前用户的所有 API Key
     */
    List<UserApiKeyVO> listByUserId(Long userId);

    /**
     * 新增或更新 API Key（按 provider 维度）
     */
    BaseOpResultVO saveOrUpdate(Long userId, UserApiKeySaveParam param);

    /**
     * 删除 API Key（限定当前用户）
     */
    BaseOpResultVO remove(Long userId, Long id);

    /**
     * 启用/禁用 API Key
     */
    BaseOpResultVO toggle(Long userId, Long id, boolean active);
}
