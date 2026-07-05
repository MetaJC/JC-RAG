package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.DemoConfigUpdateParam;
import com.jc.rag.admin.module.sys.model.param.RoleQuotaSaveParam;
import com.jc.rag.admin.module.sys.model.vo.DemoConfigVO;
import com.jc.rag.admin.module.sys.model.vo.QuotaCheckVO;
import com.jc.rag.admin.module.sys.model.vo.RoleQuotaVO;
import com.jc.rag.admin.module.sys.service.SysDemoModeService;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.domain.mapper.sys.DemoConfigMapper;
import com.jc.rag.domain.mapper.sys.RoleQuotaLimitMapper;
import com.jc.rag.domain.mapper.chat.UserApiKeyMapper;
import com.jc.rag.domain.mapper.chat.UserQuotaUsageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 演示模式 / 角色配额 / 用户用量检查实现。
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDemoModeServiceImpl implements SysDemoModeService {

    private final DemoConfigMapper demoConfigMapper;
    private final RoleQuotaLimitMapper roleQuotaLimitMapper;
    private final UserQuotaUsageMapper userQuotaUsageMapper;
    private final UserApiKeyMapper userApiKeyMapper;

    @Override
    public DemoConfigVO getDemoConfig() {
        List<Map<String, Object>> list = demoConfigMapper.selectOne();
        if (list.isEmpty()) {
            demoConfigMapper.insertDefault();
            list = demoConfigMapper.selectOne();
        }
        return toDemoConfigVO(list.get(0));
    }

    @Override
    @Transactional
    public DemoConfigVO updateDemoConfig(DemoConfigUpdateParam request) {
        Map<String, Object> config = demoConfigMapper.selectOne().get(0);
        Object idVal = config.get("id");
        if (idVal == null) {
            throw new ResourceNotFoundException("sys.config.notfound", "demo_config");
        }
        demoConfigMapper.updateConfig(
                ((Number) idVal).longValue(),
                nz(request.getDemoModeEnabled(), config.get("demoModeEnabled")),
                nz(request.getDefaultQuotaPerDay(), config.get("defaultQuotaPerDay")),
                nz(request.getDefaultTokensPerQ(), config.get("defaultTokensPerQ")),
                nz(request.getDefaultTokensPerDay(), config.get("defaultTokensPerDay")),
                request.getUpgradeHint() != null ? request.getUpgradeHint() : (String) config.get("upgradeHint")
        );
        return getDemoConfig();
    }

    @Override
    public List<RoleQuotaVO> listRoleQuotas() {
        List<Map<String, Object>> rows = roleQuotaLimitMapper.listAll();
        List<RoleQuotaVO> list = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            RoleQuotaVO vo = new RoleQuotaVO();
            vo.setRoleId(toLong(row.get("roleId")));
            vo.setRoleCode((String) row.get("roleCode"));
            vo.setRoleName((String) row.get("roleName"));
            vo.setQuotaPerDay(toInt(row.get("quotaPerDay")));
            vo.setTokensPerQ(toInt(row.get("tokensPerQ")));
            vo.setTokensPerDay(toInt(row.get("tokensPerDay")));
            vo.setCanConfigureApiKey(toInt(row.get("canConfigureApiKey")));
            list.add(vo);
        }
        return list;
    }

    @Override
    @Transactional
    public BaseOpResultVO saveRoleQuota(RoleQuotaSaveParam request) {
        roleQuotaLimitMapper.upsert(
                request.getRoleId(),
                request.getQuotaPerDay() == null ? 0 : request.getQuotaPerDay(),
                request.getTokensPerQ() == null ? 0 : request.getTokensPerQ(),
                request.getTokensPerDay() == null ? 0 : request.getTokensPerDay(),
                request.getCanConfigureApiKey() == null ? 1 : request.getCanConfigureApiKey()
        );
        return BaseOpResultVO.success(request.getRoleId(), "角色配额已保存");
    }

    @Override
    public QuotaCheckVO checkQuota(Long userId) {
        QuotaCheckVO vo = new QuotaCheckVO();
        DemoConfigVO demoConfig = getDemoConfig();
        boolean demoMode = Integer.valueOf(1).equals(demoConfig.getDemoModeEnabled());
        vo.setDemoMode(demoMode);
        if (!demoMode) {
            vo.setLimited(false);
            return vo;
        }
        LocalDate today = LocalDate.now();
        int queryCount = 0;
        int tokensUsed = 0;
        List<Map<String, Object>> usageList = userQuotaUsageMapper.findByUserAndDate(userId, today);
        if (!usageList.isEmpty()) {
            queryCount = toInt(usageList.get(0).get("queryCount"));
            tokensUsed = toInt(usageList.get(0).get("tokensUsed"));
        }
        List<Long> apiKeyIds = userApiKeyMapper.findActiveIdsByUserId(userId);
        if (!apiKeyIds.isEmpty()) {
            vo.setLimited(false);
            vo.setHasApiKey(true);
            return vo;
        }
        vo.setQueryCount(queryCount);
        vo.setTokensUsed(tokensUsed);
        vo.setQuotaPerDay(demoConfig.getDefaultQuotaPerDay() == null ? 20 : demoConfig.getDefaultQuotaPerDay());
        vo.setTokensPerQ(demoConfig.getDefaultTokensPerQ() == null ? 2048 : demoConfig.getDefaultTokensPerQ());
        vo.setTokensPerDay(demoConfig.getDefaultTokensPerDay() == null ? 10000 : demoConfig.getDefaultTokensPerDay());
        vo.setLimited(queryCount >= vo.getQuotaPerDay() || tokensUsed >= vo.getTokensPerDay());
        vo.setHasApiKey(false);
        return vo;
    }

    @Override
    @Transactional
    public void recordUsage(Long userId, int tokensUsed) {
        userQuotaUsageMapper.upsertIncrement(userId, LocalDate.now(), tokensUsed);
    }

    private static DemoConfigVO toDemoConfigVO(Map<String, Object> row) {
        DemoConfigVO vo = new DemoConfigVO();
        vo.setId(toLong(row.get("id")));
        vo.setDemoModeEnabled(toInt(row.get("demoModeEnabled")));
        vo.setDefaultQuotaPerDay(toInt(row.get("defaultQuotaPerDay")));
        vo.setDefaultTokensPerQ(toInt(row.get("defaultTokensPerQ")));
        vo.setDefaultTokensPerDay(toInt(row.get("defaultTokensPerDay")));
        vo.setUpgradeHint((String) row.get("upgradeHint"));
        return vo;
    }

    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception ignored) { return 0; }
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception ignored) { return null; }
    }

    private static int nz(Integer v, Object fallback) {
        if (v != null) return v;
        return toInt(fallback);
    }
}
