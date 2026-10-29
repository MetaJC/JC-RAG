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


package com.jc.rag.user.module.user.model.param;

import lombok.Data;

/**
 * 更新当前用户偏好参数 (C 端), 按字段可选, 只更新非空字段。
 *
 * @author JC
 */
@Data
public class UserPreferenceUpdateParam {

    /** 记忆文件路径: 留空表示不修改 */
    private String memoryFilePath;

    /** 默认模型 code: 留空表示不修改; 显式传空字符串表示"跟随系统默认" */
    private String defaultModelCode;

    /** 回答风格: 留空表示不修改; 显式传空字符串表示"使用系统默认" */
    private String chatStyle;

    /** L2 长期记忆开关: 留空表示不修改; true=启用 / false=关闭; 默认 true */
    private Boolean enableLongMemory;

    /** L3 跨会话画像开关: 留空表示不修改; true=启用 / false=关闭; 默认 true */
    private Boolean enableMemoryProfile;
}
