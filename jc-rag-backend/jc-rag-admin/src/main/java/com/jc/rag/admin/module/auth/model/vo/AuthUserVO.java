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
