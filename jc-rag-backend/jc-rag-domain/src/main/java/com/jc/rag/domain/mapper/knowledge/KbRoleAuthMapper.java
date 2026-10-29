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


package com.jc.rag.domain.mapper.knowledge;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbRoleAuthEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 知识库-角色授权 Mapper。
 * @author JC
 */
public interface KbRoleAuthMapper extends BaseMapper<KbRoleAuthEntity> {

    /**
     * 按 kb 查全部授权
     */
    List<KbRoleAuthEntity> listByKbId(@Param("kbId") Long kbId);

    /**
     * 按 kb + role 查单条
     */
    KbRoleAuthEntity selectByKbAndRole(@Param("kbId") Long kbId, @Param("roleId") Long roleId);

    /**
     * 按 kb 删除全部授权 (全量替换用)
     */
    int deleteByKbId(@Param("kbId") Long kbId);

    /**
     * 批量 upsert (用于全量替换)
     */
    int batchUpsert(@Param("list") Collection<KbRoleAuthEntity> list);

    /**
     * 查当前用户所有相关 role 的授权最大级别 (含 union all 或 max)
     */
    List<KbRoleAuthEntity> listByKbAndRoles(@Param("kbId") Long kbId, @Param("roleIds") List<Long> roleIds);
}
