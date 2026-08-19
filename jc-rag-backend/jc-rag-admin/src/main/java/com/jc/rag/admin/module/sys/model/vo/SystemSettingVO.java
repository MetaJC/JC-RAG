package com.jc.rag.admin.module.sys.model.vo;

import lombok.Data;

/**
 * 系统设置 VO (系统设置页读取).
 * <p>存储于 sys_config 表: 基础信息 group=SITE, 账号设置 group=AUTH.
 *
 * @author JC
 */
@Data
public class SystemSettingVO {

    // ===== 基础信息 =====

    /** 系统名称 */
    private String systemName;

    /** 系统 Logo URL */
    private String logoUrl;

    /** 版权信息 */
    private String copyright;

    // ===== 账号设置 =====

    /** 是否开放用户注册 */
    private Boolean registerOpen;

    /** 登录是否必须勾选用户协议 */
    private Boolean loginAgreeRequired;
}
