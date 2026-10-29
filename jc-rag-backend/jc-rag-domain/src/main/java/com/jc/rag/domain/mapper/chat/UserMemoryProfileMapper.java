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


package com.jc.rag.domain.mapper.chat;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.chat.UserMemoryProfileEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 用户跨会话画像 Mapper (L3)。
 * <p>对应 user_memory_profile 表, 基础 CRUD 走 MyBatis-Plus BaseMapper, 业务查询走 XML。
 *
 * @author JC
 */
public interface UserMemoryProfileMapper extends BaseMapper<UserMemoryProfileEntity> {

    /**
     * 按 userId 取本用户最新一条画像 (无则返回 null)。
     * @param userId 用户 ID
     * @return 画像实体; 无则返回 null
     */
    UserMemoryProfileEntity selectByUserId(@Param("userId") Long userId);
}
