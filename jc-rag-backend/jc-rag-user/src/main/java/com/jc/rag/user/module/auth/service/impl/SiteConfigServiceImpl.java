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


package com.jc.rag.user.module.auth.service.impl;

import com.jc.rag.common.constant.SysConfigKeys;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.jc.rag.user.module.auth.model.vo.SiteConfigVO;
import com.jc.rag.user.module.auth.service.SiteConfigService;
import org.springframework.stereotype.Service;

/**
 * 站点配置服务实现 (sys_config 读取).
 * <p>布尔配置约定: "1"/"true" 为真; 配置缺失时按内置默认值处理,
 * 保证未执行播种迁移的环境行为与现状一致 (注册开放/协议必选).
 *
 * @author JC
 */
@Service
public class SiteConfigServiceImpl implements SiteConfigService {

    /** 系统名称默认值 */
    private static final String DEFAULT_SYSTEM_NAME = "JC RAG";

    private final SysConfigMapper sysConfigMapper;

    public SiteConfigServiceImpl(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @Override
    public SiteConfigVO getSiteConfig() {
        SiteConfigVO vo = new SiteConfigVO();
        String name = sysConfigMapper.selectStringValue(SysConfigKeys.SITE_SYSTEM_NAME);
        vo.setSystemName(name == null || name.isBlank() ? DEFAULT_SYSTEM_NAME : name);
        vo.setLogoUrl(sysConfigMapper.selectStringValue(SysConfigKeys.SITE_LOGO_URL));
        vo.setCopyright(sysConfigMapper.selectStringValue(SysConfigKeys.SITE_COPYRIGHT));
        vo.setRegisterOpen(readBoolean(SysConfigKeys.AUTH_REGISTER_OPEN, true));
        vo.setLoginAgreeRequired(readBoolean(SysConfigKeys.AUTH_LOGIN_AGREE_REQUIRED, true));
        return vo;
    }

    @Override
    public boolean isRegisterOpen() {
        return readBoolean(SysConfigKeys.AUTH_REGISTER_OPEN, true);
    }

    /**
     * 读取布尔配置: "1"/"true" 为真, 缺失或非法值返回默认值.
     */
    private boolean readBoolean(String key, boolean defaultValue) {
        String value = sysConfigMapper.selectStringValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim());
    }
}
