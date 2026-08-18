package com.jc.rag.user.module.user.service.impl;

import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.user.module.user.model.param.ChangePasswordParam;
import com.jc.rag.user.module.user.model.param.UserProfileUpdateParam;
import com.jc.rag.user.module.user.model.vo.UserProfileVO;
import com.jc.rag.user.module.user.service.UserProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户个人中心业务实现
 *
 * @author JC
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final RbacUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserProfileServiceImpl(RbacUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        RbacUserEntity u = userMapper.selectById(userId);
        UserProfileVO vo = new UserProfileVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setUserType(u.getUserType());
        vo.setLastLoginTime(u.getLastLoginTime());
        vo.setCreatedAt(u.getCreatedAt());
        return vo;
    }

    @Override
    public BaseOpResultVO updateProfile(Long userId, UserProfileUpdateParam param) {
        RbacUserEntity u = userMapper.selectById(userId);
        if (u == null) {
            return BaseOpResultVO.fail(userId, "用户不存在");
        }
        if (param.getNickname() != null) u.setNickname(param.getNickname());
        if (param.getPhone() != null) u.setPhone(param.getPhone());
        if (param.getEmail() != null) u.setEmail(param.getEmail());
        userMapper.updateById(u);
        return BaseOpResultVO.success(userId, "个人信息已更新");
    }

    @Override
    public BaseOpResultVO changePassword(Long userId, ChangePasswordParam param) {
        RbacUserEntity u = userMapper.selectById(userId);
        if (u == null) {
            return BaseOpResultVO.fail(userId, "用户不存在");
        }
        if (!passwordEncoder.matches(param.getOldPassword(), u.getPasswordHash())) {
            return BaseOpResultVO.fail(userId, "旧密码不正确");
        }
        u.setPasswordHash(passwordEncoder.encode(param.getNewPassword()));
        userMapper.updateById(u);
        return BaseOpResultVO.success(userId, "密码已修改");
    }
}
