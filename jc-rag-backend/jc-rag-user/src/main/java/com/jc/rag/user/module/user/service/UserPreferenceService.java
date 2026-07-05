package com.jc.rag.user.module.user.service;

import com.jc.rag.user.module.user.model.param.MemoryFileSaveParam;
import com.jc.rag.user.module.user.model.param.UserPreferenceUpdateParam;
import com.jc.rag.user.module.user.model.vo.MemoryFileVO;
import com.jc.rag.user.module.user.model.vo.UserPreferenceVO;

import java.util.Map;

/**
 * 当前用户偏好 Service (C 端)。
 * <p>底层复用 sys_config 表, config_group = 'user_pref', config_key = '{userId}.{preferenceKey}'。
 *
 * @author JC
 */
public interface UserPreferenceService {

    /** 加载当前用户全部偏好 (含默认值填充) */
    UserPreferenceVO load(Long userId);

    /**
     * 更新当前用户偏好 (按 param 中非空字段逐项更新, null 字段不动; 显式空字符串则清空)。
     * @return 更新后的完整 VO
     */
    UserPreferenceVO update(Long userId, UserPreferenceUpdateParam param);

    /** 记忆文件路径默认 */
    String defaultMemoryFilePath(Long userId);

    /** 回答风格默认值 */
    String defaultChatStyle();

    /**
     * 读取记忆文件 (Markdown).
     * <p>行为:
     * <ul>
     *   <li>文件存在: 返回真实内容 + 元信息</li>
     *   <li>文件不存在: <b>自动初始化</b>默认模板到磁盘, 然后返回模板内容 (autoInitialized=true)</li>
     *   <li>父目录不存在: 自动 {@code mkdir -p} 后再初始化</li>
     * </ul>
     *
     * @param userId 当前用户 ID
     * @return 记忆文件视图
     */
    MemoryFileVO readMemoryFile(Long userId);

    /**
     * 保存记忆文件内容.
     * <p>行为:
     * <ul>
     *   <li>路径取自当前用户偏好 (sys_config)</li>
     *   <li>父目录不存在: 自动 {@code mkdir -p}</li>
     *   <li>原子写: 先写临时文件再 {@code Files.move(REPLACE_EXISTING)}, 防止写一半挂掉</li>
     * </ul>
     *
     * @param userId 当前用户 ID
     * @param param  保存参数 (含 content)
     * @return 保存后的视图 (含新 lastModified / bytes)
     */
    MemoryFileVO writeMemoryFile(Long userId, MemoryFileSaveParam param);

    /**
     * 一键忘记我 (GDPR / 个保法 被遗忘权).
     * <p>物理删除本用户全部对话数据:
     * <ul>
     *   <li>chat_message (L0 短期原文)</li>
     *   <li>chat_session (L0 会话)</li>
     *   <li>chat_session_summary (L1 摘要)</li>
     *   <li>user_memory_profile (L3 画像)</li>
     *   <li>ES jc_chat_memory (L2 长期事实, 调 Python /chat/memory/forget)</li>
     *   <li>user_pref 偏好 (sys_config) + 记忆文件物理删除 (可选)</li>
     * </ul>
     * <p>账号本身 (用户表) 不删; 用户可重新登录, 只是历史数据全清.
     * <p>失败不抛异常, 仅记日志, 整体返回每步影响条数.
     *
     * @param userId 当前用户 ID
     * @return 各表影响条数 + 总计
     */
    Map<String, Object> forgetMe(Long userId);
}

