"""PDF 文件加载器"""
from __future__ import annotations

from typing import Any

from .base_loader import DocumentLoader


class PdfLoader(DocumentLoader):
    """PDF 文件加载器"""
    
    def can_load(self, file_path: str) -> bool:
        """判断是否为 PDF 文件"""
        return file_path.lower().endswith('.pdf')
    
    def load(self, file_path: str) -> str:
        """加载 PDF 文件内容"""
        return self.load_with_meta(file_path)[0]
    
    def load_with_meta(self, file_path: str) -> tuple[str, dict[str, Any]]:
        """加载 PDF 文件内容及元数据"""
        try:
            import fitz
        except ImportError as ex:
            raise ValueError(
                "缺少 PDF 解析依赖 PyMuPDF，请先执行 pip install pymupdf"
            ) from ex
        
        doc = fitz.open(file_path)
        pages: list[str] = []
        page_count = doc.page_count
        
        for page_idx in range(page_count):
            page = doc.load_page(page_idx)
            text = page.get_text()
            
            # 如果文本内容很少，尝试 OCR
            if text.strip():
                pages.append(text)
        
        doc.close()
        
        content = '\n\n'.join(pages)
        meta = {
            'file_type': 'pdf',
            'page_count': page_count,
            'char_count': len(content),
        }
        
        return content, meta
