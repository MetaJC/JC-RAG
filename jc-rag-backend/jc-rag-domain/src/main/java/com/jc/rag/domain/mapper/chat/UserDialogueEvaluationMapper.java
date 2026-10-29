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
import com.jc.rag.domain.entity.chat.UserDialogueEvaluationEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话评估记录 Mapper.
 * <p>对应 user_dialogue_evaluation 表.</p>
 *
 * @author JC
 */
public interface UserDialogueEvaluationMapper extends BaseMapper<UserDialogueEvaluationEntity> {

    /**
     * 按 userId 查询全部评估记录, 倒序.
     */
    List<UserDialogueEvaluationEntity> selectByUserId(@Param("userId") Long userId,
                                                       @Param("limit") Integer limit);

    /**
     * 统计用户在指定时间窗口内的评估次数 (用于每日配额).
     */
    int countByUserIdAndWindow(@Param("userId") Long userId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    /**
     * 管理端: 全部评估记录 (最新在前, 支持分页).
     */
    List<UserDialogueEvaluationEntity> selectAllPaged(@Param("offset") int offset,
                                                      @Param("limit") int limit);
}
