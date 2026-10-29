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


package com.jc.rag.common.web.page;

import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * Service 分页查询模板基类 (场景 A: SQL 1:1 映实体).
 * <p>封装 "建 Page + count + pageXxx + toPageResponse" 四件套, 子类仅需提供 3 个抽象方法.
 * <p>子类实现中无需再 new Page&lt;&gt; 或调 setTotal.
 *
 * @param <VO>      业务 VO
 * @param <Param>   分页查询参数 (继承自 {@link PageQueryParam})
 * @param <EntityT> 实体类型
 * @author JC
 */
public abstract class AbstractPageService<VO, Param extends PageQueryParam, EntityT> {

    /** 实体 -> VO. */
    protected abstract VO toVO(EntityT entity);

    /** 分页查询 (Page 对象已由父类建好, 直接传给 Mapper, 由 MP 拦截器自动追加 LIMIT). */
    protected abstract IPage<EntityT> pageQuery(Param query, IPage<EntityT> page);

    /** 与 pageQuery 等条件的总记录数. */
    protected abstract long countQuery(Param query);

    /** 模板方法: 建 Page + count + 分页 + 转 VO. */
    public PageResponse<VO> pageList(Param query) {
        IPage<EntityT> page = PageHelper.pageOf(query);
        page = pageQuery(query, page);
        page.setTotal(countQuery(query));
        return PageHelper.toPageResponse(page, this::toVO);
    }
}
