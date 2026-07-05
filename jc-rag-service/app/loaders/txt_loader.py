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
