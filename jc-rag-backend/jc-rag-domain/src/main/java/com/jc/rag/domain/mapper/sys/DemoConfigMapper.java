package com.jc.rag.domain.mapper.sys;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.sys.DemoConfigEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 演示模式配置 Mapper。
 *
 * @author JC
 */
public interface DemoConfigMapper extends BaseMapper<DemoConfigEntity> {

    /** 查询唯一配置 */
    List<Map<String, Object>> selectOne();

    /** 更新全部字段 */
    int updateConfig(@Param("id") Long id,
                     @Param("demoModeEnabled") Integer demoModeEnabled,
                     @Param("defaultQuotaPerDay") Integer defaultQuotaPerDay,
                     @Param("defaultTokensPerQ") Integer defaultTokensPerQ,
                     @Param("defaultTokensPerDay") Integer defaultTokensPerDay,
                     @Param("upgradeHint") String upgradeHint);

    /** 插入默认配置 */
    int insertDefault();
}
