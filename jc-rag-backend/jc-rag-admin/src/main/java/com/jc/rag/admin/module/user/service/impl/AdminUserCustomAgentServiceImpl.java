package com.jc.rag.admin.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.admin.module.user.model.param.UserCustomAgentQueryParam;
import com.jc.rag.admin.module.user.model.vo.UserCustomAgentAdminVO;
import com.jc.rag.admin.module.user.service.AdminUserCustomAgentService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import com.jc.rag.domain.entity.user.UserCustomAgentEntity;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.domain.mapper.user.UserCustomAgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin 端用户智能体管理 Service 实现.
 * <p>含创建者 (rbac_user) 关联, 分页 + 关键词 + 状态过滤.
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserCustomAgentServiceImpl implements AdminUserCustomAgentService {

    private final UserCustomAgentMapper userCustomAgentMapper;
    private final RbacUserMapper rbacUserMapper;

    @Override
    public PageResponse<UserCustomAgentAdminVO> pageList(UserCustomAgentQueryParam query) {
        IPage<UserCustomAgentEntity> page = PageHelper.pageOf(query);
        QueryWrapper<UserCustomAgentEntity> qw = new QueryWrapper<>();
        if (query.getStatus() != null) {
            qw.eq("status", query.getStatus());
        }
        if (query.getUserId() != null) {
            qw.eq("user_id", query.getUserId());
        }
        if (query.getDateStart() != null) {
            qw.ge("created_at", query.getDateStart());
        }
        if (query.getDateEnd() != null) {
            qw.le("created_at", query.getDateEnd());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            // 关键词: 同时匹配 agent_name / content (content 单独走 SQL LIKE, 命中更广)
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like("agent_name", kw).or().like("content", kw));
        }
        qw.orderByDesc("id");
        IPage<UserCustomAgentEntity> result = userCustomAgentMapper.selectPage(page, qw);

        // 收集 userId, 批量查创建者
        List<UserCustomAgentEntity> records = result.getRecords();
        if (records.isEmpty()) {
            return PageHelper.toPageResponse(result, e -> toVO(e, null, null));
        }
        Set<Long> userIds = records.stream().map(UserCustomAgentEntity::getUserId).collect(Collectors.toSet());
        // 关键词用户名/昵称: 额外收集可能命中的用户, 用于回填展示
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            rbacUserMapper.selectList(
                new QueryWrapper<RbacUserEntity>().like("username", kw).or().like("nickname", kw)
            ).forEach(u -> userIds.add(u.getId()));
        }
        Map<Long, RbacUserEntity> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            rbacUserMapper.selectList(
                new QueryWrapper<RbacUserEntity>().in("id", userIds)
            ).forEach(u -> userMap.put(u.getId(), u));
        }
        return PageHelper.toPageResponse(result, e -> {
            RbacUserEntity u = userMap.get(e.getUserId());
            return toVO(e, u != null ? u.getUsername() : null, u != null ? u.getNickname() : null);
        });
    }

    @Override
    public UserCustomAgentAdminVO getById(Long id) {
        UserCustomAgentEntity e = userCustomAgentMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户智能体不存在: id=" + id);
        }
        RbacUserEntity u = rbacUserMapper.selectById(e.getUserId());
        return toVO(e, u != null ? u.getUsername() : null, u != null ? u.getNickname() : null);
    }

    @Override
    @Transactional
    public boolean forceDelete(Long id) {
        UserCustomAgentEntity e = userCustomAgentMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户智能体不存在: id=" + id);
        }
        return userCustomAgentMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_INVALID, "status 必须为 0(禁用) 或 1(启用)");
        }
        UserCustomAgentEntity e = userCustomAgentMapper.selectById(id);
        if (e == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户智能体不存在: id=" + id);
        }
        if (java.util.Objects.equals(e.getStatus(), status)) {
            return true;  // 幂等
        }
        UserCustomAgentEntity upd = new UserCustomAgentEntity();
        upd.setId(id);
        upd.setStatus(status);
        return userCustomAgentMapper.updateById(upd) > 0;
    }

    private UserCustomAgentAdminVO toVO(UserCustomAgentEntity e, String username, String nickname) {
        UserCustomAgentAdminVO v = new UserCustomAgentAdminVO();
        v.setId(e.getId());
        v.setUserId(e.getUserId());
        v.setUsername(username);
        v.setNickname(nickname);
        v.setAgentName(e.getAgentName());
        v.setContent(e.getContent());
        v.setStatus(e.getStatus());
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }
}
