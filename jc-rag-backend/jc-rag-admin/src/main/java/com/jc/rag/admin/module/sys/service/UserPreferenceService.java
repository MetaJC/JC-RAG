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

import com.jc.rag.admin.module.sys.model.param.UserPreferenceUpdateParam;
import com.jc.rag.admin.module.sys.model.vo.UserPreferenceVO;

/**
 * 当前用户偏好 Service。
 * <p>底层复用 sys_config 表, config_group = 'user_pref', config_key = '{userId}.{preferenceKey}'。
 *
 * @author JC
 */
public interface UserPreferenceService {

    /** 加载当前用户全部偏好 (含默认值填充) */
    UserPreferenceVO load(Long userId);

    /**
     * 更新当前用户偏好 (按 param 中非空字段逐项更新, null 字段不动)。
     * @return 更新后的完整 VO
     */
    UserPreferenceVO update(Long userId, UserPreferenceUpdateParam param);

    /** 记忆文件路径默认: ~/.jc/jc-rag/memory/user_profile.md */
    String defaultMemoryFilePath(Long userId);
}
