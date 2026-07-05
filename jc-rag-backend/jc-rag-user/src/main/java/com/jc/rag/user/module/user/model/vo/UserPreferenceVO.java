package com.jc.rag.user.module.user.model.vo;

import lombok.Data;

/**
 * 当前用户偏好设置 VO (C 端)。
 * <p>对应 sys_config 表 user_pref 分组, key 形如 {userId}.{preferenceKey}。
 * <p>第一阶段实现 3 项: memoryFilePath / defaultModelCode / chatStyle, 其余预留。
 *
 * @author JC
 */
@Data
public class UserPreferenceVO {

    /**
     * 记忆文件路径: 用户长记忆 (Markdown) 写入位置。
     * <p>默认 {@code ~/.jc/jc-rag/memory/user_profile.md}:
     * <ul>
     *   <li>{@code ~} = 当前操作系统用户主目录 (Linux/macOS: {@code $HOME}, Windows: {@code %USERPROFILE%}); 由后端在落盘时展开</li>
     *   <li>{@code .jc} = 隐藏目录, 项目所有本地数据统一存放于此 (类似 {@code .config} / {@code .cache})</li>
     *   <li>{@code jc-rag/memory/} = 记忆模块专属子目录</li>
     *   <li>{@code user_profile.md} = Markdown 格式, 可用任意编辑器查看/编辑</li>
     * </ul>
     * <p>本字段仅作为"显示路径", 真正的文件系统写入在 L2 阶段实施 (届时由后端 {@link java.nio.file.Path} 做 {@code ~} 展开与目录创建)。
     */
    private String memoryFilePath;

    /** 默认模型 code (user_agent / chat_session 选模型时优先用此值); 空 = 跟随系统默认 */
    private String defaultModelCode;

    /** 回答风格: concise (简洁) / detailed (详细) / friendly (友好) / professional (专业); 默认 concise */
    private String chatStyle;

    /**
     * L2 长期记忆开关 (用户级): true 启用 / false 关闭.
     * <p>关闭后: 不再自动提取长期 fact 入库, 召回也不再注入 systemPrompt.
     * <p>默认 true; 已存在的 fact 不会自动删除 (用户在"我的记忆"页手动清空).
     */
    private Boolean enableLongMemory;

    /**
     * L3 跨会话画像开关 (用户级): true 启用 / false 关闭.
     * <p>关闭后: 不再聚合 L2 fact 提炼画像, 也不在 systemPrompt 注入画像.
     * <p>默认 true; 已存在的画像不会自动删除 (用户在"我的记忆"页可手动重置).
     * <p>与 {@link #enableLongMemory} 独立; L2 关闭时 L3 也会无数据源, 实际无效果.
     */
    private Boolean enableMemoryProfile;
}
