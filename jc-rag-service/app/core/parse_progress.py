"""文档解析进度管理 —— 持久化版（SQLite）"""

import json
import os
import sqlite3
import threading
from datetime import datetime
from typing import Any, Optional

_db_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "data")
os.makedirs(_db_dir, exist_ok=True)
_db_path = os.path.join(_db_dir, "parse_progress.db")

_lock = threading.Lock()
_cancel_flags: dict[int, bool] = {}

_STAGE_NAMES: dict[str, str] = {
    "download": "文档下载",
    "extract": "文档提取",
    "ocr": "OCR识别",
    "chunk": "文档分块",
    "embedding": "向量化",
    "es_index": "向量索引",
}


def _get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(_db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    return conn


def _init_db():
    conn = _get_conn()
    conn.execute("""
        CREATE TABLE IF NOT EXISTS parse_progress (
            document_id INTEGER PRIMARY KEY,
            file_name TEXT NOT NULL,
            file_ext TEXT NOT NULL DEFAULT '',
            parse_status TEXT NOT NULL DEFAULT 'PROCESSING',
            overall_progress INTEGER NOT NULL DEFAULT 0,
            current_stage TEXT NOT NULL DEFAULT '',
            stages_json TEXT NOT NULL DEFAULT '[]',
            created_at TEXT NOT NULL DEFAULT '',
            updated_at TEXT NOT NULL DEFAULT ''
        )
    """)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS parse_logs (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            document_id INTEGER NOT NULL,
            timestamp TEXT NOT NULL,
            stage_id TEXT NOT NULL DEFAULT '',
            level TEXT NOT NULL DEFAULT 'info',
            message TEXT NOT NULL DEFAULT '',
            FOREIGN KEY (document_id) REFERENCES parse_progress(document_id)
        )
    """)
    conn.execute("CREATE INDEX IF NOT EXISTS idx_pl_doc ON parse_logs(document_id)")
    conn.commit()
    conn.close()


def _stage_name(stage_id: str) -> str:
    return _STAGE_NAMES.get(stage_id, stage_id)


def get_stage_ids(file_ext: str) -> list[str]:
    ext = file_ext.lower().lstrip('.')
    if ext in ("txt", "csv", "json", "xml", "md"):
        return ["download", "extract", "chunk", "embedding", "es_index"]
    if ext == "pdf":
        return ["download", "extract", "ocr", "chunk", "embedding", "es_index"]
    if ext in ("doc", "docx", "ppt", "pptx", "xls", "xlsx"):
        return ["download", "extract", "chunk", "embedding", "es_index"]
    if ext in ("png", "jpg", "jpeg", "bmp", "gif", "tiff", "tif"):
        return ["download", "ocr", "chunk", "embedding", "es_index"]
    return ["download", "extract", "ocr", "chunk", "embedding", "es_index"]


def init_progress(document_id: int, file_name: str, file_ext: str, stage_ids: Optional[list[str]] = None) -> dict[str, Any]:
    with _lock:
        _cancel_flags.pop(document_id, None)
        if stage_ids is None:
            stage_ids = get_stage_ids(file_ext)
        stages = []
        for i, sid in enumerate(stage_ids):
            stages.append({
                "stageId": sid,
                "stageName": _stage_name(sid),
                "order": i,
                "status": "pending",
                "progress": 0,
                "startedAt": None,
                "completedAt": None,
            })
        now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        first_stage = stage_ids[0] if stage_ids else ""
        entry = {
            "documentId": document_id,
            "fileName": file_name,
            "fileExt": file_ext,
            "parseStatus": "PROCESSING",
            "overallProgress": 0,
            "currentStage": first_stage,
            "stages": stages,
            "createdAt": now_str,
            "updatedAt": now_str,
        }
        _write_entry(entry)
        _clear_logs_unlocked(document_id)
        _append_log_unlocked(document_id, first_stage, "info", f"开始解析文档: {file_name}")
        return {**entry, "logs": _read_logs(document_id)}


def _clear_logs_unlocked(document_id: int):
    conn = _get_conn()
    try:
        conn.execute("DELETE FROM parse_logs WHERE document_id=?", (document_id,))
        conn.commit()
    finally:
        conn.close()


def _append_log_unlocked(document_id: int, stage_id: str, level: str, message: str):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    conn = _get_conn()
    try:
        conn.execute(
            "INSERT INTO parse_logs (document_id, timestamp, stage_id, level, message) VALUES (?,?,?,?,?)",
            (document_id, timestamp, stage_id, level, message),
        )
        conn.commit()
    finally:
        conn.close()


def append_log(document_id: int, stage_id: str, level: str, message: str):
    with _lock:
        _append_log_unlocked(document_id, stage_id, level, message)


def update_progress(document_id: int, stage_id: str, progress: float, overall_progress: float, stage_status: str = "running"):
    with _lock:
        entry = _read_entry(document_id)
        if not entry:
            return
        entry["currentStage"] = stage_id
        # Overlapping pipeline updates may arrive out of order. Keep display
        # progress monotonic so neither the bar nor a stage moves backwards.
        entry["overallProgress"] = max(int(entry.get("overallProgress", 0)), min(int(overall_progress), 100))
        now_iso = datetime.now().astimezone().isoformat()
        for s in entry.get("stages", []):
            if s["stageId"] == stage_id:
                s["progress"] = max(int(s.get("progress", 0)), min(int(progress), 100))
                if stage_status == "completed":
                    s["status"] = "completed"
                    s["progress"] = 100
                    s["completedAt"] = now_iso
                elif stage_status == "failed":
                    s["status"] = "failed"
                    s["completedAt"] = now_iso
                else:
                    s["status"] = "running"
                    if not s.get("startedAt"):
                        s["startedAt"] = now_iso
                break
        entry["updatedAt"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        _write_entry(entry)


def _update_stage_detail(document_id: int, stage_id: str, detail: str):
    with _lock:
        entry = _read_entry(document_id)
        if not entry:
            return
        for s in entry.get("stages", []):
            if s["stageId"] == stage_id:
                s["deviceInfo"] = detail
                break
        entry["updatedAt"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        _write_entry(entry)


def set_status(document_id: int, status: str):
    with _lock:
        entry = _read_entry(document_id)
        if not entry:
            conn = _get_conn()
            try:
                conn.execute(
                    "UPDATE parse_progress SET parse_status=?, updated_at=? WHERE document_id=?",
                    (status, datetime.now().strftime("%Y-%m-%d %H:%M:%S"), document_id),
                )
                conn.commit()
            finally:
                conn.close()
            return
        entry["parseStatus"] = status
        entry["updatedAt"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        _write_entry(entry)


def get_progress(document_id: int) -> Optional[dict[str, Any]]:
    entry = _read_entry(document_id)
    if not entry:
        return None
    logs = _read_logs(document_id)
    entry["logs"] = logs
    return entry


def _write_entry(entry: dict):
    conn = _get_conn()
    try:
        now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        stages_json = json.dumps(entry.get("stages", []), ensure_ascii=False)
        conn.execute("""
            INSERT OR REPLACE INTO parse_progress
                (document_id, file_name, file_ext, parse_status, overall_progress, current_stage, stages_json, created_at, updated_at)
            VALUES (?,?,?,?,?,?,?,?,?)
        """, (
            entry["documentId"],
            entry.get("fileName", ""),
            entry.get("fileExt", ""),
            entry.get("parseStatus", "PROCESSING"),
            entry.get("overallProgress", 0),
            entry.get("currentStage", ""),
            stages_json,
            entry.get("createdAt", now),
            entry.get("updatedAt", now),
        ))
        conn.commit()
    finally:
        conn.close()


def _read_entry(document_id: int) -> Optional[dict[str, Any]]:
    conn = _get_conn()
    try:
        row = conn.execute("SELECT * FROM parse_progress WHERE document_id=?", (document_id,)).fetchone()
        if not row:
            return None
        return {
            "documentId": row["document_id"],
            "fileName": row["file_name"],
            "fileExt": row["file_ext"],
            "parseStatus": row["parse_status"],
            "overallProgress": row["overall_progress"],
            "currentStage": row["current_stage"],
            "stages": json.loads(row["stages_json"]),
            "createdAt": row["created_at"],
            "updatedAt": row["updated_at"],
        }
    finally:
        conn.close()


def _read_logs(document_id: int) -> list[dict[str, Any]]:
    conn = _get_conn()
    try:
        rows = conn.execute(
            "SELECT timestamp, stage_id, level, message FROM parse_logs WHERE document_id=? ORDER BY id ASC",
            (document_id,),
        ).fetchall()
        return [
            {"timestamp": r["timestamp"], "stageId": r["stage_id"], "level": r["level"], "message": r["message"]}
            for r in rows
        ]
    finally:
        conn.close()


def get_progress_or_empty(document_id: int) -> dict[str, Any]:
    result = get_progress(document_id)
    if result is not None:
        return result
    return {
        "documentId": document_id,
        "fileName": "",
        "fileExt": "",
        "parseStatus": "IDLE",
        "overallProgress": 0,
        "currentStage": "",
        "stages": [],
        "logs": [],
    }


def cancel_task(document_id: int) -> bool:
    with _lock:
        _cancel_flags[document_id] = True
        entry = _read_entry(document_id)
        if entry and entry.get("parseStatus") == "PROCESSING":
            entry["parseStatus"] = "CANCELLED"
            for s in entry.get("stages", []):
                if s.get("status") in ("processing", "running"):
                    s["status"] = "failed"
            entry["updatedAt"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            _write_entry(entry)
            _append_log_unlocked(document_id, "", "error", "任务已被用户强制终止")
        return entry is not None and entry.get("parseStatus") in ("PROCESSING", "CANCELLED")


def check_cancelled(document_id: int) -> bool:
    return bool(_cancel_flags.get(document_id, False))


def raise_if_cancelled(document_id: int):
    if check_cancelled(document_id):
        raise RuntimeError("CANCELLED_BY_USER")


def update_stage(document_id: int, stage_id: str, status: str, progress: float, message: str, detail: str = ""):
    entry = _read_entry(document_id)
    if not entry:
        return
    stages = entry.get("stages", [])
    stage_ids = [s["stageId"] for s in stages]
    if stage_id not in stage_ids:
        return
    total = len(stages)
    idx = stage_ids.index(stage_id)
    prev_weight = idx / total * 100 if total > 0 else 0
    next_weight = (idx + 1) / total * 100 if total > 0 else 100
    stage_factor = (progress / 100.0) if progress > 0 else 0
    overall_pct = prev_weight + stage_factor * (next_weight - prev_weight)

    stage_status = "completed" if status == "completed" else ("failed" if status == "failed" else "running")
    update_progress(document_id, stage_id, progress, overall_pct, stage_status)
    if detail:
        _update_stage_detail(document_id, stage_id, detail)
    log_level = "error" if status == "failed" else "info"
    append_log(document_id, stage_id, log_level, message)


def complete_progress(document_id: int, success: bool, message: str):
    status = "SUCCESS" if success else "FAILED"
    set_status(document_id, status)
    log_level = "info" if success else "error"
    append_log(document_id, "", log_level, message)


def cleanup_entry(document_id: int):
    with _lock:
        _cancel_flags.pop(document_id, None)


_init_db()
