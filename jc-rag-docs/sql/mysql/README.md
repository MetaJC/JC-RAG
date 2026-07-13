# 历史 SQL 脚本 (已迁入 Flyway)

> **重要**: 本目录下的 SQL 文件已于 2026-07-13 全部迁移到 Flyway 主源目录, 此目录仅作历史归档.

## 迁移说明

| 项目 | 路径 |
|---|---|
| Flyway 主源 (社区版) | `jc-rag-community/jc-rag-backend/jc-rag-admin/src/main/resources/db/migration/community/` |
| Flyway 主源 (商业版 Pro 增量) | `jc-rag-pro-core/jc-rag-pro-backend/jc-rag-pro/src/main/resources/db/migration/pro/` |

## 重命名映射

| 原文件名 | Flyway V 号 |
|---|---|
| 001_jc_rag_all_in_one.sql | V1__jc_rag_all_in_one.sql |
| 002_modules.sql | V2__modules.sql |
| 003_seed_data.sql | V3__seed_data.sql |
| 004_rbac_visibility_and_role_normalization.sql | V4__rbac_visibility_and_role_normalization.sql |
| 004_management_merge_increment.sql | V4_1__management_merge_increment.sql |
| 005_rbac_arch_optimization_increment.sql | V5__rbac_arch_optimization_increment.sql |
| 006~018 | V6__xxx ~ V18__xxx |
| 019_legal_doc_and_login_fail_count.sql | V19__legal_doc_and_login_fail_count.sql |
| 019_rename_rbac_log_to_sys.sql | V19_1__rename_rbac_log_to_sys.sql |
| 020~026 | V20__xxx ~ V26__xxx |
| 027_rbac_department_position.sql | V27__rbac_department_position.sql |

## 双版本同步机制

- 商业版启动模块 (`jc-rag-pro-app-admin` / `jc-rag-pro-app-user`) 通过 Maven 多模块依赖, classpath 自动合并 `jc-rag-admin` 的 `community/` 目录
- 商业版 application.yml `spring.flyway.locations=classpath:db/migration/pro,classpath:db/migration/community` 同时加载两套
- 商业版 Flyway 启动时自动跑 V1~V28 (社区版) + V101+ (商业版增量), 物理保证商业版包含社区版所有变更

## 历史脚本 (本目录保留, 已迁出)

- 001_jc_rag_all_in_one.sql
- 002_modules.sql
- 003_seed_data.sql
- 004_rbac_visibility_and_role_normalization.sql
- 004_management_merge_increment.sql
- 005_rbac_arch_optimization_increment.sql
- 006_menu_multilevel_seed.sql
- 007_menu_extend_fields.sql
- 008_super_admin_menu_sync.sql
- 009_menu_path_align_router.sql
- 010_rbac_split_four_pages.sql
- 011_fix_skills_and_data_permissions.sql
- 012_fix_developer_role.sql
- 013_fix_rbac_split_menus.sql
- 014_disable_duplicate_menus.sql
- 015_fix_missing_menus.sql
- 016_full_fix_menus.sql
- 017_complete_menu_fix.sql
- 018_rename_sys_to_rbac.sql
- 019_legal_doc_and_login_fail_count.sql
- 019_rename_rbac_log_to_sys.sql
- 020_legal_doc_menu.sql
- 021_rbac_role_is_admin.sql
- 022_kb_permission.sql
- 023_user_memory_profile.sql
- 024_sensitive_word_user_agent_security_audit.sql
- 025_rag_agent_is_system.sql
- 026_rag_agent_content_user_prompt.sql
- 027_rbac_department_position.sql

> ⚠️ **不要直接修改本目录下的 SQL**. 所有变更请改 Flyway 目录的 V*__* 文件, 并通过 Flyway migrate 执行.
