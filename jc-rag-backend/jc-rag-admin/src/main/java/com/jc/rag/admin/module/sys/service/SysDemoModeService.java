package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.DemoConfigUpdateParam;
import com.jc.rag.admin.module.sys.model.param.RoleQuotaSaveParam;
import com.jc.rag.admin.module.sys.model.vo.DemoConfigVO;
import com.jc.rag.admin.module.sys.model.vo.QuotaCheckVO;
import com.jc.rag.admin.module.sys.model.vo.RoleQuotaVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;

import java.util.List;

/**
 * 演示模式 / 角色配额 / 用户用量检查 Service 接口。
 * @author JC
 */
public interface SysDemoModeService {

    DemoConfigVO getDemoConfig();

    DemoConfigVO updateDemoConfig(DemoConfigUpdateParam request);

    List<RoleQuotaVO> listRoleQuotas();

    BaseOpResultVO saveRoleQuota(RoleQuotaSaveParam request);

    QuotaCheckVO checkQuota(Long userId);

    void recordUsage(Long userId, int tokensUsed);
}
