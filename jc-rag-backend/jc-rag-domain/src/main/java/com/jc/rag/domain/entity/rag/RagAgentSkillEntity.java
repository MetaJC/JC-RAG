package com.jc.rag.domain.entity.rag;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * Agent-Skill 关联实体。
 * <p>对应 sys_agent_skills 表, 一个 Agent 可绑定多个 Skill, sortOrder 控制执行顺序。
 *
 * @author JC
 */
@TableName("sys_agent_skills")
public class RagAgentSkillEntity {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * Agent ID
     */
    @TableField("agent_id")
    private Long agentId;
    /**
     * Skill ID
     */
    @TableField("skill_id")
    private Long skillId;
    /**
     * 排序号 (升序执行)
     */
    @TableField("sort_order")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long v) {
        id = v;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long v) {
        agentId = v;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long v) {
        skillId = v;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer v) {
        sortOrder = v;
    }
}
