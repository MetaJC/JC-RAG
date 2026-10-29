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


"""Excel XLSX 文件加载器"""
from __future__ import annotations

from typing import Any

from .base_loader import DocumentLoader


class XlsxLoader(DocumentLoader):
    """XLSX 文件加载器"""
    
    def can_load(self, file_path: str) -> bool:
        """判断是否为 XLSX 文件"""
        return file_path.lower().endswith(('.xlsx', '.xls'))
    
    def load(self, file_path: str) -> str:
        """加载 XLSX 文件内容"""
        return self.load_with_meta(file_path)[0]
    
    def load_with_meta(self, file_path: str) -> tuple[str, dict[str, Any]]:
        """加载 XLSX 文件内容及元数据"""
        try:
            import xlrd
        except ImportError as ex:
            raise ValueError(
                "缺少 Excel 解析依赖 xlrd，请先执行 pip install xlrd"
            ) from ex
        
        workbook = xlrd.open_workbook(file_path)
        sheets = []
        
        for sheet_name in workbook.sheet_names():
            sheet = workbook.sheet_by_name(sheet_name)
            rows = []
            
            for row_idx in range(sheet.nrows):
                row = []
                for col_idx in range(sheet.ncols):
                    cell = sheet.cell_value(row_idx, col_idx)
                    row.append(str(cell))
                rows.append('\t'.join(row))
            
            if rows:
                sheets.append(f"=== {sheet_name} ===\n" + '\n'.join(rows))
        
        content = '\n\n'.join(sheets)
        meta = {
            'file_type': 'xlsx' if file_path.lower().endswith('.xlsx') else 'xls',
            'sheet_count': len(workbook.sheet_names()),
            'char_count': len(content),
        }
        
        return content, meta
