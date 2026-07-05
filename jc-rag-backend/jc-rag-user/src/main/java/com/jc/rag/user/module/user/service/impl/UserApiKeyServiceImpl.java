package com.jc.rag.user.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.domain.entity.chat.UserApiKeyEntity;
import com.jc.rag.domain.mapper.chat.UserApiKeyMapper;
import com.jc.rag.user.module.user.model.param.UserApiKeySaveParam;
import com.jc.rag.user.module.user.model.vo.UserApiKeyVO;
import com.jc.rag.user.module.user.service.UserApiKeyService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户 API Key 业务实现
 *
 * @author JC
 */
@Service
public class UserApiKeyServiceImpl implements UserApiKeyService {

    private final UserApiKeyMapper apiKeyMapper;

    public UserApiKeyServiceImpl(UserApiKeyMapper apiKeyMapper) {
        this.apiKeyMapper = apiKeyMapper;
    }

    @Override
    public List<UserApiKeyVO> listByUserId(Long userId) {
        return apiKeyMapper.selectList(new QueryWrapper<UserApiKeyEntity>().eq("user_id", userId))
                .stream().map(UserApiKeyServiceImpl::toVO).toList();
    }

    @Override
    public BaseOpResultVO saveOrUpdate(Long userId, UserApiKeySaveParam param) {
        UserApiKeyEntity existing = apiKeyMapper.selectOne(
            new QueryWrapper<UserApiKeyEntity>().eq("user_id", userId).eq("provider", param.getProvider()));
        if (existing != null) {
            existing.setApiKey(param.getApiKey());
            existing.setBaseUrl(param.getBaseUrl());
            existing.setModelName(param.getModelName());
            existing.setIsActive(1);
            apiKeyMapper.updateById(existing);
            return BaseOpResultVO.success(existing.getId(), "已更新");
        }
        UserApiKeyEntity k = new UserApiKeyEntity();
        k.setUserId(userId);
        k.setProvider(param.getProvider());
        k.setApiKey(param.getApiKey());
        k.setBaseUrl(param.getBaseUrl());
        k.setModelName(param.getModelName());
        k.setIsActive(1);
        apiKeyMapper.insert(k);
        return BaseOpResultVO.success(k.getId(), "已保存");
    }

    @Override
    public BaseOpResultVO remove(Long userId, Long id) {
        int rows = apiKeyMapper.delete(new QueryWrapper<UserApiKeyEntity>().eq("id", id).eq("user_id", userId));
        return rows > 0
                ? BaseOpResultVO.success(id, "已删除")
                : BaseOpResultVO.fail(id, "记录不存在或已删除");
    }

    @Override
    public BaseOpResultVO toggle(Long userId, Long id, boolean active) {
        UserApiKeyEntity k = apiKeyMapper.selectOne(
            new QueryWrapper<UserApiKeyEntity>().eq("id", id).eq("user_id", userId));
        if (k == null) {
            return BaseOpResultVO.fail(id, "记录不存在");
        }
        k.setIsActive(active ? 1 : 0);
        apiKeyMapper.updateById(k);
        return BaseOpResultVO.success(id, active ? "已启用" : "已停用");
    }

    private static UserApiKeyVO toVO(UserApiKeyEntity e) {
        UserApiKeyVO vo = new UserApiKeyVO();
        vo.setId(e.getId());
        vo.setUserId(e.getUserId());
        vo.setProvider(e.getProvider());
        vo.setBaseUrl(e.getBaseUrl());
        vo.setModelName(e.getModelName());
        vo.setActive(e.getIsActive() != null && e.getIsActive() == 1);
        return vo;
    }
}
