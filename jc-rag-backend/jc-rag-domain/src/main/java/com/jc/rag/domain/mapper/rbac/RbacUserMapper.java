package com.jc.rag.domain.mapper.rbac;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.rbac.RbacUserEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper。
 * <p>必须继承通用 BaseMapper，禁止直接继承 MP BaseMapper。
 *
 * @author JC
 */
public interface RbacUserMapper extends BaseMapper<RbacUserEntity> {

    long countUsers(@Param("keyword") String keyword,
                    @Param("excludeDeveloper") boolean excludeDeveloper,
                    @Param("deptId") Long deptId,
                    @Param("descendantDeptIds") List<Long> descendantDeptIds);

    /**
     * 分页查询用户 (含角色聚合)。
     * <p>调用方需用 {@code new Page<>(pageNum, pageSize, false)} 关闭自动 count，
     * 并在外部调用 {@link #countUsers} 设置 total (因 GROUP BY MP 自动 count 不可用)。
     * <p>deptId 过滤: 传入目标部门 ID, 会通过 descendantDeptIds 递归包含所有子部门用户。
     */
    IPage<Map<String, Object>> pageUsers(IPage<Map<String, Object>> page,
                                         @Param("keyword") String keyword,
                                         @Param("excludeDeveloper") boolean excludeDeveloper,
                                         @Param("deptId") Long deptId,
                                         @Param("descendantDeptIds") List<Long> descendantDeptIds);

    /** 全量导出 (无分页)。 */
    List<Map<String, Object>> listUsers(@Param("keyword") String keyword,
                                        @Param("excludeDeveloper") boolean excludeDeveloper);

    /**
     * 更新用户密码哈希
     * @param userId 用户 ID
     * @param passwordHash 新密码 BCrypt 哈希
     */
    int updatePassword(@Param("userId") Long userId,
                       @Param("passwordHash") String passwordHash);

    int updatePasswordByUsername(@Param("username") String username,
                                 @Param("passwordHash") String passwordHash);

    /** 查指定用户绑定的全部 roleId */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /** 表存在性探活（兼容旧库 sys_user / 新库 rbac_user） */
    int probeUserTable(@Param("tableName") String tableName);

    /** 认证加载：id/username/password_hash/nickname/user_type, status=1, deleted=0 */
    Map<String, Object> selectAuthInfo(@Param("username") String username);

    /** 查用户 id（注册查重） */
    Long selectIdByUsername(@Param("username") String username);

    /** 查用户名（按 id）。用于 super_admin 保留账号校验, 不存在返回 null */
    String selectUsernameById(@Param("userId") Long userId);

    /** 注册：插入 NORMAL 用户 */
    int insertNormalUser(@Param("username") String username,
                         @Param("passwordHash") String passwordHash,
                         @Param("nickname") String nickname,
                         @Param("email") String email);

    /** 登录成功：清零失败次数 + 更新最后登录时间 */
    int markLoginSuccess(@Param("userId") Long userId);

    /** 登录失败：失败次数 +1 */
    int incrementLoginFail(@Param("username") String username);

    /** 查询登录失败次数（用户不存在返回 0） */
    Integer selectLoginFailCount(@Param("username") String username);

    /**
     * 单级部门直接关联用户 (仅 rbac_user_department 表 department_id=#{departmentId} 命中).
     * <p>不递归, 不查主部门冗余字段, 不含子部门用户; 已排除 developer / super_admin.
     * <p>用于"部门管理→显示部门用户"开关展开时拉取.
     */
    List<Map<String, Object>> listUsersByDepartmentIdDirectly(@Param("departmentId") Long departmentId);

    /**
     * 单用户详情 (含部门名 / 登录时间等, 不含 passwordHash).
     * <p>用于"部门树内点用户 nickname → 弹窗显示用户详情".
     */
    Map<String, Object> selectUserDetailById(@Param("userId") Long userId);
}
