package com.jc.rag.user.module.auth.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 注册结果 VO。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean success;
    private String code;
    private String message;

    public static RegisterResultVO ok() {
        RegisterResultVO vo = new RegisterResultVO();
        vo.setSuccess(Boolean.TRUE);
        vo.setMessage("注册成功");
        return vo;
    }

    public static RegisterResultVO fail(String code, String message) {
        RegisterResultVO vo = new RegisterResultVO();
        vo.setSuccess(Boolean.FALSE);
        vo.setCode(code);
        vo.setMessage(message);
        return vo;
    }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
