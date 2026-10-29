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

import com.jc.rag.admin.module.rbac.model.param.RbacUserQueryParam;
import com.jc.rag.admin.module.rbac.model.param.RbacUserSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacUserVO;
import com.jc.rag.common.model.base.BaseService;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import com.jc.rag.common.web.page.PageResponse;

import java.util.List;

/**
 * 用户管理 Service。
 * <p>继承通用 BaseService，自动拥有 MP 通用 CRUD；本接口仅声明用户管理特有业务方法。
 *
 * @author JC
 */
public interface RbacUserService extends BaseService<RbacUserEntity> {

    /** 统计用户总数 (含 DEVELOPER). */
    long countUsers();

    /** 分页用户列表（VO 形式） - 前端管理页使用 */
    PageResponse<RbacUserVO> pageListUsers(RbacUserQueryParam query);

    /** 创建用户 */
    Long createUser(RbacUserSaveParam param);

    /**
     * 更新用户
     * @return true=成功 false=失败（如记录不存在/字段无变化）
     */
    boolean updateUser(Long id, RbacUserSaveParam param);

    /**
     * 切换用户状态（仅改 status 字段，不做全量校验）
     * @return true=成功 false=失败
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 删除用户
     * @return true=成功 false=失败
     */
    boolean deleteUser(Long id);

    /**
     * 重置密码
     * @return true=成功 false=失败
     */
    boolean resetPassword(Long id, String newPassword);

    /**
     * 重置用户的角色绑定（先清后插）
     * @return true=成功 false=失败
     */
    boolean replaceUserRoles(Long userId, List<Long> roleIds);

    /** 查询用户绑定的角色 ID 列表 */
    List<Long> findRoleIdsByUserId(Long userId);

    /** 查询角色绑定的用户 ID 列表 */
    List<Long> findUserIdsByRoleId(Long roleId);

    /**
     * 覆盖式为角色分配用户（先清后插）
     * @return true=成功 false=失败
     */
    boolean assignUsersToRole(Long roleId, List<Long> userIds);

    /**
     * 查询某部门 (含后代) 关联的用户 ID 列表 (DISTINCT).
     * <p>用于部门管理 - 分配用户弹窗回显.
     */
    List<Long> findUserIdsByDepartmentId(Long departmentId);

    /**
     * 查询某部门 (含后代) 关联的用户详细信息列表.
     * <p>字段: id / username / nickname / userType / status.
     */
    List<java.util.Map<String, Object>> listUsersByDepartmentId(Long departmentId);

    /**
     * 覆盖式为部门分配用户 (含后代): userIds 即为该部门树范围内的最终绑定关系.
     * <p>语义: 清除所有后代部门上对这些 userId 的关联, 然后在目标部门上重新建立 (is_primary=0).
     * <p>主部门 (rbac_user.department_id) 不在此处维护, 仅维护 rbac_user_department 兼职关联.
     *
     * @param departmentId 部门 id (含其所有后代)
     * @param userIds      最终要绑定的用户 ID 列表; 空集合 = 清空该部门树范围内所有用户
     * @return true=成功
     */
    boolean assignUsersToDepartment(Long departmentId, List<Long> userIds);

    /**
     * 单用户详情 (只读 VO), 用于"部门树内点用户 nickname → 弹窗显示用户详情".
     * <p>不返回 passwordHash, 含部门名 / 性别 / 状态等展示字段.
     */
    RbacUserVO getUserDetail(Long userId);

    /**
     * 查询某岗位关联的用户 ID 列表.
     * <p>用于岗位管理 - 分配用户弹窗回显.
     */
    List<Long> findUserIdsByPositionId(Long positionId);

    /**
     * 覆盖式为岗位分配用户: userIds 即为该岗位最终绑定的用户 ID 列表.
     * <p>语义: 清除该岗位上的所有旧关联, 然后用 userIds 重建 (is_primary=0).
     *
     * @param positionId 岗位 id
     * @param userIds    最终要绑定的用户 ID 列表; 空集合 = 清空该岗位所有用户
     * @return true=成功
     */
    boolean assignUsersToPosition(Long positionId, List<Long> userIds);

    /**
     * 查询某用户绑定的岗位 ID 列表 (主岗位优先).
     * <p>用于用户列表 / 编辑用户回显.
     */
    List<Long> findPositionIdsByUserId(Long userId);

    /**
     * 替换用户岗位关联 (先清后插), 用于 createUser / updateUser.
     *
     * @param userId      用户 id
     * @param positionIds 新的岗位 ID 列表; null 或空 = 清空
     * @return true=成功
     */
    boolean replaceUserPositions(Long userId, List<Long> positionIds);
}
