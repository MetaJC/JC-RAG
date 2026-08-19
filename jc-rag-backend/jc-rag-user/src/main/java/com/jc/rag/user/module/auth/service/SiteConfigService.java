package com.jc.rag.user.module.auth.service;

import com.jc.rag.user.module.auth.model.vo.SiteConfigVO;

/**
 * 站点配置服务接口 (用户端).
 * <p>读取 sys_config 中由管理端"系统设置"页维护的站点级配置,
 * 供登录页展示与注册/协议策略判断使用.
 *
 * @author JC
 */
public interface SiteConfigService {

    /**
     * 获取站点配置 (登录页等公开页面使用).
     * <p>配置缺失时返回内置默认值 (注册开放/协议必选/系统名 JC RAG).
     */
    SiteConfigVO getSiteConfig();

    /**
     * 是否开放用户注册 (注册接口与短信自动注册共用该开关).
     */
    boolean isRegisterOpen();
}
