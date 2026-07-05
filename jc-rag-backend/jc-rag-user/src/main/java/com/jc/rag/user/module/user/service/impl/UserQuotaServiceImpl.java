package com.jc.rag.user.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.domain.mapper.sys.DemoConfigMapper;
import com.jc.rag.domain.mapper.chat.UserApiKeyMapper;
import com.jc.rag.domain.mapper.chat.UserQuotaUsageMapper;
import com.jc.rag.domain.entity.chat.UserApiKeyEntity;
import com.jc.rag.domain.entity.chat.UserQuotaUsageEntity;
import com.jc.rag.user.module.user.service.UserQuotaService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户配额查询业务实现
 *
 * @author JC
 */
@Service
public class UserQuotaServiceImpl implements UserQuotaService {

    private final UserApiKeyMapper apiKeyMapper;
    private final UserQuotaUsageMapper quotaMapper;
    private final DemoConfigMapper demoConfigMapper;

    public UserQuotaServiceImpl(UserApiKeyMapper apiKeyMapper,
                                UserQuotaUsageMapper quotaMapper,
                                DemoConfigMapper demoConfigMapper) {
        this.apiKeyMapper = apiKeyMapper;
        this.quotaMapper = quotaMapper;
        this.demoConfigMapper = demoConfigMapper;
    }

    @Override
    public Map<String, Object> checkQuota(Long userId) {
        Map<String, Object> r = new LinkedHashMap<>();
        try {
            Map<String, Object> dc = demoConfigMapper.selectOne().stream()
                .findFirst().orElse(Map.of());
            boolean demoMode = ((Number) dc.getOrDefault("demo_mode_enabled", 0)).intValue() == 1;
            r.put("demoMode", demoMode);
            if (!demoMode) {
                r.put("limited", false);
                return r;
            }
            boolean hasKey = apiKeyMapper.selectCount(
                new QueryWrapper<UserApiKeyEntity>().eq("user_id", userId).eq("is_active", 1)) > 0;
            if (hasKey) {
                r.put("limited", false);
                r.put("hasApiKey", true);
                return r;
            }
            int used = quotaMapper.selectCount(
                new QueryWrapper<UserQuotaUsageEntity>().eq("user_id", userId).eq("usage_date", LocalDate.now())).intValue();
            int quotaDay = ((Number) dc.getOrDefault("default_quota_per_day", 5)).intValue();
            r.put("queryCount", used);
            r.put("quotaPerDay", quotaDay);
            r.put("limited", used >= quotaDay);
            r.put("hasApiKey", false);
        } catch (Exception e) {
            r.put("limited", false);
        }
        return r;
    }
}
