package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacPositionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 岗位 Mapper (RBAC 组织维度)。
 * <p>必须继承通用 BaseMapper, 禁止直接继承 MP BaseMapper。
 * <p>分页走 MP 内置 PaginationInnerInterceptor, 不手算 LIMIT。
 *
 * @author JC
 */
public interface RbacPositionMapper extends BaseMapper<RbacPositionEntity> {

    /**
     * 分页统计: 按 keyword / status / departmentId / level 过滤。
     */
    long countPositions(@Param("keyword") String keyword,
                        @Param("status") Integer status,
                        @Param("departmentId") Long departmentId,
                        @Param("level") Integer level);

    /**
     * 分页查询: 走 MP 分页拦截器, 调用方负责 setTotal。
     */
    IPage<Map<String, Object>> pagePositions(IPage<Map<String, Object>> page,
                                            @Param("keyword") String keyword,
                                            @Param("status") Integer status,
                                            @Param("departmentId") Long departmentId,
                                            @Param("level") Integer level);

    /**
     * 单条详情 (含部门名)。
     */
    Map<String, Object> selectPositionById(@Param("id") Long id);

    /**
     * 校验 code 唯一性 (新增 / 修改)。
     */
    long countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);
}
