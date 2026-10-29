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

import com.jc.rag.admin.module.rbac.model.param.RbacPermissionQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacPermissionSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacPermissionVO;
import com.jc.rag.common.model.base.BaseService;
import com.jc.rag.domain.entity.rbac.RbacPermissionEntity;
import com.jc.rag.common.web.page.PageResponse;

/**
 * 权限管理 Service。
 * <p>继承通用 BaseService。
 *
 * @author JC
 */
public interface RbacPermissionService extends BaseService<RbacPermissionEntity> {

    /** 统计权限总数. */
    long countPermissions();

    /** 分页权限列表（VO 形式） - 前端管理页使用 */
    PageResponse<RbacPermissionVO> pageListPermissions(RbacPermissionQueryParam query);

    /** 创建权限 */
    Long createPermission(RbacPermissionSaveParam param);

    /**
     * 更新权限
     * @return true=成功 false=失败
     */
    boolean updatePermission(Long id, RbacPermissionSaveParam param);

    /**
     * 删除权限
     * @return true=成功 false=失败
     */
    boolean deletePermission(Long id);
}
