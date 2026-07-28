"""知识库路由: 文档解析 / ES 索引 / 删除.
<p>原位置: app/api/routes.py (拆分自 2026-07-03 结构重构, 业务零变更).

@author JC
"""
from __future__ import annotations

import logging
import os
import queue
import threading
import time
import traceback
from typing import Any

from fastapi import APIRouter

from app.api.schemas import KnowledgeDeleteRequest, KnowledgeParseRequest
from app.loaders import DocumentLoaderFactory
from app.utils import download_object_to_temp_file, split_text
from app.vector_store import get_vector_store
from app.vector_store.filter_expr import FilterExpr

logger = logging.getLogger(__name__)
router = APIRouter()
_parse_gate = threading.BoundedSemaphore(1)


class CancelParseException(Exception):
    """用户强制终止解析任务的内部信号"""
    pass


@router.post("/knowledge/parse")
def knowledge_parse(request: KnowledgeParseRequest) -> dict:
    """知识库文档解析并建立索引（流式 pipeline + 多阶段进度追踪）。
    使用 def（非 async），FastAPI 在线程池中执行，不阻塞事件循环，确保取消请求能被及时处理。"""
    from app.api.deps import _resolve_cpu_info
    from app.core.config import get_settings
    from app.core.parse_progress import (
        check_cancelled,
        complete_progress,
        get_stage_ids,
        init_progress,
        update_stage,
    )
    from app.embeddings import _resolve_device, embed_texts
    from app.vector_store import get_vector_store as _get_vs

    _parse_gate.acquire()
    settings = get_settings()
    start_time = time.time()
    doc_id = int(request.documentId) if request.documentId else 0
    file_ext = (request.fileExt or "txt").strip().lower()
    kb_id = str(request.kbId)
    # [DIAG] 排查 kbId 传递: 打印 Java 端实际下发的 kbId / documentId
    logger.warning(
        "[DIAG_PARSE] doc_id=%s kb_id=%s fileName=%s request_kbId=%r request_docId=%r",
        doc_id, kb_id, request.fileName, request.kbId, request.documentId,
    )

    stage_ids = get_stage_ids(file_ext)
    init_progress(doc_id, request.fileName or "", file_ext, stage_ids)

    def _check_cancel():
        if check_cancelled(doc_id):
            raise CancelParseException("用户终止")

    try:
        used_device = _resolve_device(getattr(settings.embedding, 'device', 'auto') or 'auto')
        if used_device == "cpu":
            device_info = _resolve_cpu_info()
        elif used_device == "dml":
            try:
                import torch_directml
                device_info = f"DirectML ({torch_directml.device_name(0)})"
            except Exception:
                device_info = "DirectML"
        elif used_device == "cuda":
            try:
                import torch
                device_info = f"CUDA ({torch.cuda.get_device_name(0)})"
            except Exception:
                device_info = "CUDA"
        else:
            device_info = used_device

        model_path = getattr(settings.embedding, 'model_path', '') or "未配置"
        update_stage(doc_id, "download", "running", 5,
                     f"自动选择计算设备: {device_info}，Embedding 模型: {model_path}",
                     "")
        _check_cancel()

        embedding_override = None
        if request.ragOverride and request.ragOverride.embedding:
            embedding_override = request.ragOverride.embedding.model_dump(exclude_none=True)
            if embedding_override and embedding_override.get("device") and embedding_override.get("device") != "auto":
                used_device = _resolve_device(str(embedding_override["device"]))
                device_info = f"手动指定: {used_device}"

        update_stage(doc_id, "download", "running", 10, "正在初始化向量索引...", "")
        _check_cancel()
        vs = _get_vs()
        from app.embeddings import get_embedding_dimension
        dims = get_embedding_dimension(settings, embedding_override)
        vs.ensure_collection(vs.default_collection, dims)
        update_stage(doc_id, "download", "running", 25,
                     f"正在从 MinIO 下载文档: {request.fileName}...", "")
        _check_cancel()
        temp_path = download_object_to_temp_file(
            request.minioBucket, request.minioObjectName, request.fileExt or "bin"
        )
        download_size = os.path.getsize(temp_path) if os.path.exists(temp_path) else 0
        update_stage(doc_id, "download", "completed", 100,
                     f"文档下载完成（{download_size / 1024:.1f} KB），ES 索引已就绪", "")

        update_stage(doc_id, "extract", "running", 10,
                     f"正在解析 {file_ext.upper()} 文档...",
                     f"计算设备: {device_info}")
        _check_cancel()
        _temp_file = temp_path
        file_size = os.path.getsize(_temp_file) if _temp_file and os.path.exists(_temp_file) else 0
        extract_result = {"text": None, "meta": None}
        extract_done = threading.Event()
        extract_error = [None]

        def _extract_worker():
            try:
                text_val, meta_val = DocumentLoaderFactory.load_with_meta(_temp_file)
                extract_result["text"] = text_val
                extract_result["meta"] = meta_val
            except Exception as e:
                extract_error[0] = e
            finally:
                extract_done.set()

        ext_thread = threading.Thread(target=_extract_worker, daemon=True)
        ext_thread.start()
        _check_cancel()
        elapsed = 0
        while not extract_done.wait(1.0):
            elapsed += 1
            _check_cancel()
            kb_per_s = (file_size / 1024) / elapsed if elapsed > 0 else 0
            update_stage(doc_id, "extract", "running", min(35 + elapsed * 2, 85),
                         f"正在提取文本内容... 已处理 {elapsed} 秒（{kb_per_s:.0f} KB/s）",
                         f"计算设备: {device_info}")

        if extract_error[0]:
            raise extract_error[0]

        text = extract_result["text"]
        meta = extract_result["meta"]
        text_len = len(text) if text else 0
        page_count = meta.get("page_count", 0) if meta else 0
        update_stage(doc_id, "extract", "completed", 100,
                     f"文本提取完成，共 {text_len} 字符，{page_count} 页",
                     f"计算设备: {device_info}")

        if not text or (isinstance(text, str) and not text.strip()):
            if _temp_file and os.path.exists(_temp_file):
                os.unlink(_temp_file)
            complete_progress(doc_id, True, "文档内容为空，无需索引")
            return {"chunkCount": 0, "pageCount": meta.get("page_count", 0) if meta else 0, "message": "文档内容为空，无需索引"}

        has_ocr = "ocr" in stage_ids
        precomputed_chunks = None
        if has_ocr:
            update_stage(doc_id, "ocr", "running", 5, "正在 OCR 识别扫描内容...",
                         f"计算设备: {device_info}")
            _check_cancel()
            from app.utils.ocr_utils import get_ocr_execution_device, ocr_pdf_page_text
            page_count = meta.get("page_count", 1) if meta else 1
            ocr_device = get_ocr_execution_device()
            update_stage(doc_id, "ocr", "running", 5,
                         f"OCR 已选择实际计算设备: {ocr_device}",
                         f"OCR 实际设备: {ocr_device}")
            ocr_lines = []
            streamed_chunks = []
            stream_batch_start = 0
            stream_queue: queue.Queue[tuple[list[str], int] | None] = queue.Queue(maxsize=1)
            stream_result = {"success": 0, "failed": 0, "chunks": 0, "error": None, "error_detail": ""}
            stream_lock = threading.Lock()
            batch_size = min(8, getattr(settings.embedding, 'batch_size', 32) or 32)
            now_iso = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
            embedding_mode = "在线模型服务" if used_device == "lm_studio" else "本地 Embedding 模型"
            embedding_from_override = bool((embedding_override or {}).get("modelPath"))
            embedding_model = (embedding_override or {}).get("modelPath") or getattr(settings.embedding, "model_path", "未配置")
            embedding_source = "管理端运行时配置" if embedding_from_override else "Python 服务配置文件"

            def _consume_ocr_chunks():
                pending_docs: list[dict] = []
                sequence = 0
                try:
                    while True:
                        item = stream_queue.get()
                        if item is None:
                            break
                        batch_chunks, source_pct = item
                        for offset in range(0, len(batch_chunks), batch_size):
                            chunk_batch = batch_chunks[offset:offset + batch_size]
                            vecs, embedding_meta = embed_texts(chunk_batch, settings, embedding_override)
                            for chunk_text, vec in zip(chunk_batch, vecs):
                                pending_docs.append({"chunk_id": f"{kb_id}_{doc_id}_{sequence}", "document_id": str(doc_id), "kb_id": kb_id, "file_name": request.fileName, "doc_type": request.docType or "GENERAL", "security_level": request.securityLevel or "INTERNAL", "page_no": sequence + 1, "chunk_text": chunk_text, "chunk_text_keyword": chunk_text, "chunk_text_ws": chunk_text, "embedding_vector": vec, "version_no": request.versionNo or 1, "created_at": now_iso})
                                sequence += 1
                            with stream_lock:
                                stream_result["chunks"] += len(chunk_batch)
                                done = stream_result["chunks"]
                            actual_device = str(embedding_meta.get("device") or device_info)
                            update_stage(doc_id, "embedding", "running", source_pct, f"向量化进行中：{embedding_mode}；模型: {embedding_model}；来源: {embedding_source}；实际设备: {actual_device}；已完成 {done} 块", f"模型: {embedding_model}; 来源: {embedding_source}; 实际设备: {actual_device}")
                            if pending_docs:
                                s, f = vs.bulk_index(pending_docs)
                                with stream_lock:
                                    stream_result["success"] += s; stream_result["failed"] += f
                                update_stage(doc_id, "es_index", "running", source_pct, f"向量索引写入中：本批成功 {s} 条，失败 {f} 条", "与向量化并行写入")
                                pending_docs = []
                    if pending_docs:
                        s, f = vs.bulk_index(pending_docs)
                        with stream_lock:
                            stream_result["success"] += s; stream_result["failed"] += f
                except Exception as ex:
                    stream_result["error"] = ex
                    stream_result["error_detail"] = f"{type(ex).__name__}: {str(ex) or repr(ex)}"
                    logger.exception("OCR streaming embedding/index worker failed: doc=%s", doc_id)

            stream_worker = threading.Thread(target=_consume_ocr_chunks, daemon=True, name=f"ocr-index-{doc_id}")
            stream_worker.start()

            def _enqueue_stream_item(item: tuple[list[str], int] | None):
                while True:
                    if stream_result["error"]:
                        raise RuntimeError(f"流水线消费者失败: {stream_result['error_detail']}")
                    try:
                        stream_queue.put(item, timeout=0.5)
                        return
                    except queue.Full:
                        _check_cancel()
            for pn in range(page_count):
                _check_cancel()
                pg_text = ocr_pdf_page_text(
                    _temp_file, pn,
                    render_scale=min(1.5, float(settings.ocr.render_scale)),
                    min_score=float(settings.ocr.text_score),
                ) if _temp_file else ""
                ocr_device = get_ocr_execution_device()
                if pg_text:
                    ocr_lines.append(pg_text)
                pg_pct = min(99, int((pn + 1) / page_count * 100))
                if pn % max(1, page_count // 5) == 0 or (pn + 1) % 16 == 0 or pn == page_count - 1:
                    update_stage(doc_id, "ocr", "running", pg_pct,
                                 f"OCR 扫描第 {pn + 1}/{page_count} 页，当前页文本 {len(pg_text)} 字符，累计 {sum(len(x) for x in ocr_lines)} 字符，设备: {ocr_device}",
                                 f"OCR 实际设备: {ocr_device}")
                # Make the pipeline hand-off visible before OCR completes.
                # The final merge still preserves document-level chunk context.
                if (pn + 1) % 16 == 0 and pn + 1 < page_count:
                    # This is a real hand-off: page batches are chunked while
                    # OCR continues scanning the following pages.
                    batch_text = "\n".join(ocr_lines[stream_batch_start:])
                    batch_chunks = split_text(batch_text, chunk_size=request.chunkSize, chunk_overlap=request.chunkOverlap)
                    streamed_chunks.extend(batch_chunks)
                    stream_batch_start = len(ocr_lines)
                    chunk_pct = min(99, int((pn + 1) / page_count * 100))
                    _enqueue_stream_item((batch_chunks, chunk_pct))
                    update_stage(doc_id, "chunk", "running", chunk_pct,
                                 f"已完成 OCR 第 1-{pn + 1} 页并交给分块器，当前已生成 {len(streamed_chunks)} 个块", "OCR 页批实时交接")
            ocr_text = "\n".join(ocr_lines)
            if stream_batch_start < len(ocr_lines):
                batch_chunks = split_text("\n".join(ocr_lines[stream_batch_start:]), chunk_size=request.chunkSize, chunk_overlap=request.chunkOverlap)
                streamed_chunks.extend(batch_chunks)
                _enqueue_stream_item((batch_chunks, 99))
            precomputed_chunks = streamed_chunks
            if ocr_text.strip():
                text = ocr_text
            update_stage(doc_id, "ocr", "completed", 100, f"OCR 识别完成，共 {len(ocr_text)} 字符", f"OCR 实际设备: {ocr_device}")
            _enqueue_stream_item(None)
            stream_worker.join()
            if stream_result["error"]:
                raise RuntimeError(f"流水线处理失败: {stream_result['error_detail']}") from stream_result["error"]
            update_stage(doc_id, "chunk", "completed", 100, f"文档分块完成，共 {len(streamed_chunks)} 个块（OCR 页批实时产出）", "OCR -> 分块流水线")
            update_stage(doc_id, "embedding", "completed", 100, f"向量化完成：{embedding_mode}；模型: {embedding_model}；来源: {embedding_source}；设备: {device_info}；共 {stream_result['chunks']} 个向量", f"模型: {embedding_model}; 来源: {embedding_source}; 设备: {device_info}")
            update_stage(doc_id, "es_index", "completed", 100, f"向量索引完成：成功 {stream_result['success']} 条，失败 {stream_result['failed']} 条", "向量化并行写入")
            if _temp_file and os.path.exists(_temp_file):
                os.unlink(_temp_file)
            complete_progress(doc_id, True, f"文档解析完成，共 {stream_result['success']} 个块已写入向量库（OCR、分块、向量化、索引流水执行）")
            return {"chunkCount": stream_result["success"], "pageCount": page_count, "message": "文档解析完成"}
            update_stage(doc_id, "ocr", "completed", 100,
                         f"OCR 识别完成，共 {len(ocr_text)} 字符" if ocr_text.strip() else "OCR 未识别到文本",
                         f"计算设备: {device_info}")

        if _temp_file and os.path.exists(_temp_file):
            os.unlink(_temp_file)

        update_stage(doc_id, "chunk", "running", 10,
                     f"正在切分文本（块大小: {request.chunkSize or settings.chunk.size}，重叠: {request.chunkOverlap or settings.chunk.overlap}），总字符数: {text_len}...", "")
        _check_cancel()
        chunks = precomputed_chunks if precomputed_chunks is not None else split_text(text, chunk_size=request.chunkSize, chunk_overlap=request.chunkOverlap)
        total_chunks = len(chunks)
        avg_chunk_len = text_len // total_chunks if total_chunks > 0 else 0
        update_stage(doc_id, "chunk", "completed", 100,
                     f"文本切块完成，共 {total_chunks} 个块（平均每块 {avg_chunk_len} 字符）", "")

        if not chunks:
            complete_progress(doc_id, True, "文档切块后为空，无需索引")
            return {"chunkCount": 0, "pageCount": meta.get("page_count", 0) if meta else 0, "message": "文档切块后为空，无需索引"}

        batch_size = getattr(settings.embedding, 'batch_size', 32) or 32
        chunks_list = list(chunks)
        now_iso = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        total_emb_batches = max(1, (total_chunks + batch_size - 1) // batch_size)

        update_stage(doc_id, "embedding", "running", 0,
                     f"正在生成向量嵌入，共 {total_chunks} 个块，每批 {batch_size} 个",
                     f"计算设备: {device_info}")
        update_stage(doc_id, "es_index", "running", 0,
                     "等待向量数据就绪后写入 ES 索引...", "")
        _check_cancel()

        es_batch = 200
        pending_docs: list[dict] = []
        success_total = 0
        failed_total = 0
        emb_done = 0

        for batch_idx in range(0, total_chunks, batch_size):
            _check_cancel()
            batch_end = min(batch_idx + batch_size, total_chunks)
            batch = chunks_list[batch_idx:batch_end]
            vecs, _ = embed_texts(batch, settings, embedding_override)
            emb_done += len(batch)

            for i, (chunk_text, vec) in enumerate(zip(batch, vecs)):
                pending_docs.append({
                    "chunk_id": f"{kb_id}_{doc_id}_{batch_idx + i}",
                    "document_id": str(doc_id),
                    "kb_id": kb_id,
                    "file_name": request.fileName,
                    "doc_type": request.docType or "GENERAL",
                    "security_level": request.securityLevel or "INTERNAL",
                    "page_no": meta.get("page_no") if meta else batch_idx + i + 1,
                    "chunk_text": chunk_text,
                    "chunk_text_keyword": chunk_text,
                    "chunk_text_ws": chunk_text,
                    "embedding_vector": vec,
                    "version_no": request.versionNo or 1,
                    "created_at": now_iso,
                })

            emb_pct = min(99, int(emb_done / total_chunks * 100))
            update_stage(doc_id, "embedding", "running", emb_pct,
                         f"向量嵌入中... 已完成 {emb_done}/{total_chunks} 个块",
                         f"计算设备: {device_info}")

            if len(pending_docs) >= es_batch or emb_done >= total_chunks:
                _check_cancel()
                s, f = vs.bulk_index(pending_docs)
                success_total += s
                failed_total += f
                idx_pct = min(99, int(success_total / total_chunks * 100))
                update_stage(doc_id, "es_index", "running", idx_pct,
                             f"写入向量索引中... 已写入 {success_total}/{total_chunks} 条",
                             "")
                pending_docs = []

        if pending_docs:
            _check_cancel()
            s, f = vs.bulk_index(pending_docs)
            success_total += s
            failed_total += f

        update_stage(doc_id, "embedding", "completed", 100,
                     f"向量嵌入完成，共 {emb_done} 个向量",
                     f"计算设备: {device_info}")

        cost = round(time.time() - start_time, 2)
        page_count = meta.get("page_count", total_chunks) if meta else total_chunks

        if failed_total > 0:
            update_stage(doc_id, "es_index", "completed", 90,
                         f"ES 索引写入完成，成功: {success_total}，失败: {failed_total}", "")
        else:
            update_stage(doc_id, "es_index", "completed", 100,
                         f"ES 索引写入完成，共 {success_total} 条记录", "")

        logger.info("knowledge_parse done: doc=%s kb=%s chunks=%d success=%d failed=%d cost=%ss",
                     doc_id, kb_id, total_chunks, success_total, failed_total, cost)

        complete_progress(doc_id, True, f"文档解析完成，共 {success_total} 个块已写入向量库（耗时 {cost} 秒）")
        return {"chunkCount": success_total, "pageCount": page_count,
                "message": f"索引完成，{success_total}个块已写入向量库（耗时{cost}秒）",}

    except CancelParseException:
        logger.warning("knowledge_parse cancelled: doc=%s, rolling back vector index", doc_id)
        try:
            vs.delete_by_document_id(str(doc_id))
            logger.info("Vector index rollback for cancelled doc=%s OK", doc_id)
        except Exception as rollback_err:
            logger.error("Vector index rollback failed for doc=%s: %s", doc_id, rollback_err)
        complete_progress(doc_id, False, "任务已被用户终止，向量索引已回滚")
        return {"chunkCount": 0, "pageCount": 0, "message": {"phase": "cancelled", "msg": "任务已被终止，向量数据已清理"}}
    except Exception as ex:
        error_detail = f"{type(ex).__name__}: {str(ex) or repr(ex)}"
        logger.error("knowledge_parse failed: doc=%s err=%s\n%s", doc_id, error_detail, traceback.format_exc())
        complete_progress(doc_id, False, f"解析失败: {error_detail}")
        return {"chunkCount": 0, "pageCount": 0, "message": {"phase": "error", "msg": f"解析失败: {error_detail}"}}
    finally:
        _parse_gate.release()


@router.post("/knowledge/delete")
async def knowledge_delete(request: KnowledgeDeleteRequest) -> dict:
    """清理知识库/文档的向量索引数据"""
    try:
        vs = get_vector_store()
        if request.documentId:
            vs.delete_by_document_id(str(request.documentId))
        elif request.kbId:
            deleted = vs.delete_by_filter(FilterExpr.eq("kb_id", str(request.kbId)))
            logger.info("kb_id=%s 清理向量索引 %d 条", request.kbId, deleted)
        return {"status": "SUCCESS", "message": "索引数据已清理"}
    except Exception as ex:
        logger.error("knowledge_delete failed: %s", str(ex))
        return {"status": "FAILED", "message": str(ex)}
