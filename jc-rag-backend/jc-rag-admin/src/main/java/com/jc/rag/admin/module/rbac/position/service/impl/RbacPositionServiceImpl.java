package com.jc.rag.admin.module.rbac.position.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.admin.module.rbac.position.model.param.RbacPositionQueryParam;
import com.jc.rag.admin.module.rbac.position.model.param.RbacPositionSaveParam;
import com.jc.rag.admin.module.rbac.position.model.vo.RbacPositionVO;
import com.jc.rag.admin.module.rbac.position.service.RbacPositionService;
import com.jc.rag.admin.module.rbac.service.AbstractRbacService;
import com.jc.rag.common.exception.DataConflictException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.common.model.base.BaseServiceImpl;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.domain.entity.rbac.RbacDepartmentEntity;
import com.jc.rag.domain.entity.rbac.RbacPositionEntity;
import com.jc.rag.domain.mapper.rbac.RbacDepartmentMapper;
import com.jc.rag.domain.mapper.rbac.RbacPositionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 岗位管理实现 (RBAC 组织维度)。
 * <p>继承通用 BaseServiceImpl 获得 MP 全部 CRUD, 本类只写岗位管理特有业务。
 * <p>level 1=初级 / 2=中级 / 3=高级, levelText 在此模块内自行 switch 解析 (不在 AbstractRbacService 中)。
 *
 * @author JC
 */
@Slf4j
@Service
public class RbacPositionServiceImpl
        extends BaseServiceImpl<RbacPositionMapper, RbacPositionEntity>
        implements RbacPositionService {

    private final AbstractRbacService rbac;
    private final RbacDepartmentMapper departmentMapper;

    public RbacPositionServiceImpl(AbstractRbacService rbac,
                                   RbacDepartmentMapper departmentMapper) {
        this.rbac = rbac;
        this.departmentMapper = departmentMapper;
    }

    @Override
    public PageResponse<RbacPositionVO> pageList(RbacPositionQueryParam query) {
        String like = buildLike(query.getKeyword());
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = baseMapper.pagePositions(page, like, query.getStatus(), query.getDepartmentId(), query.getLevel());
        page.setTotal(baseMapper.countPositions(like, query.getStatus(), query.getDepartmentId(), query.getLevel()));
        List<RbacPositionVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResponse.of(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public RbacPositionVO getDetail(Long id) {
        Map<String, Object> row = baseMapper.selectPositionById(id);
        if (row == null || row.isEmpty()) {
            throw new ResourceNotFoundException("rbac.position.notfound", id);
        }
        return toVO(row);
    }

    @Override
    public List<RbacPositionVO> listActive() {
        return this.list(new QueryWrapper<RbacPositionEntity>().eq("status", 1)
                        .orderByAsc("department_id", "sort", "id"))
                .stream()
                .map(this::toEntityVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long create(RbacPositionSaveParam param) {
        assertDepartmentExists(param.getDepartmentId());
        assertCodeUnique(param.getCode(), null);
        RbacPositionEntity e = new RbacPositionEntity();
        e.setDepartmentId(param.getDepartmentId());
        e.setName(param.getName());
        e.setCode(param.getCode());
        e.setLevel(param.getLevel());
        e.setSort(param.getSort() == null ? 0 : param.getSort());
        e.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        e.setRemark(param.getRemark());
        save(e);
        return e.getId();
    }

    @Override
    @Transactional
    public boolean update(Long id, RbacPositionSaveParam param) {
        RbacPositionEntity e = getById(id);
        if (e == null) {
            throw new ResourceNotFoundException("rbac.position.notfound", id);
        }
        if (param.getCode() != null && !param.getCode().equals(e.getCode())) {
            assertCodeUnique(param.getCode(), id);
            e.setCode(param.getCode());
        }
        if (param.getDepartmentId() != null && !param.getDepartmentId().equals(e.getDepartmentId())) {
            assertDepartmentExists(param.getDepartmentId());
            e.setDepartmentId(param.getDepartmentId());
        }
        if (param.getName() != null) e.setName(param.getName());
        if (param.getLevel() != null) e.setLevel(param.getLevel());
        if (param.getSort() != null) e.setSort(param.getSort());
        if (param.getStatus() != null) e.setStatus(param.getStatus());
        if (param.getRemark() != null) e.setRemark(param.getRemark());
        return updateById(e);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        RbacPositionEntity e = getById(id);
        if (e == null) {
            throw new ResourceNotFoundException("rbac.position.notfound", id);
        }
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        RbacPositionEntity e = getById(id);
        if (e == null) {
            throw new ResourceNotFoundException("rbac.position.notfound", id);
        }
        e.setStatus(status);
        return updateById(e);
    }

    // ==================== 私有 ====================

    private void assertCodeUnique(String code, Long excludeId) {
        long n = baseMapper.countByCode(code, excludeId);
        if (n > 0) {
            throw new DataConflictException("rbac.position.codeDuplicate", code);
        }
    }

    private void assertDepartmentExists(Long departmentId) {
        if (departmentId == null) return;
        if (departmentMapper.selectById(departmentId) == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", departmentId);
        }
    }

    private String buildLike(String keyword) {
        return PageHelper.buildLikeClause(keyword);
    }

    private String levelText(Integer level) {
        if (level == null) return "";
        return switch (level) {
            case 1 -> "初级";
            case 2 -> "中级";
            case 3 -> "高级";
            default -> String.valueOf(level);
        };
    }

    /** Map → VO (SQL 查询路径) */
    private RbacPositionVO toVO(Map<String, Object> row) {
        RbacPositionVO v = new RbacPositionVO();
        v.setId(toLong(row.get("id")));
        v.setDepartmentId(toLong(row.get("departmentId")));
        v.setDepartmentName((String) row.get("departmentName"));
        v.setName((String) row.get("name"));
        v.setCode((String) row.get("code"));
        v.setLevel(toInt(row.get("level")));
        v.setLevelText(levelText(v.getLevel()));
        v.setSort(toInt(row.get("sort")));
        v.setStatus(toInt(row.get("status")));
        v.setStatusText(rbac.statusText(v.getStatus()));
        v.setRemark((String) row.get("remark"));
        return v;
    }

    /** Entity → VO (listActive 路径) */
    private RbacPositionVO toEntityVO(RbacPositionEntity e) {
        RbacPositionVO v = new RbacPositionVO();
        v.setId(e.getId());
        v.setDepartmentId(e.getDepartmentId());
        v.setName(e.getName());
        v.setCode(e.getCode());
        v.setLevel(e.getLevel());
        v.setLevelText(levelText(e.getLevel()));
        v.setSort(e.getSort());
        v.setStatus(e.getStatus());
        v.setStatusText(rbac.statusText(e.getStatus()));
        v.setRemark(e.getRemark());
        RbacDepartmentEntity dept = departmentMapper.selectById(e.getDepartmentId());
        if (dept != null) v.setDepartmentName(dept.getName());
        return v;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.valueOf(o.toString());
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.valueOf(o.toString());
    }
}
