"""API 路由聚合层.
<p>把所有业务域子 router 挂到统一 router 上, 保持 main.py 的 import 入口不变.
<p>本文件原为单一 900+ 行大文件, 2026-07-03 按业务域拆分为 5 个子模块 (system/knowledge/retrieval/chat/config),
<p>2026-07-03 新增 memory (L2 长期事实) 子模块.
<p>2026-07-03 新增 profile (L3 跨会话画像) 子模块.
<p>共享 Pydantic 模型抽至 app/api/schemas.py, 共享辅助函数抽至 app/api/deps.py.
<p>业务代码零变更, 端点 URL/参数/响应保持一致.

@author JC
"""
from fastapi import APIRouter

from app.api.routers import chat, config, knowledge, memory, profile, retrieval, system

router = APIRouter()
router.include_router(system.router)
router.include_router(knowledge.router)
router.include_router(retrieval.router)
router.include_router(chat.router)
router.include_router(memory.router)
router.include_router(profile.router)
router.include_router(config.router)

# Health is registered by the standalone Community application only. A host
# application (such as Pro) owns its own service identity and /health contract.

__all__ = ["router"]
