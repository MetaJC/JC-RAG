package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.ApiKeySaveParam;
import com.jc.rag.admin.module.sys.model.vo.ApiKeyVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;

import java.util.List;

/**
 * 用户自定义 API Key Service。
 * @author JC
 */
public interface SysApiKeyService {

    /** 列出当前用户全部 API Key. */
    List<ApiKeyVO> listUserApiKeys(Long userId, String keyword);

    /** 取当前用户指定 provider 的启用中的 API Key. */
    ApiKeyVO getActiveApiKey(Long userId, String provider);

    /** 新增/更新 (按 userId+provider 唯一). */
    ApiKeyVO saveApiKey(Long userId, ApiKeySaveParam param);

    /** 切换启用状态. */
    BaseOpResultVO toggleApiKey(Long id, boolean active);

    /** 删除. */
    BaseOpResultVO deleteApiKey(Long id);
}
