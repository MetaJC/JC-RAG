package com.jc.rag.admin.module.rbac.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 菜单 VO
 * @author JC
 */
@Data
public class RbacMenuVO {
    private Long id;
    private Long parentId;
    private String code;
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
    private String statusText;
    private List<RbacMenuVO> children;
}
