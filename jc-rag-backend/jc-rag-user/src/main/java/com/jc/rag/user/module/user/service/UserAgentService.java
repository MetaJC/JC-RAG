package com.jc.rag.user.module.user.service;

import com.jc.rag.domain.entity.rag.RagAgentEntity;
import java.util.List;

/**
 * 用户 Agent 业务接口
 *
 * @author JC
 */
public interface UserAgentService {

    /**
     * 查询已发布的 Agent 列表
     */
    List<RagAgentEntity> listPublished();
}
