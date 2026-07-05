"""子路由聚合入口.
<p>对外暴露各个业务域的 router 实例, 由 app.api.routes 统一 include_router.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from app.api.routers import chat, config, knowledge, memory, profile, retrieval, system

__all__ = ["chat", "config", "knowledge", "memory", "profile", "retrieval", "system"]
