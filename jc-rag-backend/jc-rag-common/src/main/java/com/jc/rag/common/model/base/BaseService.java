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


package com.jc.rag.common.model.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;

import java.io.Serializable;

/**
 * 通用 Service 基接口。
 * <p>在 MP IService 之上补充业务常用方法（强校验、统一分页）。
 *
 * @author JC
 */
public interface BaseService<T> extends IService<T> {

    /** 按 id 查询，找不到抛 {@link ResourceNotFoundException} (i18n key: common.id.notfound)。 */
    default T getByIdRequired(Serializable id) {
        T t = getById(id);
        if (t == null) {
            throw new ResourceNotFoundException("common.id.notfound", id);
        }
        return t;
    }

    /** 统一分页入口，返回 PageResponse 与前端约定。 */
    default PageResponse<T> pageList(Wrapper<T> wrapper, PageRequest pr) {
        IPage<T> p = page(new Page<>(pr.getPageNum(), pr.getPageSize()), wrapper);
        return new PageResponse<>(p.getRecords(), p.getTotal(), pr.getPageNum(), pr.getPageSize());
    }
}
