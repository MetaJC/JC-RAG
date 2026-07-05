package com.jc.rag.admin.module.sys.model.vo;

import lombok.Data;

/**
 * 当前用户偏好设置 VO。
 * <p>对应 sys_config 表 user_pref 分组, key 形如 {userId}.{preferenceKey}。
 *
 * @author JC
 */
@Data
public class UserPreferenceVO {

    /** 记忆文件路径: 用户长记忆 (Markdown) 写入位置, 默认 ~/.jc/jc-rag/memory/user_profile.md */
    private String memoryFilePath;
}
