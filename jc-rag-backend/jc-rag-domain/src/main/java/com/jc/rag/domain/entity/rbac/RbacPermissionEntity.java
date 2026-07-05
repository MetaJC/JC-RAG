package com.jc.rag.domain.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 权限实体 (菜单/按钮/接口)。
 * <p>对应 rbac_permission 表, 字段分工：
 * <ul>
 *   <li>{@code path}：前端菜单路径（/admin/dashboard），用于前端路由</li>
 *   <li>{@code urlPattern}：Controller 注解 path（/api/rbac/user/{id}），用于后端 permCode 路由级鉴权</li>
 * </ul>
 *
 * @author JC
 */
@TableName("rbac_permission")
public class RbacPermissionEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 权限编码 (后端 @RequiresPermissions 引用)
     */
    private String permCode;
    /**
     * 权限名称 (前端展示)
     */
    private String permName;
    /**
     * 权限类型 (MENU 菜单 / BUTTON 按钮 / API 接口)
     */
    private String permType;
    /**
     * 父权限 ID (顶级为 0)
     */
    private Long parentId;
    /**
     * 前端路由 path
     */
    private String path;
    /**
     * 后端接口 path (含 {id} 占位)
     */
    private String urlPattern;
    /**
     * HTTP 方法 (GET/POST/...)
     */
    private String method;
    /**
     * 前端图标
     */
    private String icon;
    /**
     * 排序号 (升序)
     */
    private Integer sort;
    /**
     * 前端组件路径 (懒加载用)
     */
    private String component;
    /**
     * 是否显示 (1 显示 / 0 隐藏)
     */
    private Integer visible;
    /**
     * 是否外链 (1 是 / 0 否)
     */
    private Integer isExternal;
    /**
     * 是否缓存 (1 缓存 / 0 不缓存)
     */
    private Integer keepAlive;
    /**
     * 路由参数 (?k=v&...)
     */
    private String query;
    /**
     * 启用状态 (1 启用 / 0 禁用)
     */
    private Integer status;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public String getPermCode() {
        return permCode;
    }

    public void setPermCode(String v) {
        permCode = v;
    }

    public String getPermName() {
        return permName;
    }

    public void setPermName(String v) {
        permName = v;
    }

    public String getPermType() {
        return permType;
    }

    public void setPermType(String v) {
        permType = v;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long v) {
        parentId = v;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String v) {
        path = v;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String v) {
        urlPattern = v;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String v) {
        method = v;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String v) {
        icon = v;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer v) {
        sort = v;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String v) {
        component = v;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer v) {
        visible = v;
    }

    public Integer getIsExternal() {
        return isExternal;
    }

    public void setIsExternal(Integer v) {
        isExternal = v;
    }

    public Integer getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(Integer v) {
        keepAlive = v;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String v) {
        query = v;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer v) {
        status = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime v) {
        updatedAt = v;
    }
}
