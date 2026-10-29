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


package com.jc.rag.admin.module.rag.service;

import com.jc.rag.admin.module.rag.model.param.RagSkillQueryParam;
import com.jc.rag.admin.module.rag.model.param.RagSkillSaveParam;
import com.jc.rag.admin.module.rag.model.vo.RagSkillVO;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;

import java.util.List;
import java.util.Map;

/**
 * Skill Service。
 * @author JC
 */
public interface RagSkillService {

    PageResponse<RagSkillVO> pageList(RagSkillQueryParam query);

    List<RagSkillVO> listAll();

    RagSkillVO getById(Long id);

    Long create(RagSkillSaveParam param, Long userId);

    /**
     * 更新 Skill
     * @return true=成功 false=失败
     */
    boolean update(Long id, RagSkillSaveParam param);

    /**
     * 删除 Skill
     * @return true=成功 false=失败
     */
    boolean delete(Long id);

    /** 兼容旧前端：直接返回 Map 列表 */
    PageResponse<Map<String, Object>> pageListAsMap(String keyword, String status, PageRequest pageRequest);
}
