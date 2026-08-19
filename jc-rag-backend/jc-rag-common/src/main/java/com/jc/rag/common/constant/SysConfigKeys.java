package com.jc.rag.common.constant;

/**
 * sys_config 表配置键常量 (集中定义, 避免 admin/user 两端字符串散落).
 * <p>键命名约定: {分组}.{配置项}, 与 config_group 列对应.
 *
 * @author JC
 */
public final class SysConfigKeys {

    private SysConfigKeys() {
    }

    // ===== 站点基础信息 (config_group = SITE) =====

    /** 系统名称 */
    public static final String SITE_SYSTEM_NAME = "site.system_name";

    /** 系统 Logo URL */
    public static final String SITE_LOGO_URL = "site.logo_url";

    /** 版权信息 */
    public static final String SITE_COPYRIGHT = "site.copyright";

    // ===== 账号与安全 (config_group = AUTH) =====

    /** 是否开放用户注册 (1=开放, 0=关闭; 关闭后注册接口与短信自动注册均不可用) */
    public static final String AUTH_REGISTER_OPEN = "auth.register.open";

    /** 登录是否必须勾选用户协议 (1=必须勾选, 0=隐藏协议复选框且不限制) */
    public static final String AUTH_LOGIN_AGREE_REQUIRED = "auth.login.agree_required";
}
