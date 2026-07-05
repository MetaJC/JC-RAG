package com.jc.rag.admin.module.auth.model.vo;

import com.jc.rag.admin.module.rbac.model.vo.RbacMenuVO;

import java.util.List;

/**
 * 当前登录用户信息 VO。
 * <p>permCodes：用户拥有的全部 permCode，供前端 v-permission 指令 / 路由 meta 鉴权使用。
 * <p>menus：用户可见菜单树（permType=MENU + status=1 + 角色绑定），供 MainLayout 渲染。
 * @author JC
 */
public class AuthUserVO {

    private Long userId;
    private String username;
    private String nickname;
    private String userType;
    private List<String> permCodes;
    private List<RbacMenuVO> menus;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public List<String> getPermCodes() {
        return permCodes;
    }

    public void setPermCodes(List<String> permCodes) {
        this.permCodes = permCodes;
    }

    public List<RbacMenuVO> getMenus() {
        return menus;
    }

    public void setMenus(List<RbacMenuVO> menus) {
        this.menus = menus;
    }
}
