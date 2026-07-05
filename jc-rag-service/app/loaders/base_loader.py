"""文档加载器基类"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Iterator


class DocumentLoader(ABC):
    """文档加载器抽象基类"""
    
    @abstractmethod
    def can_load(self, file_path: str) -> bool:
        """判断是否支持加载该文件"""
        pass
    
    @abstractmethod
    def load(self, file_path: str) -> str:
        """加载文档内容"""
        pass
    
    @abstractmethod
    def load_with_meta(self, file_path: str) -> tuple[str, dict[str, Any]]:
        """加载文档内容及元数据"""
        pass


class DocumentLoaderFactory:
    """文档加载器工厂"""
    
    _loaders: list[DocumentLoader] = []
    
    @classmethod
    def register_loader(cls, loader: DocumentLoader) -> None:
        """注册加载器"""
        cls._loaders.append(loader)
    
    @classmethod
    def get_loader(cls, file_path: str) -> DocumentLoader | None:
        """获取支持该文件的加载器"""
        for loader in cls._loaders:
            if loader.can_load(file_path):
                return loader
        return None
    
    @classmethod
    def load(cls, file_path: str) -> str:
        """加载文档内容"""
        loader = cls.get_loader(file_path)
        if not loader:
            raise ValueError(f"不支持的文件类型: {file_path}")
        return loader.load(file_path)
    
    @classmethod
    def load_with_meta(cls, file_path: str) -> tuple[str, dict[str, Any]]:
        """加载文档内容及元数据"""
        loader = cls.get_loader(file_path)
        if not loader:
            raise ValueError(f"不支持的文件类型: {file_path}")
        return loader.load_with_meta(file_path)
