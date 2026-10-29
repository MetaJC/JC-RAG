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
import com.jc.rag.domain.entity.rbac.RbacRoleEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 角色 Mapper。
 *
 * @author JC
 */
public interface RbacRoleMapper extends BaseMapper<RbacRoleEntity> {

    long countRoles(@Param("keyword") String keyword);

    /**
     * 分页查询角色 (含权限聚合)。
     * <p>调用方需关闭 searchCount, 用 {@link #countRoles} 手动设置 total。
     */
    IPage<Map<String, Object>> pageRoles(IPage<Map<String, Object>> page,
                                         @Param("keyword") String keyword);

    /** 全量导出 (无分页)。 */
    List<Map<String, Object>> listRoles(@Param("keyword") String keyword);

    Long selectIdByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 统计用户拥有的 is_admin=1 且启用的角色数量。
     * <p>登录管理端准入校验: 至少 1 个, developer 用户绕过此检查。
     */
    long countAdminRoleByUserId(@Param("userId") Long userId);
}
