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

import com.jc.rag.admin.module.sys.model.param.SystemSettingParam;
import com.jc.rag.admin.module.sys.model.vo.SystemSettingVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统设置服务接口 (sys_config 读写).
 * <p>设置项由系统设置页维护, 用户端登录页与注册/登录链路读取生效.
 *
 * @author JC
 */
public interface SysSettingService {

    /** 查询全部系统设置 (缺失项返回内置默认值) */
    SystemSettingVO getSettings();

    /** 保存系统设置 (逐项 UPSERT sys_config) */
    @Transactional
    SystemSettingVO saveSettings(SystemSettingParam param);
}
