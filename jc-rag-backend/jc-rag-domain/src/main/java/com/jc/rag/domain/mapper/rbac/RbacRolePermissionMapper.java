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

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacRolePermissionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-权限关联 Mapper。
 *
 * @author JC
 */
public interface RbacRolePermissionMapper extends BaseMapper<RbacRolePermissionEntity> {

    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    int deleteByRoleId(@Param("roleId") Long roleId);

    int insertBatch(@Param("list") List<RbacRolePermissionEntity> list);
}
