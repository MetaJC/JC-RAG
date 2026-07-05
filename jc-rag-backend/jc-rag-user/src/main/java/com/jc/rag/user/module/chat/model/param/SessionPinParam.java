package com.jc.rag.user.module.chat.model.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * C 端会话置顶 Param。
 * @author JC
 */
@Data
public class SessionPinParam {

    @NotNull(message = "pinned 不能为空")
    private Boolean pinned;
}
