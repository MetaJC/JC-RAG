package com.jc.rag.user.module.user.service;

import com.jc.rag.domain.entity.chat.UserMemoryProfileEntity;

/**
 * 用户跨会话画像 Service (L3)。
 * <p>由 L2 长期事实 (ES jc_chat_memory) 周期性聚合并经 LLM 提炼成结构化画像 (五段: domain/role/skills/preferences/goals)。
 * <p>本接口负责画像的读取、注入文本构造、用户开关判断; 异步提取流程由调用方 (UserChatServiceImpl) 触发。
 *
 * @author JC
 */
public interface UserMemoryProfileService {

    /**
     * 取本用户画像 (无则返回 null, 由调用方决定兜底).
     * @param userId 用户 ID
     * @return 画像实体; 无画像时返回 null
     */
    UserMemoryProfileEntity getProfile(Long userId);

    /**
     * 构造可注入 LLM systemPrompt 的画像文本 (带 L3 前缀).
     * <p>无画像或画像为空时返回 null, 调用方应跳过注入.
     * @param userId 用户 ID
     * @return 画像提示文本; 无画像返回 null
     */
    String buildPromptText(Long userId);

    /**
     * 用户是否开启 L3 跨会话画像 (与 L2 开关独立).
     * <p>关闭时不提取也不注入, 失败/未配置一律视为开启 (不影响主流程).
     * @param userId 用户 ID
     * @return true 开启, false 关闭
     */
    boolean isEnabled(Long userId);

    /**
     * 画像提取: 调 Python /chat/profile/extract 提炼画像, 成功后写库.
     * <p>本方法由 UserChatServiceImpl 异步触发; 失败/超时仅记录日志, 不影响主对话.
     * @param userId 用户 ID
     */
    void extractProfileAsync(Long userId);

    /**
     * 重置 (清空) 本用户画像: 物理删除 user_memory_profile 行.
     * <p>下次异步触发时会重建.
     * @param userId 用户 ID
     * @return 删除行数 (0 或 1)
     */
    int resetProfile(Long userId);
}
