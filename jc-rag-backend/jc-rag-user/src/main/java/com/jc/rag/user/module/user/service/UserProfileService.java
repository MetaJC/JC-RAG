package com.jc.rag.user.module.user.service;

import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.user.module.user.model.param.ChangePasswordParam;
import com.jc.rag.user.module.user.model.param.UserProfileUpdateParam;
import com.jc.rag.user.module.user.model.vo.UserProfileVO;

/**
 * 用户个人中心业务接口
 *
 * @author JC
 */
public interface UserProfileService {

    /**
     * 获取当前用户基本信息及当日调用次数
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 更新当前用户昵称/手机/邮箱
     */
    BaseOpResultVO updateProfile(Long userId, UserProfileUpdateParam param);

    /**
     * 修改当前用户密码
     */
    BaseOpResultVO changePassword(Long userId, ChangePasswordParam param);
}
