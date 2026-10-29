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


package com.jc.rag.admin.module.rbac.service;

import com.jc.rag.admin.module.rbac.model.param.RbacRoleQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacRoleSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacRoleVO;
import com.jc.rag.common.model.base.BaseService;
import com.jc.rag.domain.entity.rbac.RbacRoleEntity;
import com.jc.rag.common.web.page.PageResponse;

import java.util.List;

/**
 * 角色管理 Service。
 * <p>继承通用 BaseService，自动拥有 MP 通用 CRUD；本接口仅声明角色管理特有业务方法。
 *
 * @author JC
 */
public interface RbacRoleService extends BaseService<RbacRoleEntity> {

    /** 统计角色总数. */
    long countRoles();

    /** 分页角色列表（VO 形式） - 前端管理页使用 */
    PageResponse<RbacRoleVO> pageListRoles(RbacRoleQueryParam query);

    /** 创建角色 */
    Long createRole(RbacRoleSaveParam param);

    /**
     * 更新角色
     * @return true=成功 false=失败
     */
    boolean updateRole(Long id, RbacRoleSaveParam param);

    /**
     * 删除角色（同时清理角色-权限绑定）
     * @return true=成功 false=失败
     */
    boolean deleteRole(Long id);

    /**
     * 切换角色状态（仅改 status 字段，不做全量校验）
     * @return true=成功 false=失败
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 重置角色的权限绑定（先清后插）
     * @return true=成功 false=失败
     */
    boolean replaceRolePermissions(Long roleId, List<Long> permissionIds);

    /** 查询角色绑定的权限 ID 列表 */
    List<Long> findPermissionIdsByRoleId(Long roleId);
}
