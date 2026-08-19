package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.SystemSettingParam;
import com.jc.rag.admin.module.sys.model.vo.SystemSettingVO;
import com.jc.rag.admin.module.sys.service.SysSettingService;
import com.jc.rag.common.constant.SysConfigKeys;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 系统设置服务实现 (sys_config 逐项 UPSERT).
 * <p>布尔配置统一以 "1"/"0" 落库, 与用户端读取约定一致.
 *
 * @author JC
 */
@Service
@RequiredArgsConstructor
public class SysSettingServiceImpl implements SysSettingService {

    /** 系统名称默认值 */
    private static final String DEFAULT_SYSTEM_NAME = "JC RAG";

    /** 配置分组: 站点基础信息 */
    private static final String GROUP_SITE = "SITE";

    /** 配置分组: 账号与安全 */
    private static final String GROUP_AUTH = "AUTH";

    private final SysConfigMapper sysConfigMapper;

    @Override
    public SystemSettingVO getSettings() {
        SystemSettingVO vo = new SystemSettingVO();
        String name = sysConfigMapper.selectStringValue(SysConfigKeys.SITE_SYSTEM_NAME);
        vo.setSystemName(name == null || name.isBlank() ? DEFAULT_SYSTEM_NAME : name);
        vo.setLogoUrl(sysConfigMapper.selectStringValue(SysConfigKeys.SITE_LOGO_URL));
        vo.setCopyright(sysConfigMapper.selectStringValue(SysConfigKeys.SITE_COPYRIGHT));
        vo.setRegisterOpen(readBoolean(SysConfigKeys.AUTH_REGISTER_OPEN, true));
        vo.setLoginAgreeRequired(readBoolean(SysConfigKeys.AUTH_LOGIN_AGREE_REQUIRED, true));
        return vo;
    }

    @Override
    public SystemSettingVO saveSettings(SystemSettingParam param) {
        sysConfigMapper.upsertConfig(GROUP_SITE, SysConfigKeys.SITE_SYSTEM_NAME,
                param.getSystemName().trim(), "STRING", 0, "系统名称");
        sysConfigMapper.upsertConfig(GROUP_SITE, SysConfigKeys.SITE_LOGO_URL,
                normalize(param.getLogoUrl()), "STRING", 0, "系统 Logo URL");
        sysConfigMapper.upsertConfig(GROUP_SITE, SysConfigKeys.SITE_COPYRIGHT,
                normalize(param.getCopyright()), "STRING", 0, "版权信息");
        sysConfigMapper.upsertConfig(GROUP_AUTH, SysConfigKeys.AUTH_REGISTER_OPEN,
                toBoolValue(param.getRegisterOpen()), "BOOLEAN", 0, "是否开放用户注册");
        sysConfigMapper.upsertConfig(GROUP_AUTH, SysConfigKeys.AUTH_LOGIN_AGREE_REQUIRED,
                toBoolValue(param.getLoginAgreeRequired()), "BOOLEAN", 0, "登录是否必须勾选用户协议");
        return getSettings();
    }

    /** 布尔转 "1"/"0" 存储值 */
    private String toBoolValue(Boolean value) {
        return Boolean.TRUE.equals(value) ? "1" : "0";
    }

    /** 空串归一: null → "" */
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    /** 读取布尔配置: "1"/"true" 为真, 缺失返回默认值 */
    private boolean readBoolean(String key, boolean defaultValue) {
        String value = sysConfigMapper.selectStringValue(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "1".equals(value.trim()) || "true".equalsIgnoreCase(value.trim());
    }
}
