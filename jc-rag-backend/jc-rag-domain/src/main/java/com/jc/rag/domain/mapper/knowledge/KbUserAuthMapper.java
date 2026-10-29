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
import com.jc.rag.domain.entity.knowledge.KbUserAuthEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 知识库-用户授权 Mapper。
 * @author JC
 */
public interface KbUserAuthMapper extends BaseMapper<KbUserAuthEntity> {

    /**
     * 按 kb 查全部授权
     */
    List<KbUserAuthEntity> listByKbId(@Param("kbId") Long kbId);

    /**
     * 按 kb + user 查单条
     */
    KbUserAuthEntity selectByKbAndUser(@Param("kbId") Long kbId, @Param("userId") Long userId);

    /**
     * 按 kb 删除全部授权 (全量替换用)
     */
    int deleteByKbId(@Param("kbId") Long kbId);

    /**
     * 批量 upsert (用于全量替换)
     */
    int batchUpsert(@Param("list") Collection<KbUserAuthEntity> list);

    /**
     * 查当前用户的授权
     */
    KbUserAuthEntity selectByKbAndUserId(@Param("kbId") Long kbId, @Param("userId") Long userId);

    /**
     * 按 kb + userIds 批量查 (IN 查询)
     */
    List<KbUserAuthEntity> listByKbAndUsers(@Param("kbId") Long kbId, @Param("userIds") List<Long> userIds);
}
