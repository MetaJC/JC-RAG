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
}
