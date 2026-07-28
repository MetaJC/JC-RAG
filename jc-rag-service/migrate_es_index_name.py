"""一次性脚本：把 SQLite parse_progress.db 中所有 'ES索引' 替换为 '向量索引'"""
import sqlite3
import json
import os
import sys

DB = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "parse_progress.db")
if not os.path.exists(DB):
    print(f"[skip] {DB} not exists")
    sys.exit(0)

conn = sqlite3.connect(DB)
conn.row_factory = sqlite3.Row

stages_fixed = 0
logs_fixed = 0
docs = list(conn.execute("SELECT document_id, stages_json FROM parse_progress").fetchall())
for row in docs:
    doc_id = row["document_id"]
    raw = row["stages_json"]
    if not raw or "ES" not in raw:
        continue
    try:
        stages = json.loads(raw)
    except Exception:
        continue
    changed = False
    for s in stages:
        if isinstance(s, dict) and s.get("stageName") == "ES索引":
            s["stageName"] = "向量索引"
            changed = True
    if changed:
        new_json = json.dumps(stages, ensure_ascii=False)
        conn.execute(
            "UPDATE parse_progress SET stages_json=?, updated_at=? WHERE document_id=?",
            (new_json, __import__("datetime").datetime.now().strftime("%Y-%m-%d %H:%M:%S"), doc_id),
        )
        stages_fixed += 1
        print(f"  [stages] doc={doc_id} -> 向量索引")

log_rows = list(conn.execute("SELECT id, message FROM parse_logs WHERE message LIKE '%ES%'").fetchall())
for r in log_rows:
    if "ES索引" in r["message"]:
        new_msg = r["message"].replace("ES索引", "向量索引")
        conn.execute("UPDATE parse_logs SET message=? WHERE id=?", (new_msg, r["id"]))
        logs_fixed += 1
        print(f"  [logs] id={r['id']}")

conn.commit()
conn.close()
print(f"\n完成: stages 修复 {stages_fixed} 条, logs 修复 {logs_fixed} 条")
print("提示: 下次刷新前端即可看到新名称。")
