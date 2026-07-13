package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacDepartmentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 部门 Mapper (RBAC 组织维度)。
 * <p>必须继承通用 BaseMapper, 禁止直接继承 MP BaseMapper。
 * <p>分页走 MP 内置 PaginationInnerInterceptor, 不手算 LIMIT。
 *
 * @author JC
 */
public interface RbacDepartmentMapper extends BaseMapper<RbacDepartmentEntity> {

    /**
     * 分页统计: 按 keyword / status / parentId 过滤。
     */
    long countDepartments(@Param("keyword") String keyword,
                          @Param("status") Integer status,
                          @Param("parentId") Long parentId);

    /**
     * 分页查询: 走 MP 分页拦截器, 调用方负责 setTotal。
     */
    IPage<Map<String, Object>> pageDepartments(IPage<Map<String, Object>> page,
                                              @Param("keyword") String keyword,
                                              @Param("status") Integer status,
                                              @Param("parentId") Long parentId);

    /**
     * 全量部门 (按 parent_id, sort 升序) — 用于构建组织树。
     */
    List<Map<String, Object>> listAllDepartments();

    /**
     * 单条详情 (Map 形式, 便于字段裁剪)。
     */
    Map<String, Object> selectDepartmentById(@Param("id") Long id);

    /**
     * 校验 code 唯一性 (新增 / 修改)。
     */
    long countByCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    /**
     * 删除前校验: 该部门下是否存在启用的岗位。
     */
    long countActivePositionByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 防环校验: 查给定 id 的所有后代 id (含深层).
     * <p>拖拽前判断 target 是否在 source 的子树中, 是则拒绝避免循环.
     * <p>实现: MySQL 8 递归 CTE, 兼容 utf8mb4.
     */
    List<Long> selectDescendantIds(@Param("id") Long id);

    /**
     * 查某父级下同级最大 sort 值 (用于 inner 拖入时设置 sort = max + 1).
     */
    Integer selectMaxSortByParent(@Param("parentId") Long parentId);

    /**
     * 批量更新 sort (单 SQL, foreach).
     * <p>用于兄弟节点重排, 减少 N 次单条 UPDATE.
     *
     * @param items [{id, sort}] (Map / DO 任一, 此处传 Map 简化)
     */
    int batchUpdateSort(@Param("items") List<Map<String, Object>> items);

    /**
     * 查某父级下 sort >= start 的兄弟 (含 id/sort), 用于区间重排.
     */
    List<Map<String, Object>> selectSiblingsInSortRange(@Param("parentId") Long parentId,
                                                        @Param("start") int start,
                                                        @Param("end") int end);

    /**
     * 查某父级下全部兄弟 (按 sort 升序), 用于整段重排.
     */
    List<Map<String, Object>> listSiblingsForRebuild(@Param("parentId") Long parentId);

    /**
     * 批量更新状态: 把一组 id 的 status 一次性更新.
     * <p>用于父级启停时把后代也带过来 (级联启用/禁用).
     */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);
}
