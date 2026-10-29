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

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacUserPositionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户-岗位关联 Mapper (多对多)。
 * <p>按用户查岗位列表, 按岗位查用户列表; 同步 / 替换主岗位.
 *
 * @author JC
 */
public interface RbacUserPositionMapper extends BaseMapper<RbacUserPositionEntity> {

    /** 按用户 ID 查其全部关联的岗位 ID 列表 */
    List<Long> selectPositionIdsByUserId(@Param("userId") Long userId);

    /** 按岗位 ID 列表查关联的用户 ID 列表 (DISTINCT 去重) */
    List<Long> selectUserIdsByPositionIds(@Param("positionIds") List<Long> positionIds);

    /** 按岗位 ID 查关联的用户 ID 列表 */
    List<Long> selectUserIdsByPositionId(@Param("positionId") Long positionId);

    /**
     * 查询某岗位关联的用户详细信息 (Map 形式, 便于字段裁剪).
     * <p>JOIN rbac_user 取用户名/昵称/类型/状态, 过滤 deleted=0.
     * <p>字段: id, username, nickname, user_type, status.
     */
    List<Map<String, Object>> selectUsersByPositionId(@Param("positionId") Long positionId);

    /** 按用户 ID 删除其全部岗位关联 */
    int deleteByUserId(@Param("userId") Long userId);

    /** 按岗位 ID 删除其全部用户关联 */
    int deleteByPositionId(@Param("positionId") Long positionId);

    /** 批量插入用户-岗位关联 */
    int insertBatch(@Param("list") List<RbacUserPositionEntity> list);

    /**
     * 查询某用户的岗位 (含岗位名) Map 列表, 用于用户列表 / 详情展示.
     * <p>字段: positionId, positionName, displayName
     */
    List<Map<String, Object>> selectPositionsByUserId(@Param("userId") Long userId);
}
