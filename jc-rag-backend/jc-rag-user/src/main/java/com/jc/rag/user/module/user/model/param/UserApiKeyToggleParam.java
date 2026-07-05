package com.jc.rag.user.module.user.model.param;

import java.io.Serializable;

/**
 * 切换 API Key 启用状态请求参数 (C 端个人中心)。
 * @author JC
 */
public class UserApiKeyToggleParam implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean active;

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
