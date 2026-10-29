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


"""Agent 基类"""
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, Tuple

from .tool_registry import ToolRegistry


class Agent(ABC):
    """Agent 抽象基类"""
    
    def __init__(self, name: str, description: str = ""):
        self.name = name
        self.description = description
        self.session_memory = []
    
    @abstractmethod
    def run(self, prompt: str, **kwargs) -> str:
        """执行 Agent"""
        pass
    
    def add_memory(self, item: Dict[str, Any]) -> None:
        """添加会话记忆"""
        self.session_memory.append(item)
    
    def get_memory(self, limit: Optional[int] = None) -> List[Dict[str, Any]]:
        """获取会话记忆"""
        if limit:
            return self.session_memory[-limit:]
        return self.session_memory
    
    def clear_memory(self) -> None:
        """清空会话记忆"""
        self.session_memory = []


class ToolUsingAgent(Agent):
    """支持工具调用的 Agent"""
    
    def __init__(self, name: str, description: str = "", tool_names: Optional[List[str]] = None):
        super().__init__(name, description)
        self.tool_names = tool_names or []
    
    def get_available_tools(self) -> List[str]:
        """获取可用工具列表"""
        return self.tool_names
    
    def call_tool(self, tool_name: str, **kwargs) -> Any:
        """调用工具"""
        if tool_name not in self.tool_names:
            raise ValueError(f"Agent {self.name} 不支持工具 {tool_name}")
        return ToolRegistry.call_tool(tool_name, **kwargs)


class AgentFactory:
    """Agent 工厂"""
    
    _agents: Dict[str, Agent] = {}
    
    @classmethod
    def register_agent(cls, agent: Agent) -> None:
        """注册 Agent"""
        cls._agents[agent.name] = agent
    
    @classmethod
    def get_agent(cls, name: str) -> Optional[Agent]:
        """获取 Agent"""
        return cls._agents.get(name)
    
    @classmethod
    def list_agents(cls) -> List[Agent]:
        """获取所有 Agent 列表"""
        return list(cls._agents.values())


# 工具调用提示词模板
TOOL_CALL_TEMPLATE = """
你是一个智能助手，需要根据用户的问题决定是否调用工具。

可用工具：
{tools}

当你决定调用工具时，请使用以下格式输出：
<function name="{tool_name}">{parameters}</function>

如果你不需要调用工具，可以直接回答用户问题。

用户问题：{prompt}
"""


def format_tool_call(tool_name: str, **kwargs) -> str:
    """格式化工具调用"""
    params = "\n".join([f"{k}: {v}" for k, v in kwargs.items()])
    return f"<function name=\"{tool_name}\">{params}</function>"


def parse_tool_call(response: str) -> Optional[Tuple[str, Dict[str, str]]]:
    """解析工具调用"""
    import re
    
    match = re.search(r'<function name="([^"]+)">([^<]+)</function>', response, re.DOTALL)
    if not match:
        return None
    
    tool_name = match.group(1).strip()
    params_str = match.group(2).strip()
    
    params = {}
    for line in params_str.split('\n'):
        if ':' in line:
            key, value = line.split(':', 1)
            params[key.strip()] = value.strip()
    
    return tool_name, params
