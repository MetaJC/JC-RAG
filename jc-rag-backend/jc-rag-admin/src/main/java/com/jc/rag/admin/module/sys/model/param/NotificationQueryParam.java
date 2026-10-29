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


package com.jc.rag.admin.module.sys.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 系统通知查询 Param。
 * <p>分页字段 {@code page/size} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class NotificationQueryParam extends PageQueryParam {

    private String targetUserType;

    private Integer status;
}
