"""Word DOCX 文件加载器"""
from __future__ import annotations

from typing import Any

from .base_loader import DocumentLoader


class DocxLoader(DocumentLoader):
    """DOCX 文件加载器"""
    
    def can_load(self, file_path: str) -> bool:
        """判断是否为 DOCX 文件"""
        return file_path.lower().endswith('.docx')
    
    def load(self, file_path: str) -> str:
        """加载 DOCX 文件内容"""
        return self.load_with_meta(file_path)[0]
    
    def load_with_meta(self, file_path: str) -> tuple[str, dict[str, Any]]:
        """加载 DOCX 文件内容及元数据"""
        try:
            from docx import Document
        except ImportError as ex:
            raise ValueError(
                "缺少 DOCX 解析依赖 python-docx，请先执行 pip install python-docx"
            ) from ex
        
        doc = Document(file_path)
        paragraphs = [p.text for p in doc.paragraphs if p.text.strip()]
        
        content = '\n\n'.join(paragraphs)
        meta = {
            'file_type': 'docx',
            'paragraph_count': len(paragraphs),
            'char_count': len(content),
        }
        
        return content, meta
