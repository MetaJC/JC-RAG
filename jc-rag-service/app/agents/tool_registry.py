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


"""工具注册中心"""
from __future__ import annotations

from typing import Any, Callable, Dict, List, Optional, TypeVar

T = TypeVar('T')


class Tool:
    """工具定义"""
    
    def __init__(
        self,
        name: str,
        description: str,
        func: Callable[..., Any],
        parameters: Optional[List[Dict[str, Any]]] = None,
        return_type: str = "string"
    ):
        self.name = name
        self.description = description
        self.func = func
        self.parameters = parameters or []
        self.return_type = return_type
    
    def call(self, **kwargs) -> Any:
        """调用工具"""
        return self.func(**kwargs)
    
    def to_json(self) -> Dict[str, Any]:
        """转换为 JSON 格式（用于 LLM 调用）"""
        return {
            "name": self.name,
            "description": self.description,
            "parameters": self.parameters,
            "return_type": self.return_type,
        }


class ToolRegistry:
    """工具注册表"""
    
    _tools: Dict[str, Tool] = {}
    
    @classmethod
    def register(cls, tool: Tool) -> None:
        """注册工具"""
        cls._tools[tool.name] = tool
    
    @classmethod
    def get_tool(cls, name: str) -> Optional[Tool]:
        """获取工具"""
        return cls._tools.get(name)
    
    @classmethod
    def list_tools(cls) -> List[Tool]:
        """获取所有工具列表"""
        return list(cls._tools.values())
    
    @classmethod
    def call_tool(cls, name: str, **kwargs) -> Any:
        """调用工具"""
        tool = cls.get_tool(name)
        if not tool:
            raise ValueError(f"工具 {name} 未注册")
        return tool.call(**kwargs)


# 装饰器：注册工具
def tool(name: str, description: str, parameters: Optional[List[Dict[str, Any]]] = None, return_type: str = "string"):
    def decorator(func: Callable[..., Any]) -> Callable[..., Any]:
        ToolRegistry.register(Tool(name, description, func, parameters, return_type))
        return func
    return decorator
