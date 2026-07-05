package com.jc.rag.domain.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.user.UserCustomAgentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * C 端用户自定义智能体 Mapper。
 *
 * @author JC
 */
@Mapper
public interface UserCustomAgentMapper extends BaseMapper<UserCustomAgentEntity> {
}
