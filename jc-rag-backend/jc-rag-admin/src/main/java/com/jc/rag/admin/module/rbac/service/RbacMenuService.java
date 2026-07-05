package com.jc.rag.admin.module.rbac.service;

import com.jc.rag.admin.module.rbac.model.param.RbacMenuSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacMenuVO;
import com.jc.rag.common.model.base.BaseService;
import com.jc.rag.domain.entity.rbac.RbacPermissionEntity;

import java.util.List;

/**
 * 菜单管理 Service (基于权限表 permType=MENU 的子集)。
 * <p>继承通用 BaseService；实体为 {@link RbacPermissionEntity}，过滤 permType=MENU。
 *
 * @author JC
 */
public interface RbacMenuService extends BaseService<RbacPermissionEntity> {

    /** 菜单树（含子节点），供左侧菜单渲染用 */
    List<RbacMenuVO> listMenuTree();

    /** 菜单扁平列表（按 permType=MENU 过滤），供管理后台「菜单管理」tab 用 */
    List<RbacMenuVO> listMenus();

    /** 创建菜单 (内部写入 permType=MENU) */
    Long createMenu(RbacMenuSaveParam param);

    /**
     * 更新菜单 (校验 permType=MENU)
     * @return true=成功 false=失败
     */
    boolean updateMenu(Long id, RbacMenuSaveParam param);

    /**
     * 删除菜单 (校验 permType=MENU + 有子菜单拒绝)
     * @return true=成功 false=失败
     */
    boolean deleteMenu(Long id);

    /**
     * 拖拽移动节点 (支持改父级 + 调整同级排序)
     * <p>仅允许 permType=MENU; newParentId=0 或 null 表示移到根
     * <p>防回环: newParentId 不能是自身或自身的后代
     * @param id 被移动节点 id
     * @param newParentId 新父级 id (0/根 = null)
     * @param sort 拖到目标位置后的 sort (调用方根据 drop position 计算)
     * @return true=成功 false=失败
     */
    boolean moveMenu(Long id, Long newParentId, Integer sort);

    /** 按用户 id 取可见菜单树（permType=MENU + status=1 + role-bound） */
    List<RbacMenuVO> listMenuTreeByUserId(Long userId);

    /** 当前登录用户 id (委托 AbstractRbacService) */
    Long currentUserId();
}
