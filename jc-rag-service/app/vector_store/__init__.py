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
向量存储抽象层 - 社区版副本 (与 pro 同源, 手动保持一致).

@author JC
@since 2026-07-16
"""
from __future__ import annotations

from .base import VectorStore
from .factory import get_vector_store, reset_vector_store, set_vector_store
from .filter_expr import FilterCondition, FilterExpr, OrGroup

__all__ = [
    "FilterCondition",
    "FilterExpr",
    "OrGroup",
    "VectorStore",
    "get_vector_store",
    "reset_vector_store",
    "set_vector_store",
]
