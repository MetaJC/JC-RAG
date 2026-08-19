package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.SystemSettingParam;
import com.jc.rag.admin.module.sys.model.vo.SystemSettingVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统设置服务接口 (sys_config 读写).
 * <p>设置项由系统设置页维护, 用户端登录页与注册/登录链路读取生效.
 *
 * @author JC
 */
public interface SysSettingService {

    /** 查询全部系统设置 (缺失项返回内置默认值) */
    SystemSettingVO getSettings();

    /** 保存系统设置 (逐项 UPSERT sys_config) */
    @Transactional
    SystemSettingVO saveSettings(SystemSettingParam param);
}
