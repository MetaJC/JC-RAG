package com.jc.rag.user.module.user.model.param;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 更新 C 端用户个人信息请求参数。
 *
 * @author JC
 */
public class UserProfileUpdateParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @Size(max = 32, message = "昵称长度不能超过 32")
    private String nickname;

    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
