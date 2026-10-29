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
import com.jc.rag.domain.entity.chat.UserApiKeyEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户自定义 API Key Mapper (admin/user 共用)。
 *
 * @author JC
 */
public interface UserApiKeyMapper extends BaseMapper<UserApiKeyEntity> {

    /** 按 userId + 模糊关键词 (provider/model_name) 查询 */
    List<Map<String, Object>> listUserApiKeys(@Param("userId") Long userId,
                                              @Param("likeKeyword") String likeKeyword);

    /** 获取用户在指定 provider 下的激活 key */
    List<Map<String, Object>> getActiveApiKey(@Param("userId") Long userId,
                                              @Param("provider") String provider);

    /** 查询用户全部激活的 API Key id (用于演示模式跳过配额检查) */
    List<Long> findActiveIdsByUserId(@Param("userId") Long userId);

    /** 按 user + provider 查询记录 (用于 upsert 判断) */
    List<Map<String, Object>> findByUserAndProvider(@Param("userId") Long userId,
                                                    @Param("provider") String provider);

    /** 更新 API key */
    int updateApiKey(@Param("id") Long id,
                     @Param("baseUrl") String baseUrl,
                     @Param("apiKey") String apiKey,
                     @Param("modelName") String modelName);

    /** 插入 API key */
    int insertApiKey(@Param("userId") Long userId,
                     @Param("provider") String provider,
                     @Param("baseUrl") String baseUrl,
                     @Param("apiKey") String apiKey,
                     @Param("modelName") String modelName);

    /** 启用 / 停用 */
    int toggleActive(@Param("id") Long id,
                     @Param("active") boolean active);

    /** 按 id 删除 */
    int deleteById(@Param("id") Long id);

    /** 按 id 查询 */
    List<Map<String, Object>> getById(@Param("id") Long id);
}
