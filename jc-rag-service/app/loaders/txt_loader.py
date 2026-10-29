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


"""文本文件加载器"""
from __future__ import annotations

from typing import Any

from .base_loader import DocumentLoader


class TxtLoader(DocumentLoader):
    """文本文件加载器"""
    
    def can_load(self, file_path: str) -> bool:
        """判断是否为文本文件"""
        return file_path.lower().endswith(('.txt', '.md', '.json', '.yaml', '.yml'))
    
    def load(self, file_path: str) -> str:
        """加载文本文件内容"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                return f.read()
        except UnicodeDecodeError:
            with open(file_path, 'r', encoding='gbk') as f:
                return f.read()
    
    def load_with_meta(self, file_path: str) -> tuple[str, dict[str, Any]]:
        """加载文本文件内容及元数据"""
        content = self.load(file_path)
        meta = {
            'file_type': 'txt',
            'encoding': 'utf-8',
            'char_count': len(content),
            'line_count': len(content.split('\n')),
        }
        return content, meta
