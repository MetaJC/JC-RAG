package com.jc.rag.domain.mapper.sys;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.sys.SysSensitiveWordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统敏感词 Mapper。
 *
 * @author JC
 */
@Mapper
public interface SysSensitiveWordMapper extends BaseMapper<SysSensitiveWordEntity> {
}
