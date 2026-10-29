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


package com.jc.rag.admin.module.sys.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

/**
 * 目录浏览结果 VO (admin)。
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirBrowseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String currentPath;
    private String parentPath;
    private List<DirEntry> entries;

    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String currentPath) { this.currentPath = currentPath; }
    public String getParentPath() { return parentPath; }
    public void setParentPath(String parentPath) { this.parentPath = parentPath; }
    public List<DirEntry> getEntries() { return entries; }
    public void setEntries(List<DirEntry> entries) { this.entries = entries; }

    /** 目录条目内嵌 VO。 */
    public static class DirEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String path;
        private Boolean directory;
        private Long sizeBytes;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Boolean getDirectory() { return directory; }
        public void setDirectory(Boolean directory) { this.directory = directory; }
        public Long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    }
}
