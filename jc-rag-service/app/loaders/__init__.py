"""文档加载器模块初始化"""
from .base_loader import DocumentLoader, DocumentLoaderFactory
from .txt_loader import TxtLoader
from .pdf_loader import PdfLoader
from .docx_loader import DocxLoader
from .xlsx_loader import XlsxLoader
from .pptx_loader import PptxLoader

# 注册所有加载器
DocumentLoaderFactory.register_loader(TxtLoader())
DocumentLoaderFactory.register_loader(PdfLoader())
DocumentLoaderFactory.register_loader(DocxLoader())
DocumentLoaderFactory.register_loader(XlsxLoader())
DocumentLoaderFactory.register_loader(PptxLoader())

__all__ = [
    "DocumentLoader",
    "DocumentLoaderFactory",
    "TxtLoader",
    "PdfLoader",
    "DocxLoader",
    "XlsxLoader",
    "PptxLoader",
]
