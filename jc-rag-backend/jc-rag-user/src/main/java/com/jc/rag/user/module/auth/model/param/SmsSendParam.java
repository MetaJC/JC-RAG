package com.jc.rag.user.module.auth.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 发送短信验证码请求参数 (验证码登录前置步骤)。
 * <p>必须携带滑动验证码 token (后端一次性消费), 防止短信接口被脚本刷量。
 *
 * @author JC
 */
public class SmsSendParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    /** 滑动验证码 token (必填, 后端一次性消费) */
    @NotBlank(message = "请先完成滑动验证")
    private String captchaToken;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
}
