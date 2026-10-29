/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.user.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.domain.entity.chat.UserApiKeyEntity;
import com.jc.rag.domain.mapper.chat.UserApiKeyMapper;
import com.jc.rag.user.module.user.model.param.UserApiKeySaveParam;
import com.jc.rag.user.module.user.model.vo.UserApiKeyVO;
import com.jc.rag.user.module.user.service.UserApiKeyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户 API Key 业务实现.
 * <p>多 Key 单活不变式: 允许同一用户配置多条 Key (含同平台), 但同一时刻
 * 至多一条启用; 启用某条时同事务关闭其他 Key. 提问链路按
 * is_active=1 取唯一 Key, 与该不变式严格一致.
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
    @Transactional(rollbackFor = Exception.class)
    public BaseOpResultVO saveOrUpdate(Long userId, UserApiKeySaveParam param) {
        // 编辑: 按 id + 归属校验更新参数, 保持原启用状态, 不影响其他 Key
        if (param.getId() != null) {
            UserApiKeyEntity existing = apiKeyMapper.selectOne(
                new QueryWrapper<UserApiKeyEntity>()
                    .eq("id", param.getId()).eq("user_id", userId));
            if (existing == null) {
                return BaseOpResultVO.fail(param.getId(), "记录不存在或已删除");
            }
            existing.setProvider(param.getProvider());
            // 编辑时 apiKey 留空 = 保持原 Key (列表接口不回传密钥明文)
            if (param.getApiKey() != null && !param.getApiKey().isBlank()) {
                existing.setApiKey(param.getApiKey());
            }
            existing.setBaseUrl(param.getBaseUrl());
            existing.setModelName(param.getModelName());
            apiKeyMapper.updateById(existing);
            return BaseOpResultVO.success(existing.getId(), "已更新");
        }
        // 新增: 默认禁用, 需通过启用开关手动生效 (启用时自动关闭其他 Key)
        if (param.getApiKey() == null || param.getApiKey().isBlank()) {
            return BaseOpResultVO.fail(null, "apiKey 不能为空");
        }
        UserApiKeyEntity k = new UserApiKeyEntity();
        k.setUserId(userId);
        k.setProvider(param.getProvider());
        k.setApiKey(param.getApiKey());
        k.setBaseUrl(param.getBaseUrl());
        k.setModelName(param.getModelName());
        k.setIsActive(0);
        apiKeyMapper.insert(k);
        return BaseOpResultVO.success(k.getId(), "已保存，请启用后生效");
    }

    @Override
    public BaseOpResultVO remove(Long userId, Long id) {
        int rows = apiKeyMapper.delete(new QueryWrapper<UserApiKeyEntity>().eq("id", id).eq("user_id", userId));
        return rows > 0
                ? BaseOpResultVO.success(id, "已删除")
                : BaseOpResultVO.fail(id, "记录不存在或已删除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseOpResultVO toggle(Long userId, Long id, boolean active) {
        UserApiKeyEntity k = apiKeyMapper.selectOne(
            new QueryWrapper<UserApiKeyEntity>().eq("id", id).eq("user_id", userId));
        if (k == null) {
            return BaseOpResultVO.fail(id, "记录不存在");
        }
        // 启用时先关闭该用户其他 Key, 保证同一时刻至多一条生效
        if (active) {
            apiKeyMapper.update(null,
                new UpdateWrapper<UserApiKeyEntity>()
                    .eq("user_id", userId).ne("id", id).eq("is_active", 1)
                    .set("is_active", 0));
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
