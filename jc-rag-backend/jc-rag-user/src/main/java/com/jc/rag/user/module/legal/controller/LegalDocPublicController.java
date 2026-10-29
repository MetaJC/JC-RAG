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


package com.jc.rag.user.module.legal.controller;

import com.jc.rag.common.web.R;
import com.jc.rag.domain.mapper.legal.SysLegalDocMapper;
import com.jc.rag.user.module.legal.model.vo.LegalDocVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户协议 / 隐私政策 公共查询接口 (公开, 无需登录)。
 * <p>仅返回 PUBLISHED 状态内容; 管理端写入/草稿/发布由 admin 端 SysLegalDocController 提供。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/legal-doc")
public class LegalDocPublicController {

    private static final List<String> DOC_TYPES = List.of("USER_AGREEMENT", "PRIVACY_POLICY");

    private final SysLegalDocMapper legalDocMapper;

    public LegalDocPublicController(SysLegalDocMapper legalDocMapper) {
        this.legalDocMapper = legalDocMapper;
    }

    /** 已发布文档列表 (供登录页超链接预览摘要) */
    @GetMapping("/published")
    public R<List<LegalDocVO>> listPublished() {
        List<LegalDocVO> list = new ArrayList<>();
        for (String type : DOC_TYPES) {
            LegalDocVO vo = toVO(getPublished(type));
            if (vo != null) list.add(vo);
        }
        return R.ok(list);
    }

    /** 单条已发布详情 (协议详情页使用) */
    @GetMapping("/{docType}/published")
    public R<LegalDocVO> publishedDetail(@PathVariable String docType) {
        Map<String, Object> data = getPublished(docType);
        if (data == null) {
            return R.fail(404, "未找到已发布的协议内容");
        }
        return R.ok(toVO(data));
    }

    private Map<String, Object> getPublished(String docType) {
        try {
            return legalDocMapper.findPublishedByDocType(docType);
        } catch (Exception e) {
            return null;
        }
    }

    private LegalDocVO toVO(Map<String, Object> row) {
        if (row == null) return null;
        LegalDocVO vo = new LegalDocVO();
        vo.setId(toLong(row.get("id")));
        vo.setDocType((String) row.get("docType"));
        vo.setTitle((String) row.get("title"));
        vo.setContent((String) row.get("contentMd"));
        vo.setPublishedAt(toLocalDateTime(row.get("publishedAt")));
        return vo;
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    private static java.time.LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof java.time.LocalDateTime t) return t;
        return java.time.LocalDateTime.parse(v.toString().replace(' ', 'T'));
    }
}
