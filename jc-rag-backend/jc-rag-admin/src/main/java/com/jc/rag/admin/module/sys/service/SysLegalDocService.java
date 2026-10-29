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

import com.jc.rag.admin.module.sys.model.param.SysLegalDocSaveParam;
import com.jc.rag.admin.module.sys.model.vo.SysLegalDocVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户协议 / 隐私政策 Service。
 * <p>每种 docType 全表唯一, 业务上只有一条记录 (草稿/已发布切换)。
 *
 * @author JC
 */
public interface SysLegalDocService {

    /** 列表: 所有 docType (草稿+已发布) */
    List<SysLegalDocVO> listAll();

    /** 单条详情 (草稿/已发布都返回) */
    SysLegalDocVO getByDocType(String docType);

    /**
     * 保存 (草稿或发布) - 同一 docType 唯一
     * @return true=成功 false=失败
     */
    boolean save(SysLegalDocSaveParam param, Long userId);

    /**
     * 发布: 把指定 docType 置为 PUBLISHED, 记录发布时间+发布人
     * @return true=成功 false=失败
     */
    boolean publish(String docType, Long userId);

    /** 解析上传的 word/markdown 文件为 Markdown 字符串, 仅做转换不入库 */
    String parseUploadedFile(MultipartFile file);
}
