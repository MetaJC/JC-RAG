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


package com.jc.rag.admin.module.user.service;

import com.jc.rag.admin.module.user.model.param.UserCustomAgentQueryParam;
import com.jc.rag.admin.module.user.model.vo.UserCustomAgentAdminVO;
import com.jc.rag.common.web.page.PageResponse;

/**
 * Admin 端用户智能体管理服务.
 * <p>Admin 只读 + 强制删除 + 启用/禁用, 用于审计/管理/查看.
 * @author JC
 */
public interface AdminUserCustomAgentService {

    /** 分页查询, 含创建者信息 (join rbac_user) */
    PageResponse<UserCustomAgentAdminVO> pageList(UserCustomAgentQueryParam query);

    /** 查单条 */
    UserCustomAgentAdminVO getById(Long id);

    /** 强制删除 (admin 端审计性删除) */
    boolean forceDelete(Long id);

    /** 启用/禁用 (admin 端审计性管控) */
    boolean updateStatus(Long id, Integer status);
}
