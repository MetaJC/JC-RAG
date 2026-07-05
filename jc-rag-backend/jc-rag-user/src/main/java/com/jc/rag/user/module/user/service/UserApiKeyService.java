package com.jc.rag.user.module.user.service;

import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.user.module.user.model.param.UserApiKeySaveParam;
import com.jc.rag.user.module.user.model.vo.UserApiKeyVO;

import java.util.List;

/**
 * 用户 API Key 业务接口
 *
 * @author JC
 */
public interface UserApiKeyService {

    /**
     * 查询当前用户的所有 API Key
     */
    List<UserApiKeyVO> listByUserId(Long userId);

    /**
     * 新增或更新 API Key（按 provider 维度）
     */
    BaseOpResultVO saveOrUpdate(Long userId, UserApiKeySaveParam param);

    /**
     * 删除 API Key（限定当前用户）
     */
    BaseOpResultVO remove(Long userId, Long id);

    /**
     * 启用/禁用 API Key
     */
    BaseOpResultVO toggle(Long userId, Long id, boolean active);
}
