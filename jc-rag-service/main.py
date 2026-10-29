# Copyright 2024-2026 OpenJC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# 本文件来源于 JC RAG Community Edition
# 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
# 二次开发请保留此出处声明。


"""
JC RAG 社区版独立启动入口 (薄壳).

@author JC
@since 2026-07-15 v1.2: app 定义迁移到 app/main.py, 本文件改为薄壳
@deprecated 推荐用: uvicorn app.main:app --host 0.0.0.0 --port 8000
"""
from __future__ import annotations

import uvicorn

from app.core.config import get_settings

settings = get_settings()

# 从包内入口导入 app 实例, 供 uvicorn 引用
from app.main import app  # noqa: E402,F401  (uvicorn 引用 main:app)


if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.app.host,
        port=settings.app.port,
        reload=False,
    )
