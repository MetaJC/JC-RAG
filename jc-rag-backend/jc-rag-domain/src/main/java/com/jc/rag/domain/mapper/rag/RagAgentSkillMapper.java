package com.jc.rag.domain.mapper.rag;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rag.RagAgentSkillEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Agent-Skill 关联 Mapper。
 * @author JC
 */
public interface RagAgentSkillMapper extends BaseMapper<RagAgentSkillEntity> {

    int insertBatch(@Param("list") List<RagAgentSkillEntity> list);

    int deleteByAgentId(@Param("agentId") Long agentId);
}
