package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新当前用户偏好参数 (按字段可选, 只更新非空字段)。
 *
 * @author JC
 */
@Data
public class UserPreferenceUpdateParam {

    /** 记忆文件路径: 留空表示不修改 */
    private String memoryFilePath;
}
