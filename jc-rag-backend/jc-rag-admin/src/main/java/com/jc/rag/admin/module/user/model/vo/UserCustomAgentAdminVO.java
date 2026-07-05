package com.jc.rag.admin.module.user.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin 端用户智能体视图 (含创建者信息, 用于审计/管理/查看).
 * <p>业务规则: Admin 端只读 + 强制删除, 不允许编辑 (用户数据归用户).
 * @author JC
 */
@Data
public class UserCustomAgentAdminVO {
    private Long id;
    private Long userId;
    /** 创建者用户名 (来自 rbac_user) */
    private String username;
    /** 创建者昵称 */
    private String nickname;
    private String agentName;
    private String content;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
