package com.jc.rag.user.module.user.service;

import java.util.Map;

/**
 * 用户配额查询业务接口
 *
 * @author JC
 */
public interface UserQuotaService {

    /**
     * 检查当前用户配额情况
     */
    Map<String, Object> checkQuota(Long userId);
}
