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
