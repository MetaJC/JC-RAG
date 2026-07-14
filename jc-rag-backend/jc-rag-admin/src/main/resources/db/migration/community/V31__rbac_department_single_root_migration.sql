-- =============================================================================
-- V31__rbac_department_single_root_migration.sql
-- -----------------------------------------------------------------------------
-- 目的:
--   把数据库中现存的多 parent_id=0 根节点合并为唯一根 (公司名称).
-- 业务背景:
--   之前设计允许任意 parent_id=0, 导致顶级不唯一, 部门管理页面表格内出现多个根,
--   父级选择 / 用户部门下拉等位置都出现多个根, 与"顶级=公司名称"语义冲突.
--   此次改为唯一根 (parent_id=0 仅允许"公司名称" 1 个), 业务层禁止创建顶级
--   (RbacDepartmentService.create / update 校验 parent_id != 0).
-- 表: rbac_department
-- 幂等:
--   - 若已存在 name='公司名称' 且 parent_id=0 的根, 复用之, 不重复插入
--   - 若所有现存 parent_id=0 部门都已迁移到公司根下, UPDATE 影响 0 行
--   - 可重跑
-- =============================================================================

-- 1) 查找「公司名称」根节点 (如果已存在)
SELECT id INTO @company_root_id
FROM rbac_department
WHERE parent_id = 0 AND name = '公司名称'
ORDER BY id ASC
LIMIT 1;

-- 2) 若不存在, 新建一个「公司名称」根节点
INSERT INTO rbac_department (parent_id, name, sort, status, created_at, updated_at)
SELECT 0, '公司名称', 0, 1, NOW(), NOW()
FROM DUAL
WHERE @company_root_id IS NULL;

-- 3) 重新取一次 (确保拿到刚创建的新 id)
SELECT id INTO @company_root_id
FROM rbac_department
WHERE parent_id = 0 AND name = '公司名称'
ORDER BY id ASC
LIMIT 1;

-- 4) 把所有其他 parent_id=0 部门的 parent_id 改为「公司名称」id
UPDATE rbac_department
SET parent_id = @company_root_id, updated_at = NOW()
WHERE parent_id = 0 AND id != @company_root_id;
