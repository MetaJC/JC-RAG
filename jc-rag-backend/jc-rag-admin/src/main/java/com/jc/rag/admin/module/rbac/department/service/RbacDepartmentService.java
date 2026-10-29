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


package com.jc.rag.admin.module.rbac.department.service;

import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentQueryParam;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentSaveParam;
import com.jc.rag.admin.module.rbac.department.model.vo.RbacDepartmentCascadeResult;
import com.jc.rag.admin.module.rbac.department.model.vo.RbacDepartmentVO;
import com.jc.rag.common.model.base.BaseService;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.domain.entity.rbac.RbacDepartmentEntity;

import java.util.List;

/**
 * 部门管理 Service (RBAC 组织维度)。
 * <p>继承通用 BaseService 获得 MP 通用 CRUD, 本接口仅声明部门管理特有业务方法。
 *
 * @author JC
 */
public interface RbacDepartmentService extends BaseService<RbacDepartmentEntity> {

    /**
     * 分页部门列表 (VO 形式)。
     */
    PageResponse<RbacDepartmentVO> pageList(RbacDepartmentQueryParam query);

    /**
     * 部门详情。
     */
    RbacDepartmentVO getDetail(Long id);

    /**
     * 部门树 (用于前端部门选择 / 组织架构展示)。
     */
    List<RbacDepartmentVO> listTree();

    /**
     * 创建部门。
     */
    Long create(RbacDepartmentSaveParam param);

    /**
     * 更新部门。
     * @return true=成功 false=失败
     */
    boolean update(Long id, RbacDepartmentSaveParam param);

    /**
     * 删除部门 (有子部门 / 有启用岗位则拒绝)。
     * @return true=成功 false=失败
     */
    boolean delete(Long id);

    /**
     * 切换状态 (仅改 status)。
     * @return true=成功 false=失败
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 拖拽调整部门结构 (改变父级 / 排序).
     *
     * @param id       被拖动的部门 id
     * @param targetId 释放位置的目标部门 id
     * @param position before=同级前位 / after=同级后位 / inner=变为子级
     * @return true=成功
     */
    boolean move(Long id, Long targetId, String position);

    /**
     * 查找顶级部门 (公司根节点, parent_id=0).
     * <p>系统约定: 顶级部门唯一, 即公司名称. 多个顶级 (按 sort 取第一个) 视为历史脏数据, 兜底.
     *
     * @return 顶级部门 VO, 找不到返回 null
     */
    RbacDepartmentVO getRootDepartment();

    /**
     * 更新公司名称 (顶级部门的 name).
     * <p>仅 super_admin / developer 可调用 (Controller 层校验).
     *
     * @param newName 新公司名称
     * @return true=成功
     */
    boolean updateCompanyName(String newName);

    /**
     * 级联更新状态: 把目标部门 + 所有后代 (含子级 / 孙级) 一次性更新.
     * <p>父级启停时, 子部门应同步收到影响, 保持组织维度状态一致.
     *
     * @param id     目标部门 id
     * @param status 0=禁用 1=启用
     * @return 级联结果 (含受影响的部门名称列表, 供前端展示)
     */
    RbacDepartmentCascadeResult updateStatusCascade(Long id, Integer status);
}
