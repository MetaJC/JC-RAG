package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 菜单保存 Param
 * <p>permType 限定为菜单族: CATALOG (目录/容器) | MENU (菜单/叶子)
 * <p>其它类型 (BUTTON/API) 走 SysPermission 接口
 *
 * @author JC
 */
@Data
public class RbacMenuSaveParam {

    private Long parentId;

    /** 目录 CATALOG / 菜单 MENU，默认 MENU */
    private String permType;

    @NotBlank(message = "菜单编码不能为空")
    private String code;

    @NotBlank(message = "菜单标题不能为空")
    private String title;

    private String path;

    private String icon;

    private Integer sort;

    private String component;

    private Integer visible;
    private Integer isExternal;
    private Integer keepAlive;
    private String query;
    private Integer status;
}
