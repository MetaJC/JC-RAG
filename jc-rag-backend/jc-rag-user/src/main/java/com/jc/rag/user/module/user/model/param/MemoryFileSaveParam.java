package com.jc.rag.user.module.user.model.param;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存记忆文件内容 Param.
 *
 * @author JC
 */
@Data
public class MemoryFileSaveParam {

    /**
     * 新的文件内容.
     * <p>允许空字符串 (清空文件); 限制最大 1MB 防止异常写入.
     */
    @NotNull(message = "content 不能为空 (允许空字符串)")
    @Size(max = 1024 * 1024, message = "content 长度不能超过 1MB")
    private String content;
}
