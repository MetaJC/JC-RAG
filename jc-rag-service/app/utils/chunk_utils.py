"""文本切块工具模块"""
from __future__ import annotations


def split_text(text: str, chunk_size: int = 700, chunk_overlap: int = 120) -> list[str]:
    """将文本切分成指定大小的块"""
    chunks: list[str] = []
    start = 0
    text = text.strip()
    
    if not text:
        return chunks
    
    while start < len(text):
        end = min(start + chunk_size, len(text))
        
        # 在中文语境下，尽量在句子边界处分割
        if end < len(text):
            # 查找最后一个中文句号、问号、感叹号
            for char in ['。', '！', '？', '\n', ';', '；']:
                pos = text.rfind(char, start + chunk_overlap, end)
                if pos > start + chunk_overlap:
                    end = pos + 1
                    break
        
        chunks.append(text[start:end].strip())
        
        if end >= len(text):
            break
        
        start = max(end - chunk_overlap, start + 1)
    
    return [chunk for chunk in chunks if chunk]


def merge_overlapping_chunks(chunks: list[str], max_overlap: int = 100) -> list[str]:
    """合并重叠的块"""
    if len(chunks) < 2:
        return chunks
    
    merged: list[str] = [chunks[0]]
    
    for chunk in chunks[1:]:
        last = merged[-1]
        
        # 计算重叠长度
        overlap_len = 0
        max_check = min(len(last), len(chunk), max_overlap)
        
        for i in range(max_check, 0, -1):
            if last.endswith(chunk[:i]):
                overlap_len = i
                break
        
        if overlap_len > 0:
            merged[-1] = last + chunk[overlap_len:]
        else:
            merged.append(chunk)
    
    return merged


def count_tokens(text: str) -> int:
    """估算文本的 token 数量（简单估算）"""
    # 中文字符按 1 token = 1 字符估算
    # 英文字符按 1 token = 4 字符估算
    chinese_count = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
    english_count = len(text) - chinese_count
    
    return chinese_count + (english_count // 4)
