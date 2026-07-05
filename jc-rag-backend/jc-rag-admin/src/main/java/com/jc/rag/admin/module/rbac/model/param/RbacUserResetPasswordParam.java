package com.jc.rag.admin.module.rbac.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码请求参数。
 *
 * @author JC
 */
@Data
public class RbacUserResetPasswordParam {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在 6-32 位之间")
    private String password;
}
