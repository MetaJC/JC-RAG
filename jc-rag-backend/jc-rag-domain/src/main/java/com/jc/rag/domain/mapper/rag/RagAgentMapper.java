package com.jc.rag.domain.mapper.rag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rag.RagAgentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Agent Mapper。
 * @author JC
 */
public interface RagAgentMapper extends BaseMapper<RagAgentEntity> {

    long countAgents(@Param("status") Integer status, @Param("isSystem") Integer isSystem, @Param("keyword") String keyword);

    IPage<RagAgentEntity> pageAgents(IPage<RagAgentEntity> page,
                                      @Param("status") Integer status,
                                      @Param("isSystem") Integer isSystem,
                                      @Param("keyword") String keyword);

    List<RagAgentEntity> listAgents(
            @Param("status") Integer status,
            @Param("isSystem") Integer isSystem,
            @Param("keyword") String keyword
    );

    int insertAgent(RagAgentEntity row);

    int updateAgent(@Param("id") Long id, @Param("row") Map<String, Object> row);

    int updateAgentStatus(@Param("id") Long id, @Param("status") Integer status, @Param("version") Integer version);

    int deleteAgent(@Param("id") Long id);
}
