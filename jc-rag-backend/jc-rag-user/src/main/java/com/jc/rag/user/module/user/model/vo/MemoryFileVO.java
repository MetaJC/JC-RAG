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


package com.jc.rag.user.module.user.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆文件 (Markdown) 视图。
 * <p>承载前端"查看 / 编辑 / 复制路径 / 在系统中打开"四种操作的统一数据。
 *
 * @author JC
 */
@Data
public class MemoryFileVO {

    /**
     * 原始路径 (用户填的, 含 {@code ~}); 仅作展示.
     */
    private String rawPath;

    /**
     * 展开后的真实路径 (如 {@code C:\Users\xxx\.jc\jc-rag\memory\\user_profile.md});
     * 后端真正读写的路径.
     */
    private String resolvedPath;

    /**
     * 父目录 (展开后), 例如 {@code ~/.jc/jc-rag/memory}.
     */
    private String parentDir;

    /**
     * 文件内容 (Markdown 文本).
     * <p>文件不存在时返回默认模板 (首次查看会触发后端"自动初始化").
     */
    private String content;

    /**
     * 文件是否真实存在磁盘上.
     */
    private Boolean exists;

    /**
     * 文件字节数 (UTF-8 编码).
     */
    private Long bytes;

    /**
     * 最后修改时间 (文件存在才有值).
     */
    private LocalDateTime lastModified;

    /**
     * 是否为后端自动写入的默认模板 (用户尚未编辑过).
     */
    private Boolean autoInitialized;
}
