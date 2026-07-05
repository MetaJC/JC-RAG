package com.jc.rag.domain.mapper.sys;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.sys.SysConfigEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统运行时配置 Mapper。
 *
 * @author JC
 */
public interface SysConfigMapper extends BaseMapper<SysConfigEntity> {

    /** 全表加载 key→value 映射 (RuntimeConfigService 启动加载使用) */
    List<Map<String, Object>> listAllKeyValue();

    /** 按 config_key 查单条 config_value */
    String selectStringValue(@Param("configKey") String configKey);

    /** UPSERT 单条配置 */
    int upsertConfig(@Param("configGroup") String configGroup,
                     @Param("configKey") String configKey,
                     @Param("configValue") String configValue,
                     @Param("valueType") String valueType,
                     @Param("encrypted") Integer encrypted,
                     @Param("remark") String remark);
}
