package com.jc.rag.user.module.chat.model.param;

import lombok.Data;

/**
 * C 端创建会话 Param。
 * <p>sessionTitle 可空: 为空时 service 层用"新会话"兜底。
 * @author JC
 */
@Data
public class SessionCreateParam {

    private String sessionTitle;

    private Long kbId;
}
