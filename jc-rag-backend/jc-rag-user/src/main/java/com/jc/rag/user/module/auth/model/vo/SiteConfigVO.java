package com.jc.rag.user.module.auth.model.vo;

import lombok.Data;

/**
 * 站点配置 VO (用户端登录页读取).
 * <p>来源 sys_config 表, 管理端"系统设置"页维护; 登录页据此控制
 * 注册入口显隐、协议勾选强制性与页面品牌信息展示.
 *
 * @author JC
 */
@Data
public class SiteConfigVO {

    /** 系统名称 */
    private String systemName;

    /** 系统 Logo URL */
    private String logoUrl;

    /** 版权信息 */
    private String copyright;

    /** 是否开放用户注册 */
    private Boolean registerOpen;

    /** 登录是否必须勾选用户协议 */
    private Boolean loginAgreeRequired;
}
