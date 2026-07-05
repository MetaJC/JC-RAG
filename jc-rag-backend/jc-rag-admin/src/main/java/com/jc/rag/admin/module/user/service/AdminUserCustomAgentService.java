package com.jc.rag.admin.module.user.service;

import com.jc.rag.admin.module.user.model.param.UserCustomAgentQueryParam;
import com.jc.rag.admin.module.user.model.vo.UserCustomAgentAdminVO;
import com.jc.rag.common.web.page.PageResponse;

/**
 * Admin 端用户智能体管理服务.
 * <p>Admin 只读 + 强制删除 + 启用/禁用, 用于审计/管理/查看.
 * @author JC
 */
public interface AdminUserCustomAgentService {

    /** 分页查询, 含创建者信息 (join rbac_user) */
    PageResponse<UserCustomAgentAdminVO> pageList(UserCustomAgentQueryParam query);

    /** 查单条 */
    UserCustomAgentAdminVO getById(Long id);

    /** 强制删除 (admin 端审计性删除) */
    boolean forceDelete(Long id);

    /** 启用/禁用 (admin 端审计性管控) */
    boolean updateStatus(Long id, Integer status);
}
