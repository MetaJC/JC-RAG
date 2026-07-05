package com.jc.rag.user.module.chat.model.vo;

import lombok.Data;

import java.util.List;

/**
 * C 端知识库选择配置 VO。
 * <p>三种模式:
 * <ul>
 *   <li>FIXED: 超管设置了默认 KB, 前端不展示下拉, options 仅有默认项</li>
 *   <li>SELECTABLE: 允许客户端从公开 KB 中自行选择, options 列出全部 clientSelectable=1 的 PUBLIC KB</li>
 *   <li>EMPTY: 没有任何可用 KB</li>
 * </ul>
 *
 * @author JC
 */
@Data
public class KbSelectConfigVO {

    /** FIXED / SELECTABLE / EMPTY */
    private String mode;

    /** 当前默认 KB (FIXED 时为该 KB, SELECTABLE 时为 null) */
    private KbOptionVO defaultKb;

    /** 可选项 (FIXED 时为 [defaultKb], SELECTABLE 时为多选, EMPTY 为空) */
    private List<KbOptionVO> options;
}
