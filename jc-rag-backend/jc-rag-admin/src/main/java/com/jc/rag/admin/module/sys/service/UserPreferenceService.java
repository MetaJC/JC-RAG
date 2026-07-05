package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.UserPreferenceUpdateParam;
import com.jc.rag.admin.module.sys.model.vo.UserPreferenceVO;

/**
 * 当前用户偏好 Service。
 * <p>底层复用 sys_config 表, config_group = 'user_pref', config_key = '{userId}.{preferenceKey}'。
 *
 * @author JC
 */
public interface UserPreferenceService {

    /** 加载当前用户全部偏好 (含默认值填充) */
    UserPreferenceVO load(Long userId);

    /**
     * 更新当前用户偏好 (按 param 中非空字段逐项更新, null 字段不动)。
     * @return 更新后的完整 VO
     */
    UserPreferenceVO update(Long userId, UserPreferenceUpdateParam param);

    /** 记忆文件路径默认: ~/.jc/jc-rag/memory/user_profile.md */
    String defaultMemoryFilePath(Long userId);
}
