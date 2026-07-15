package com.jc.rag.admin.module.rbac.department.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentQueryParam;
import com.jc.rag.admin.module.rbac.department.model.param.RbacDepartmentSaveParam;
import com.jc.rag.admin.module.rbac.department.model.vo.RbacDepartmentCascadeResult;
import com.jc.rag.admin.module.rbac.department.model.vo.RbacDepartmentVO;
import com.jc.rag.admin.module.rbac.department.service.RbacDepartmentService;
import com.jc.rag.admin.module.rbac.service.AbstractRbacService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.exception.DataConflictException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.common.model.base.BaseServiceImpl;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.domain.entity.rbac.RbacDepartmentEntity;
import com.jc.rag.domain.mapper.rbac.RbacDepartmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门管理实现 (RBAC 组织维度)。
 * <p>继承通用 BaseServiceImpl 获得 MP 全部 CRUD, 本类只写部门管理特有业务。
 * <p>拼树规则: parentId=0/根 视为一级, 按 sort 升序; 删除时校验子部门 + 启用岗位。
 *
 * @author JC
 */
@Slf4j
@Service
public class RbacDepartmentServiceImpl
        extends BaseServiceImpl<RbacDepartmentMapper, RbacDepartmentEntity>
        implements RbacDepartmentService {

    private final AbstractRbacService rbac;

    public RbacDepartmentServiceImpl(AbstractRbacService rbac) {
        this.rbac = rbac;
    }

    @Override
    public PageResponse<RbacDepartmentVO> pageList(RbacDepartmentQueryParam query) {
        String like = buildLike(query.getKeyword());
        IPage<Map<String, Object>> page = new Page<>(query.getPageNum(), query.getPageSize(), false);
        page = baseMapper.pageDepartments(page, like, query.getStatus(), query.getParentId());
        page.setTotal(baseMapper.countDepartments(like, query.getStatus(), query.getParentId()));
        List<RbacDepartmentVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResponse.of(records, page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    @Override
    public RbacDepartmentVO getDetail(Long id) {
        Map<String, Object> row = baseMapper.selectDepartmentById(id);
        if (row == null || row.isEmpty()) {
            throw new ResourceNotFoundException("rbac.department.notfound", id);
        }
        RbacDepartmentVO vo = toVO(row);
        vo.setPositionCount(baseMapper.countActivePositionByDepartmentId(id));
        return vo;
    }

    @Override
    public List<RbacDepartmentVO> listTree() {
        List<Map<String, Object>> all = baseMapper.listAllDepartments();
        if (all.isEmpty()) return List.of();
        List<RbacDepartmentVO> nodes = all.stream().map(this::toVO).collect(Collectors.toList());
        return buildTree(nodes);
    }

    @Override
    public RbacDepartmentVO getRootDepartment() {
        Map<String, Object> row = baseMapper.selectRootDepartment();
        if (row == null || row.isEmpty()) return null;
        RbacDepartmentVO vo = toVO(row);
        // 当前用户是否可编辑公司名称: 仅 developer / super_admin 拥有权限
        vo.setCanEdit(rbac.isDeveloper() || rbac.isSuperAdmin());
        return vo;
    }

    @Override
    @Transactional
    public boolean updateCompanyName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new BizException(ResultCode.PARAM_INVALID, "公司名称不能为空");
        }
        RbacDepartmentVO root = getRootDepartment();
        if (root == null) {
            throw new ResourceNotFoundException("rbac.department.root.notfound", null);
        }
        rbac.assertCanEditRootDepartment();
        RbacDepartmentEntity e = getById(root.getId());
        if (e == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", root.getId());
        }
        e.setName(newName.trim());
        return updateById(e);
    }

    @Override
    @Transactional
    public Long create(RbacDepartmentSaveParam param) {
        long parentId = param.getParentId() == null ? 0L : param.getParentId();
        // 业务层禁止创建顶级 (parent_id=0).
        // <p>顶级 (parent_id=0) 仅允许唯一节点 = 公司名称, 只能由 Flyway / 系统初始化创建.
        // <p>前端必须挂到具体部门 (公司名称或更低) 下.
        if (parentId == 0L) {
            throw new BizException(ResultCode.PARAM_INVALID, "业务层禁止创建顶级部门; 顶级仅允许唯一的公司名称节点");
        }
        RbacDepartmentEntity e = new RbacDepartmentEntity();
        e.setParentId(parentId);
        e.setName(param.getName());
        // code 字段前端不再管理, 后端保留兼容: 仅在显式传入且非空时写入
        if (param.getCode() != null && !param.getCode().isBlank()) {
            e.setCode(param.getCode());
        }
        e.setSort(param.getSort() == null ? 0 : param.getSort());
        e.setStatus(param.getStatus() == null ? 1 : param.getStatus());
        e.setRemark(param.getRemark());
        save(e);
        return e.getId();
    }

    @Override
    @Transactional
    public boolean update(Long id, RbacDepartmentSaveParam param) {
        RbacDepartmentEntity e = getById(id);
        if (e == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", id);
        }
        if (param.getParentId() != null) {
            if (param.getParentId().equals(id)) {
                throw new BizException(ResultCode.PARAM_INVALID, "父级部门不能为自身");
            }
            // 业务层禁止改为顶级 (parent_id=0); 顶级=公司名称, 唯一, 不可由 update 改
            if (param.getParentId() == 0L) {
                throw new BizException(ResultCode.PARAM_INVALID, "业务层禁止改为顶级部门; 顶级仅允许唯一的公司名称节点");
            }
            e.setParentId(param.getParentId());
        }
        if (param.getName() != null) e.setName(param.getName());
        if (param.getSort() != null) e.setSort(param.getSort());
        if (param.getStatus() != null) e.setStatus(param.getStatus());
        if (param.getRemark() != null) e.setRemark(param.getRemark());
        return updateById(e);
    }

    @Override
    @Transactional
    public boolean delete(Long id) {
        RbacDepartmentEntity e = getById(id);
        if (e == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", id);
        }
        // 顶级 (公司名称) 节点不允许删除, 保护唯一根
        if (e.getParentId() != null && e.getParentId() == 0L) {
            throw new BizException(ResultCode.PARAM_INVALID, "顶级 (公司名称) 节点不允许删除");
        }
        long childCount = count(new QueryWrapper<RbacDepartmentEntity>().eq("parent_id", id));
        if (childCount > 0) {
            throw new DataConflictException("rbac.department.hasChildren", id, childCount);
        }
        // V34 后岗位与部门解耦, 不再校验 activePos (countActivePositionByDepartmentId 固定返回 0)
        return removeById(id);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, Integer status) {
        RbacDepartmentEntity e = getById(id);
        if (e == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", id);
        }
        // 顶级部门状态切换需高级权限
        if (e.getParentId() != null && e.getParentId() == 0L) {
            rbac.assertCanEditRootDepartment();
        }
        e.setStatus(status);
        return updateById(e);
    }

    @Override
    @Transactional
    public RbacDepartmentCascadeResult updateStatusCascade(Long id, Integer status) {
        RbacDepartmentEntity root = getById(id);
        if (root == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", id);
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_INVALID, "status 必须是 0 或 1");
        }

        // 1. 找所有后代 id (含子级、孙级... 严格后代, 不含自身)
        List<Long> descendantIds = baseMapper.selectDescendantIds(id);
        // 2. 把自身放最前
        List<Long> allIds = new ArrayList<>(descendantIds.size() + 1);
        allIds.add(id);
        allIds.addAll(descendantIds);
        // 3. 单 SQL 批量更新
        baseMapper.batchUpdateStatus(allIds, status);

        // 4. 收集受影响名称, 按 id 升序便于前端按层级展示
        List<String> names = new ArrayList<>(allIds.size());
        for (Long did : allIds) {
            RbacDepartmentEntity d = getById(did);
            if (d != null) names.add(d.getName());
        }

        RbacDepartmentCascadeResult r = new RbacDepartmentCascadeResult();
        r.setRootId(id);
        r.setRootName(root.getName());
        r.setStatus(status);
        r.setAffectedCount(allIds.size());
        r.setAffectedNames(names);
        return r;
    }

    // ============================================================
    // 拖拽调整部门结构 (含防环 / 兄弟重排)
    // ============================================================

    /** 拖拽位置枚举: 严格匹配前端传值 */
    private static final String POS_BEFORE = "before";
    private static final String POS_AFTER = "after";
    private static final String POS_INNER = "inner";

    @Override
    @Transactional
    public boolean move(Long id, Long targetId, String position) {
        // 1. 基本校验
        if (id == null || targetId == null) {
            throw new BizException(ResultCode.PARAM_INVALID, "id / targetId 不能为空");
        }
        if (id.equals(targetId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "不能拖到自己");
        }
        if (position == null
                || !(POS_BEFORE.equals(position) || POS_AFTER.equals(position) || POS_INNER.equals(position))) {
            throw new BizException(ResultCode.PARAM_INVALID, "position 必须是 before/after/inner");
        }
        RbacDepartmentEntity src = getById(id);
        RbacDepartmentEntity tgt = getById(targetId);
        if (src == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", id);
        }
        if (tgt == null) {
            throw new ResourceNotFoundException("rbac.department.notfound", targetId);
        }

        // 2. 防环: target 不能是 src 的后代 (含深层)
        List<Long> descendants = baseMapper.selectDescendantIds(id);
        if (descendants.contains(targetId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "不能拖到自身的后代节点下");
        }

        // 3. 统一计算 newParentId / newSort
        long oldParentId = src.getParentId() == null ? 0L : src.getParentId();
        long newParentId;
        int newSort;
        if (POS_INNER.equals(position)) {
            // inner: 变子级, 排到同级最后 (max+1)
            newParentId = tgt.getId();
            Integer max = baseMapper.selectMaxSortByParent(newParentId);
            newSort = (max == null ? -1 : max) + 1;
        } else {
            // before / after: 与 target 同级
            newParentId = tgt.getParentId() == null ? 0L : tgt.getParentId();
            // 业务层禁止拖到顶级下 (顶级=公司名称 唯一根), 不允许在其外加同级
            if (newParentId == 0L) {
                throw new BizException(ResultCode.PARAM_INVALID, "业务层禁止拖到顶级下; 顶级仅允许唯一的公司名称节点");
            }
            int t = tgt.getSort() == null ? 0 : tgt.getSort();
            newSort = POS_BEFORE.equals(position) ? t : t + 1;
        }

        // 4. 旧父级压缩: 跨父级移动时, 旧兄弟的 sort 重新连续化, 避免留空洞
        if (oldParentId != newParentId) {
            compactSiblingsAfterRemoval(oldParentId, id);
        }

        // 5. 新父级腾位: [newSort, +∞) 的兄弟 sort 全部 +1
        //    对同父级 / 跨父级 / inner 都安全 (src 自身被显式排除)
        shiftSiblingsAfterInsertion(newParentId, newSort, id);

        // 6. 落位: 设置 src.parent_id / src.sort
        src.setParentId(newParentId);
        src.setSort(newSort);
        updateById(src);
        return true;
    }

    /**
     * 旧父级压缩: src 移走后, [src.sort, +∞) 范围兄弟 -1.
     */
    private void compactSiblingsAfterRemoval(Long parentId, Long removedId) {
        int start = 0; // 简化: 整段重排
        List<Map<String, Object>> rows = baseMapper.listSiblingsForRebuild(parentId);
        List<Map<String, Object>> items = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> r : rows) {
            Long rid = toLong(r.get("id"));
            if (rid == null || rid.equals(removedId)) continue;
            Map<String, Object> it = new HashMap<>();
            it.put("id", rid);
            it.put("sort", i++);
            items.add(it);
        }
        if (!items.isEmpty()) baseMapper.batchUpdateSort(items);
    }

    /**
     * 新父级插入后移位: [newSort, +∞) 范围兄弟 +1.
     */
    private void shiftSiblingsAfterInsertion(Long parentId, int newSort, Long insertedId) {
        List<Map<String, Object>> rows = baseMapper.listSiblingsForRebuild(parentId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Long rid = toLong(r.get("id"));
            if (rid == null || rid.equals(insertedId)) continue;
            int cur = toInt(r.get("sort"));
            if (cur >= newSort) {
                Map<String, Object> it = new HashMap<>();
                it.put("id", rid);
                it.put("sort", cur + 1);
                items.add(it);
            }
        }
        if (!items.isEmpty()) baseMapper.batchUpdateSort(items);
    }

    // ==================== 私有 ====================

    /** 关键字 %xxx% 包装, null/空返回 null */
    private String buildLike(String keyword) {
        return PageHelper.buildLikeClause(keyword);
    }

    /** Map → VO 转换 */
    private RbacDepartmentVO toVO(Map<String, Object> row) {
        RbacDepartmentVO v = new RbacDepartmentVO();
        v.setId(toLong(row.get("id")));
        v.setParentId(toLong(row.get("parentId")));
        v.setName((String) row.get("name"));
        v.setCode((String) row.get("code"));
        v.setSort(toInt(row.get("sort")));
        v.setStatus(toInt(row.get("status")));
        v.setStatusText(rbac.statusText(v.getStatus()));
        v.setRemark((String) row.get("remark"));
        v.setHasChildren(Boolean.FALSE);
        v.setChildren(new ArrayList<>());
        // createdAt: BaseVO 已有该字段 (带 @JsonFormat), MyBatis 读 created_at 为 java.sql.Timestamp
        Object c = row.get("createdAt");
        if (c instanceof java.sql.Timestamp ts) {
            v.setCreatedAt(ts.toLocalDateTime());
        } else if (c instanceof java.time.LocalDateTime ldt) {
            v.setCreatedAt(ldt);
        }
        return v;
    }

    /** 拼树: parentId=0 视为一级 */
    private List<RbacDepartmentVO> buildTree(List<RbacDepartmentVO> all) {
        Map<Long, List<RbacDepartmentVO>> byParent = new HashMap<>();
        for (RbacDepartmentVO n : all) {
            byParent.computeIfAbsent(n.getParentId() == null ? 0L : n.getParentId(), k -> new ArrayList<>()).add(n);
        }
        for (RbacDepartmentVO n : all) {
            List<RbacDepartmentVO> children = byParent.get(n.getId());
            if (children != null && !children.isEmpty()) {
                children.sort(Comparator.comparing(RbacDepartmentVO::getSort,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                n.setChildren(children);
                n.setHasChildren(Boolean.TRUE);
            }
        }
        List<RbacDepartmentVO> roots = byParent.getOrDefault(0L, new ArrayList<>());
        roots.sort(Comparator.comparing(RbacDepartmentVO::getSort,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return roots;
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
