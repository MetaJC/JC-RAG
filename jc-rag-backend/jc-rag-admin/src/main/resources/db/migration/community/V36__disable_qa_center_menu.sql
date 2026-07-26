-- ============================================================
-- V36 禁用管理端问答中心菜单
-- <p>问答中心 (menu:qa_center) 在管理端价值有限, 长期未维护, 与 user 端问答功能重复
-- <p>前端组件 ChatCenterView.vue / api/chat.ts / types/chat.ts 已在源码中删除
-- <p>后端 ChatController / ChatService / ChatServiceImpl 整目录已删除
-- <p>本脚本仅禁用菜单 (status=0, visible=0), 保留数据, 可回滚
--
-- @author JC
-- @since 2026-07-25
-- ============================================================

UPDATE rbac_permission
SET status = 0,
    visible = 0
WHERE perm_code = 'menu:qa_center';
