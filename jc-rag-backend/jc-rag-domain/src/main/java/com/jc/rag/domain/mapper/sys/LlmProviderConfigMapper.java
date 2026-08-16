package com.jc.rag.domain.mapper.sys;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.sys.LlmProviderConfigEntity;

/**
 * 模型提供方配置 Mapper。
 *
 * @author JC
 */
public interface LlmProviderConfigMapper extends BaseMapper<LlmProviderConfigEntity> {

    /** 按提供方标识查询最新一条已保存配置 (供 Embedding 在线服务解析 endpoint/密钥使用) */
    LlmProviderConfigEntity selectLatestByProviderName(String providerName);
}
