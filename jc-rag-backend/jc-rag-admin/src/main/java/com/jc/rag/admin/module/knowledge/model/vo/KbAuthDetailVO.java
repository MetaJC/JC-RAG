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


package com.jc.rag.admin.module.knowledge.model.vo;

import com.jc.rag.domain.enums.knowledge.KbPermLevelEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库授权详情 VO。
 * <p>包含: 角色列表 + 选中状态 / 角色下用户列表 + 选中状态 / 客户端可见选项 / 知识库基础信息。
 *
 * @author JC
 */
@Data
public class KbAuthDetailVO {

    private Long kbId;
    private String kbName;
    private String visibilityScope;
    private Integer clientSelectable;

    /** 全量角色授权项: 含 roleId/roleName/permLevel */
    private List<RoleAuthItem> roleAuths = new ArrayList<>();

    /** 全量用户授权项 (ADMIN 用户) */
    private List<UserAuthItem> userAuths = new ArrayList<>();

    @Data
    public static class RoleAuthItem {
        private Long roleId;
        private String roleCode;
        private String roleName;
        /** 0 NONE / 1 VIEW / 2 EDIT / 3 MANAGE */
        private Integer permLevel;
        private String permLevelText;

        public static RoleAuthItem of(Long roleId, String code, String name, Integer level) {
            RoleAuthItem i = new RoleAuthItem();
            i.setRoleId(roleId);
            i.setRoleCode(code);
            i.setRoleName(name);
            i.setPermLevel(level);
            i.setPermLevelText(KbPermLevelEnum.ofCode(level).getText());
            return i;
        }
    }

    @Data
    public static class UserAuthItem {
        private Long userId;
        private String username;
        private String nickname;
        /** 0 NONE / 1 VIEW / 2 EDIT / 3 MANAGE */
        private Integer permLevel;
        private String permLevelText;
        /** 该用户所属角色 ID 集合, 用于左侧联动展示 */
        private List<Long> roleIds;

        public static UserAuthItem of(Long userId, String username, String nickname,
                                      Integer level, List<Long> roleIds) {
            UserAuthItem i = new UserAuthItem();
            i.setUserId(userId);
            i.setUsername(username);
            i.setNickname(nickname);
            i.setPermLevel(level);
            i.setPermLevelText(KbPermLevelEnum.ofCode(level).getText());
            i.setRoleIds(roleIds);
            return i;
        }
    }
}
