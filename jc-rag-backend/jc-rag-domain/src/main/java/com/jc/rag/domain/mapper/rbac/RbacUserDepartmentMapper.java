package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacUserDepartmentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户-部门关联 Mapper (多对多)。
 * <p>按用户查部门列表, 按部门查用户列表; 替换 / 同步主部门.
 *
 * @author JC
 */
public interface RbacUserDepartmentMapper extends BaseMapper<RbacUserDepartmentEntity> {

    /** 按用户 ID 查其全部关联的部门 ID 列表 */
    List<Long> selectDepartmentIdsByUserId(@Param("userId") Long userId);

    /** 按部门 ID 列表查关联的用户 ID 列表 (可传入多个, 重复去重) */
    List<Long> selectUserIdsByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);

    /**
     * 按部门 ID 递归查其全部后代部门关联的用户 (含子级、孙级等) 的用户 ID 列表.
     * <p>用于"部门管理 - 分配用户" 列表回显; 支持级联到子部门.
     */
    List<Long> selectUserIdsByDepartmentIdRecursive(@Param("departmentId") Long departmentId);

    /**
     * 查询某部门 (含后代) 关联的用户详细信息 (Map 形式, 便于字段裁剪).
     * <p>用于"分配用户"弹窗回显 + 排除已禁用的"伪关联".
     * <p>字段: id, username, nickname, user_type, status.
     */
    List<Map<String, Object>> selectUsersByDepartmentId(@Param("departmentId") Long departmentId);

    /** 按用户 ID 删除其全部部门关联 (主部门 + 兼职) */
    int deleteByUserId(@Param("userId") Long userId);

    /** 批量插入用户-部门关联 (用于 replaceUserDepartments) */
    int insertBatch(@Param("list") List<RbacUserDepartmentEntity> list);

    /** 查询该部门及其所有后代的部门 ID (递归包含子级/孙级) */
    List<Long> selectDescendantDepartmentIds(@Param("departmentId") Long departmentId);
}
