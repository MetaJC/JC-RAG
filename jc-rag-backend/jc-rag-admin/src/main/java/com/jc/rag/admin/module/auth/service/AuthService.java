package com.jc.rag.admin.module.auth.service;

import com.jc.rag.admin.module.auth.model.param.ChangePasswordParam;
import com.jc.rag.admin.module.auth.model.param.LoginParam;
import com.jc.rag.admin.module.auth.model.vo.AuthUserVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AuthService 接口
 *
 * @author JC
 */
public interface AuthService {

    /**
     * 登录并创建 Session。
     */
    AuthUserVO login(LoginParam request, HttpServletRequest httpServletRequest);

    /**
     * 获取当前登录用户信息。
     */
    AuthUserVO currentUser();

    /**
     * 退出登录并销毁 Session。
     */
    BaseOpResultVO logout(HttpServletRequest request, HttpServletResponse response);

    /**
     * 修改当前用户密码。
     */
    BaseOpResultVO changePassword(ChangePasswordParam request);
}
