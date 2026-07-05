package com.jc.rag.admin.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jc.rag.admin.module.rbac.service.AbstractRbacService;
import com.jc.rag.admin.module.rbac.service.RbacMenuService;
import com.jc.rag.admin.module.rbac.service.RbacRoleService;
import com.jc.rag.admin.module.rbac.service.RbacUserService;
import com.jc.rag.admin.module.sys.model.vo.DashboardVO;
import com.jc.rag.admin.module.sys.service.DashboardService;
import com.jc.rag.domain.entity.rbac.RbacPermissionEntity;
import com.jc.rag.domain.entity.rbac.RbacRoleEntity;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import com.jc.rag.domain.mapper.rbac.RbacPermissionMapper;
import com.jc.rag.domain.mapper.rbac.RbacRoleMapper;
import com.jc.rag.domain.mapper.sys.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard 聚合实现。
 * <p>通过组合方式依赖各业务 Service + 注入 {@link AbstractRbacService} 取得当前用户上下文。
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final RbacUserService userService;
    private final RbacRoleService roleService;
    private final RbacMenuService menuService;
    private final RbacRoleMapper roleMapper;
    private final RbacPermissionMapper permissionMapper;
    private final SysOperationLogMapper operationLogMapper;
    private final AbstractRbacService rbac;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setUserCount(userService.countUsers());
        vo.setRoleCount(roleService.countRoles());
        vo.setPermissionCount(permissionMapper.selectCount(null));
        vo.setMenuCount((long) menuService.listMenus().size());
        vo.setRoles(buildRoleList());
        vo.setPermissions(buildPermissionList());
        try {
            Long todayLogs = operationLogMapper.selectCount(new QueryWrapper<SysOperationLog>()
                    .apply("DATE(created_at) = CURDATE()"));
            vo.setTodayLogCount(todayLogs == null ? 0L : todayLogs);
        } catch (Exception e) {
            vo.setTodayLogCount(0L);
        }
        vo.setServerTime(LocalDateTime.now());
        vo.setCurrentUsername(rbac.currentUsername());
        return vo;
    }

    private List<DashboardVO.RoleSummary> buildRoleList() {
        List<RbacRoleEntity> all = roleMapper.selectList(null);
        List<DashboardVO.RoleSummary> rows = new ArrayList<>(all.size());
        for (RbacRoleEntity role : all) {
            DashboardVO.RoleSummary m = new DashboardVO.RoleSummary();
            m.setId(role.getId());
            m.setName(role.getRoleName());
            m.setCode(role.getRoleCode());
            m.setDescription(role.getRoleDesc());
            m.setStatus(role.getStatus());
            rows.add(m);
        }
        return rows;
    }

    private List<DashboardVO.PermissionSummary> buildPermissionList() {
        List<RbacPermissionEntity> all = permissionMapper.selectList(null);
        List<DashboardVO.PermissionSummary> rows = new ArrayList<>(all.size());
        for (RbacPermissionEntity p : all) {
            DashboardVO.PermissionSummary m = new DashboardVO.PermissionSummary();
            m.setId(p.getId());
            m.setCode(p.getPermCode());
            m.setName(p.getPermName());
            m.setPermType(p.getPermType());
            m.setStatus(p.getStatus());
            rows.add(m);
        }
        return rows;
    }
}
