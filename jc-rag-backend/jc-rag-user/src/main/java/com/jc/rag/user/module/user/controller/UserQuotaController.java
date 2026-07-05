package com.jc.rag.user.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.entity.chat.UserApiKeyEntity;
import com.jc.rag.domain.entity.chat.UserQuotaUsageEntity;
import com.jc.rag.domain.mapper.chat.UserApiKeyMapper;
import com.jc.rag.domain.mapper.chat.UserQuotaUsageMapper;
import com.jc.rag.domain.mapper.sys.DemoConfigMapper;
import com.jc.rag.user.module.user.model.vo.QuotaCheckVO;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 用户配额查询 Controller (C 端个人中心)。
 * <p>查询当前用户今日是否被演示模式配额限制。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/user/quota")
public class UserQuotaController {

    private final UserApiKeyMapper apiKeyMapper;
    private final UserQuotaUsageMapper quotaMapper;
    private final DemoConfigMapper demoConfigMapper;

    public UserQuotaController(UserApiKeyMapper apiKeyMapper,
                               UserQuotaUsageMapper quotaMapper,
                               DemoConfigMapper demoConfigMapper) {
        this.apiKeyMapper = apiKeyMapper;
        this.quotaMapper = quotaMapper;
        this.demoConfigMapper = demoConfigMapper;
    }

    @GetMapping
    public R<QuotaCheckVO> checkQuota(Authentication auth) {
        CoreUserDetails cu = (CoreUserDetails) auth.getPrincipal();
        QuotaCheckVO vo = new QuotaCheckVO();
        try {
            Map<String, Object> dc = demoConfigMapper.selectOne().stream()
                .findFirst().orElse(Map.of());
            boolean demoMode = ((Number) dc.getOrDefault("demo_mode_enabled", 0)).intValue() == 1;
            vo.setDemoMode(demoMode);
            if (!demoMode) {
                vo.setLimited(false);
                return R.ok(vo);
            }
            boolean hasKey = apiKeyMapper.selectCount(
                new QueryWrapper<UserApiKeyEntity>().eq("user_id", cu.getUserId()).eq("is_active", 1)) > 0;
            if (hasKey) {
                vo.setLimited(false);
                vo.setHasApiKey(true);
                return R.ok(vo);
            }
            int used = quotaMapper.selectCount(
                new QueryWrapper<UserQuotaUsageEntity>().eq("user_id", cu.getUserId()).eq("usage_date", LocalDate.now())).intValue();
            int quotaDay = ((Number) dc.getOrDefault("default_quota_per_day", 5)).intValue();
            vo.setQueryCount(used);
            vo.setQuotaPerDay(quotaDay);
            vo.setLimited(used >= quotaDay);
            vo.setHasApiKey(false);
        } catch (Exception e) {
            vo.setLimited(false);
        }
        return R.ok(vo);
    }
}
