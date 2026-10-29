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


"""Agent 模块初始化"""
from .tool_registry import Tool, ToolRegistry, tool
from .agent_base import Agent, ToolUsingAgent, AgentFactory, format_tool_call, parse_tool_call
from .rag_agent import RagAgent

__all__ = [
    "Tool",
    "ToolRegistry",
    "tool",
    "Agent",
    "ToolUsingAgent",
    "AgentFactory",
    "format_tool_call",
    "parse_tool_call",
    "RagAgent",
]
