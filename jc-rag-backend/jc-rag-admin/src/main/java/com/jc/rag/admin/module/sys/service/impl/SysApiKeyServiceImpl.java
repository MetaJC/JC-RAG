package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.ApiKeySaveParam;
import com.jc.rag.admin.module.sys.model.vo.ApiKeyVO;
import com.jc.rag.admin.module.sys.service.SysApiKeyService;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.domain.mapper.chat.UserApiKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户自定义 API Key Service 实现。
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysApiKeyServiceImpl implements SysApiKeyService {

    private final UserApiKeyMapper userApiKeyMapper;

    @Override
    public List<ApiKeyVO> listUserApiKeys(Long userId, String keyword) {
        List<Map<String, Object>> rows = userApiKeyMapper.listUserApiKeys(userId, PageHelper.buildLikeClause(keyword));
        return rows.stream().map(SysApiKeyServiceImpl::toApiKeyVO).toList();
    }

    @Override
    public ApiKeyVO getActiveApiKey(Long userId, String provider) {
        List<Map<String, Object>> rows = userApiKeyMapper.getActiveApiKey(userId, provider);
        return rows.isEmpty() ? null : toApiKeyVO(rows.get(0));
    }

    @Override
    @Transactional
    public ApiKeyVO saveApiKey(Long userId, ApiKeySaveParam param) {
        List<Map<String, Object>> existing = userApiKeyMapper.findByUserAndProvider(userId, param.getProvider());
        if (!existing.isEmpty()) {
            Long id = ((Number) existing.get(0).get("id")).longValue();
            userApiKeyMapper.updateApiKey(id, param.getBaseUrl(), param.getApiKey(), param.getModelName());
            return toApiKeyVO(firstOrEmpty(userApiKeyMapper.getById(id)));
        }
        userApiKeyMapper.insertApiKey(userId, param.getProvider(), param.getBaseUrl(), param.getApiKey(), param.getModelName());
        List<Map<String, Object>> all = userApiKeyMapper.listUserApiKeys(userId, null);
        for (Map<String, Object> row : all) {
            if (param.getProvider().equals(row.get("provider"))) {
                return toApiKeyVO(row);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public BaseOpResultVO toggleApiKey(Long id, boolean active) {
        userApiKeyMapper.toggleActive(id, active);
        return BaseOpResultVO.success(id, active ? "已启用" : "已停用");
    }

    @Override
    @Transactional
    public BaseOpResultVO deleteApiKey(Long id) {
        userApiKeyMapper.deleteById(id);
        return BaseOpResultVO.success(id, "已删除");
    }

    private static Map<String, Object> firstOrEmpty(List<Map<String, Object>> list) {
        return list == null || list.isEmpty() ? Map.of() : list.get(0);
    }

    private static ApiKeyVO toApiKeyVO(Map<String, Object> row) {
        ApiKeyVO vo = new ApiKeyVO();
        vo.setId(toLong(row.get("id")));
        vo.setUserId(toLong(row.get("userId")));
        vo.setProvider((String) row.get("provider"));
        vo.setBaseUrl((String) row.get("baseUrl"));
        vo.setModelName((String) row.get("modelName"));
        vo.setActive(toBoolean(row.get("isActive")));
        vo.setCreatedAt(toLocalDateTime(row.get("createdAt")));
        vo.setUpdatedAt(toLocalDateTime(row.get("updatedAt")));
        return vo;
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }

    private static Boolean toBoolean(Object v) {
        if (v == null) return null;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return Boolean.valueOf(v.toString());
    }

    private static LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime t) return t;
        return LocalDateTime.parse(v.toString().replace(' ', 'T'));
    }
}
