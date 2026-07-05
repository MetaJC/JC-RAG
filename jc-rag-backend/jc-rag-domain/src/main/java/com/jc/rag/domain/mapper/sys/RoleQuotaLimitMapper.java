package com.jc.rag.domain.mapper.sys;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.sys.RoleQuotaLimitEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 角色配额限制 Mapper。
 *
 * @author JC
 */
public interface RoleQuotaLimitMapper extends BaseMapper<RoleQuotaLimitEntity> {

    /** 列出全部角色配额 (JOIN rbac_role 取名称) */
    List<Map<String, Object>> listAll();

    /** UPSERT 单条 */
    int upsert(@Param("roleId") Long roleId,
               @Param("quotaPerDay") Integer quotaPerDay,
               @Param("tokensPerQ") Integer tokensPerQ,
               @Param("tokensPerDay") Integer tokensPerDay,
               @Param("canConfigureApiKey") Integer canConfigureApiKey);
}
