package com.jc.rag.admin.module.knowledge.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库 VO。
 * <p>对应 kb_knowledge_base 表, 用于列表 / 详情 / 创建 / 更新 / 默认知识库等场景。
 *
 * @author JC
 */
@Data
public class KnowledgeBaseVO {
    private Long id;
    private String kbCode;
    private String kbName;
    private String kbDesc;
    private String visibilityScope;
    /** 公开 KB 是否允许客户端用户自行选择 (0 否 / 1 是) */
    private Integer clientSelectable;
    /** 所有者用户 ID */
    private Long ownerUserId;
    /** 当前用户对该 KB 的最大权限级别 (1 VIEW / 2 EDIT / 3 MANAGE / 0 NONE), 仅列表场景填充 */
    private Integer myPermLevel;
    private Integer status;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
