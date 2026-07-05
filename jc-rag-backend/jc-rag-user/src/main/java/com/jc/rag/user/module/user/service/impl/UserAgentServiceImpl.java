package com.jc.rag.user.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.domain.entity.rag.RagAgentEntity;
import com.jc.rag.domain.mapper.rag.RagAgentMapper;
import com.jc.rag.user.module.user.service.UserAgentService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户 Agent 业务实现
 *
 * @author JC
 */
@Service
public class UserAgentServiceImpl implements UserAgentService {

    private final RagAgentMapper agentMapper;

    public UserAgentServiceImpl(RagAgentMapper agentMapper) {
        this.agentMapper = agentMapper;
    }

    @Override
    public List<RagAgentEntity> listPublished() {
        return agentMapper.selectList(new QueryWrapper<RagAgentEntity>().eq("status", 1));
    }
}
