package com.jc.rag.user.module.chat.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * C 端重命名会话 Param。
 * @author JC
 */
@Data
public class SessionRenameParam {

    @NotBlank(message = "sessionTitle 不能为空")
    private String sessionTitle;
}
