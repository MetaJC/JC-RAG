package com.jc.rag.admin.module.user.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin 端用户智能体分页查询参数.
 * <p>支持: 智能体名/内容/用户名/昵称 模糊 + 状态过滤 + 时间范围 + 精确 userId.
 * @author JC
 */
@Data
public class UserCustomAgentQueryParam extends PageQueryParam {
    /** 关键词: 命中 username / nickname / agent_name / content */
    private String keyword;
    /** 1=启用 0=禁用 null=全部 */
    private Integer status;
    /** 精确 userId 过滤 */
    private Long userId;
    /** 创建时间下界 (含) */
    private LocalDateTime dateStart;
    /** 创建时间上界 (含) */
    private LocalDateTime dateEnd;
}
