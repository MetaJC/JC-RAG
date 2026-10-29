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


package com.jc.rag.admin.module.rbac.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.admin.module.rbac.model.param.RbacPermissionQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacPermissionSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacPermissionVO;
import com.jc.rag.admin.module.rbac.service.AbstractRbacService;
import com.jc.rag.admin.module.rbac.service.RbacPermissionService;
import com.jc.rag.common.exception.DataConflictException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.domain.mapper.rbac.RbacPermissionMapper;
import com.jc.rag.common.model.base.BaseServiceImpl;
import com.jc.rag.domain.entity.rbac.RbacPermissionEntity;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 权限管理实现。
 * <p>继承通用 BaseServiceImpl 获得 MP 全部 CRUD。
 *
 * @author JC
 */
@Slf4j
@Service
public class RbacPermissionServiceImpl extends BaseServiceImpl<RbacPermissionMapper, RbacPermissionEntity> implements RbacPermissionService {

    private final AbstractRbacService rbac;

    public RbacPermissionServiceImpl(AbstractRbacService rbac) {
        this.rbac = rbac;
    }

    @Override
    public long countPermissions() {
        return this.count();
    }

    @Override
    public PageResponse<RbacPermissionVO> pageListPermissions(RbacPermissionQueryParam query) {
        String like = buildLike(query.getKeyword());
        String pt = (query.getPermType() == null || query.getPermType().isEmpty()) ? null : query.getPermType();
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = baseMapper.pagePermissions(page, pt, like);
        page.setTotal(baseMapper.countPermissions(pt, like));
        return PageHelper.toPageResponse(page, this::toPermissionVO);
    }

    @Override
    @Transactional
    public Long createPermission(RbacPermissionSaveParam param) {
        if (getOne(new QueryWrapper<RbacPermissionEntity>().eq("perm_code", param.getPermCode())) != null) {
            throw new DataConflictException("rbac.permission.duplicate.code", param.getPermCode());
        }
        RbacPermissionEntity p = new RbacPermissionEntity();
        p.setParentId(param.getParentId() == null ? 0L : param.getParentId());
        p.setPermCode(param.getPermCode());
        p.setPermName(param.getPermName());
        p.setPath(param.getPath());
        p.setMethod(param.getMethod());
        p.setPermType(param.getPermType() == null ? "MENU" : param.getPermType());
        p.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        save(p);
        return p.getId();
    }

    @Override
    @Transactional
    public boolean updatePermission(Long id, RbacPermissionSaveParam param) {
        RbacPermissionEntity p = getById(id);
        if (p == null) throw new ResourceNotFoundException("rbac.permission.notfound", id);
        if (param.getParentId() != null) p.setParentId(param.getParentId());
        if (param.getPermCode() != null) p.setPermCode(param.getPermCode());
        if (param.getPermName() != null) p.setPermName(param.getPermName());
        if (param.getPath() != null) p.setPath(param.getPath());
        if (param.getMethod() != null) p.setMethod(param.getMethod());
        if (param.getPermType() != null) p.setPermType(param.getPermType());
        if (param.getStatus() != null) p.setStatus(param.getStatus());
        return updateById(p);
    }

    @Override
    @Transactional
    public boolean deletePermission(Long id) {
        RbacPermissionEntity p = getById(id);
        if (p == null) throw new ResourceNotFoundException("rbac.permission.notfound", id);
        return removeById(id);
    }

    private String buildLike(String keyword) {
        if (keyword == null || keyword.isEmpty()) return null;
        return "%" + keyword + "%";
    }

    private RbacPermissionVO toPermissionVO(Map<String, Object> row) {
        RbacPermissionVO v = new RbacPermissionVO();
        v.setId(toLong(row.get("id")));
        v.setPermCode((String) row.get("permCode"));
        v.setPermName((String) row.get("permName"));
        v.setPermType((String) row.get("permType"));
        v.setPermTypeText(rbac.permissionTypeText(v.getPermType()));
        v.setParentId(toLong(row.get("parentId")));
        v.setPath((String) row.get("path"));
        v.setMethod((String) row.get("method"));
        v.setStatus(toInt(row.get("status")));
        v.setStatusText(rbac.statusText(v.getStatus()));
        return v;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }
}
