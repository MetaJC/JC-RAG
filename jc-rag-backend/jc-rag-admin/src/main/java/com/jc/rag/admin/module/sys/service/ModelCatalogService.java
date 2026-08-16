package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.vo.ModelCatalogVO;

/**
 * 模型目录服务 (三层同步机制)。
 *
 * <p>L1 内置目录 (classpath model-catalog.json, 随版本发布);
 * L2 远程目录 (可配置 URL + sys_config 缓存 + TTL, 失败自动回退);
 * L3 账户级实时刷新由 {@link ModelProviderConfigService#refreshModels(Long)} 提供。
 * @author JC
 */
public interface ModelCatalogService {

    /**
     * 获取当前生效的模型目录。
     *
     * <p>优先级: TTL 内远程缓存 &gt; 远程拉取 &gt; 过期缓存 &gt; 内置目录。
     */
    ModelCatalogVO getCatalog();

    /**
     * 强制从远程地址拉取模型目录并更新缓存。
     *
     * @throws IllegalArgumentException 未配置远程地址或拉取失败
     */
    ModelCatalogVO refreshCatalog();
}
