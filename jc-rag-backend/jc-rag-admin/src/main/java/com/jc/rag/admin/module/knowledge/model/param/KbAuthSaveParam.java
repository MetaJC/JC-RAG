package com.jc.rag.admin.module.knowledge.model.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 知识库授权保存参数。
 * <p>采用全量替换策略: 传入 roleAuths / userAuths 即覆盖该 KB 的全部授权。
 *
 * @author JC
 */
@Data
public class KbAuthSaveParam {

    /** 角色授权列表 (level 0/1/2/3, 0 表示取消授权) */
    @NotNull(message = "角色授权列表不能为空")
    private List<KbRoleAuthItem> roleAuths;

    /** 用户授权列表 */
    private List<KbUserAuthItem> userAuths;

    @Data
    public static class KbRoleAuthItem {
        private Long roleId;
        /** 1 VIEW / 2 EDIT / 3 MANAGE */
        private Integer permLevel;
    }

    @Data
    public static class KbUserAuthItem {
        private Long userId;
        private Integer permLevel;
    }
}
