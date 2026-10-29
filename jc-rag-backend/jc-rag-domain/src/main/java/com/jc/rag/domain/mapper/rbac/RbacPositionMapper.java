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


package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacPositionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 岗位 Mapper (RBAC 组织维度)。
 * <p>必须继承通用 BaseMapper, 禁止直接继承 MP BaseMapper。
 * <p>分页走 MP 内置 PaginationInnerInterceptor, 不手算 LIMIT。
 *
 * @author JC
 */
public interface RbacPositionMapper extends BaseMapper<RbacPositionEntity> {

    /**
     * 分页统计: 按 keyword / status / level 过滤。
     */
    long countPositions(@Param("keyword") String keyword,
                        @Param("status") Integer status,
                        @Param("level") Integer level);

    /**
     * 分页查询: 走 MP 分页拦截器, 调用方负责 setTotal。
     */
    IPage<Map<String, Object>> pagePositions(IPage<Map<String, Object>> page,
                                            @Param("keyword") String keyword,
                                            @Param("status") Integer status,
                                            @Param("level") Integer level);

    /**
     * 单条详情。
     */
    Map<String, Object> selectPositionById(@Param("id") Long id);
}
