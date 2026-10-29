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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.admin.module.sys.model.param.UserPreferenceUpdateParam;
import com.jc.rag.admin.module.sys.model.vo.UserPreferenceVO;
import com.jc.rag.admin.module.sys.service.UserPreferenceService;
import com.jc.rag.domain.entity.sys.SysConfigEntity;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * UserPreferenceService 实现: 复用 sys_config 表, 避免新增表。
 * <p>存储约定:
 * <ul>
 *   <li>config_group = 'user_pref'</li>
 *   <li>config_key = '{userId}.{preferenceKey}' (如 '1001.memoryFilePath')</li>
 *   <li>config_value = 偏好值字符串</li>
 *   <li>value_type = STRING / NUMBER / BOOLEAN</li>
 * </ul>
 * <p>读取走 QueryWrapper 按 (group, key) 精准匹配, 不依赖 SysConfigMapper.selectStringValue
 * (后者不传 group, 跨分组时存在误命中风险)。
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private static final String GROUP = "user_pref";

    /** 偏好项 Key 常量 (作为 config_key 的后缀) */
    private static final String K_MEMORY_FILE_PATH = "memoryFilePath";

    private static final String REMARK_MEMORY_FILE_PATH = "用户偏好 - 长记忆文件路径 (Markdown)";

    private final SysConfigMapper sysConfigMapper;

    @Override
    public UserPreferenceVO load(Long userId) {
        UserPreferenceVO vo = new UserPreferenceVO();
        vo.setMemoryFilePath(
                readString(userId, K_MEMORY_FILE_PATH, defaultMemoryFilePath(userId))
        );
        return vo;
    }

    @Override
    public UserPreferenceVO update(Long userId, UserPreferenceUpdateParam param) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        if (param == null) {
            return load(userId);
        }
        if (param.getMemoryFilePath() != null && !param.getMemoryFilePath().isBlank()) {
            String trimmed = param.getMemoryFilePath().trim();
            writeString(userId, K_MEMORY_FILE_PATH, trimmed, REMARK_MEMORY_FILE_PATH);
            log.info("用户偏好更新: userId={}, memoryFilePath={}", userId, trimmed);
        }
        return load(userId);
    }

    @Override
    public String defaultMemoryFilePath(Long userId) {
        return "~/.jc/jc-rag/memory/user_profile.md";
    }

    /* ----------- 私有辅助 ----------- */

    private String readString(Long userId, String keySuffix, String defaultValue) {
        String key = userId + "." + keySuffix;
        SysConfigEntity e = sysConfigMapper.selectOne(
                new QueryWrapper<SysConfigEntity>()
                        .eq("config_group", GROUP)
                        .eq("config_key", key)
                        .last("LIMIT 1")
        );
        if (e == null || e.getConfigValue() == null || e.getConfigValue().isBlank()) {
            return defaultValue;
        }
        return e.getConfigValue();
    }

    private void writeString(Long userId, String keySuffix, String value, String remark) {
        String key = userId + "." + keySuffix;
        sysConfigMapper.upsertConfig(GROUP, key, value, "STRING", 0, remark);
    }
}
