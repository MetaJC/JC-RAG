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
