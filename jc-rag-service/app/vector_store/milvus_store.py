"""
Milvus vector store implementation.

Business code uses the VectorStore abstraction; this module translates calls to
the Milvus SDK.
"""
from __future__ import annotations

import logging
from typing import Any

from pymilvus import (
    Collection,
    CollectionSchema,
    DataType,
    FieldSchema,
    connections,
    utility,
)

from .base import VectorStore
from .filter_expr import FilterExpr

logger = logging.getLogger(__name__)


CHUNK_FIELDS: list[FieldSchema] = [
    FieldSchema(name="chunk_id", dtype=DataType.VARCHAR, max_length=64, is_primary=True),
    FieldSchema(name="document_id", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="kb_id", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="file_name", dtype=DataType.VARCHAR, max_length=512),
    FieldSchema(name="doc_type", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="security_level", dtype=DataType.VARCHAR, max_length=32),
    FieldSchema(name="page_no", dtype=DataType.INT64),
    FieldSchema(name="chunk_text", dtype=DataType.VARCHAR, max_length=8192, enable_analyzer=True, enable_match=True),
    FieldSchema(name="chunk_text_keyword", dtype=DataType.VARCHAR, max_length=8191),
    FieldSchema(name="chunk_text_ws", dtype=DataType.VARCHAR, max_length=8192, enable_analyzer=True, enable_match=True),
    FieldSchema(name="embedding_vector", dtype=DataType.FLOAT_VECTOR, dim=1024),
    FieldSchema(name="version_no", dtype=DataType.INT64),
    FieldSchema(name="created_at", dtype=DataType.VARCHAR, max_length=64),
]


DEFAULT_DIM = 1024


class MilvusVectorStore(VectorStore):
    """Milvus backend for KAG chunks and long-term memory."""

    def __init__(
        self,
        host: str,
        port: int,
        user: str,
        password: str,
        default_collection: str,
    ) -> None:
        self._host = host
        self._port = port
        self._user = user
        self._password = password
        self._default_collection = default_collection
        self._pk_field: dict[str, str] = {}
        try:
            if not connections.has_connection("default"):
                connections.connect(
                    alias="default",
                    host=host,
                    port=port,
                    user=user,
                    password=password,
                )
        except Exception as e:
            logger.error("Milvus connection failed: %s", e)
            raise

    @property
    def default_collection(self) -> str:
        return self._default_collection

    def backend_name(self) -> str:
        return "milvus"

    def ensure_collection(self, name: str, dims: int) -> None:
        if utility.has_collection(name):
            return

        fields: list[FieldSchema] = []
        for field in CHUNK_FIELDS:
            if field.name == "embedding_vector":
                fields.append(
                    FieldSchema(name="embedding_vector", dtype=DataType.FLOAT_VECTOR, dim=dims)
                )
            else:
                fields.append(field)

        schema = CollectionSchema(
            fields=fields,
            description="JC RAG KAG chunks",
            enable_dynamic_field=False,
        )
        coll = Collection(name=name, schema=schema)
        coll.create_index(
            field_name="embedding_vector",
            index_params={
                "metric_type": "COSINE",
                "index_type": "HNSW",
                "params": {"M": 16, "efConstruction": 64},
            },
        )
        for text_field in ("chunk_text", "chunk_text_ws"):
            try:
                coll.create_index(
                    field_name=text_field,
                    index_params={"index_type": "INVERTED"},
                )
            except Exception as e:
                logger.warning("Milvus create_index %s failed (non-fatal): %s", text_field, e)
        for scalar_field in ("document_id", "kb_id", "file_name"):
            try:
                coll.create_index(
                    field_name=scalar_field,
                    index_params={"index_type": "INVERTED"},
                )
            except Exception as e:
                logger.warning("Milvus create_index %s failed (non-fatal): %s", scalar_field, e)
        coll.load()
        self._pk_field[name] = "chunk_id"
        logger.info("Milvus collection ready: %s (dims=%d)", name, dims)

    def ensure_memory_collection(self, name: str, dims: int) -> None:
        if utility.has_collection(name):
            self._pk_field.setdefault(name, "memory_id")
            return

        fields: list[FieldSchema] = [
            FieldSchema(name="memory_id", dtype=DataType.VARCHAR, max_length=64, is_primary=True),
            FieldSchema(name="user_id", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="session_id", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="key", dtype=DataType.VARCHAR, max_length=256),
            FieldSchema(name="value", dtype=DataType.VARCHAR, max_length=8192, enable_analyzer=True),
            FieldSchema(name="category", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="source", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="active", dtype=DataType.BOOL),
            FieldSchema(name="embedding_vector", dtype=DataType.FLOAT_VECTOR, dim=dims),
            FieldSchema(name="last_used_at", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="created_at", dtype=DataType.VARCHAR, max_length=64),
            FieldSchema(name="updated_at", dtype=DataType.VARCHAR, max_length=64),
        ]
        schema = CollectionSchema(
            fields=fields,
            description="JC RAG L2 memory",
            enable_dynamic_field=False,
        )
        coll = Collection(name=name, schema=schema)
        coll.create_index(
            field_name="embedding_vector",
            index_params={
                "metric_type": "COSINE",
                "index_type": "HNSW",
                "params": {"M": 16, "efConstruction": 64},
            },
        )
        try:
            coll.create_index(field_name="value", index_params={"index_type": "INVERTED"})
        except Exception as e:
            logger.warning("Milvus create_index value failed (non-fatal): %s", e)
        for scalar_field in ("user_id", "category", "active", "session_id"):
            try:
                coll.create_index(
                    field_name=scalar_field,
                    index_params={"index_type": "INVERTED"},
                )
            except Exception as e:
                logger.warning("Milvus create_index %s failed (non-fatal): %s", scalar_field, e)
        coll.load()
        self._pk_field[name] = "memory_id"
        logger.info("Milvus memory collection ready: %s (dims=%d)", name, dims)

    def _get_collection(self, collection: str | None) -> Collection:
        name = collection or self._default_collection
        if not utility.has_collection(name):
            raise ValueError(f"Milvus collection '{name}' does not exist. Call ensure_collection first.")
        return Collection(name)

    def _pk(self, coll: Collection) -> str:
        return self._pk_field.get(coll.name, "chunk_id")

    def bulk_index(
        self,
        docs: list[dict[str, Any]],
        collection: str | None = None,
    ) -> tuple[int, int]:
        coll = self._get_collection(collection)
        if not docs:
            return 0, 0

        field_schemas = {field.name: field for field in coll.schema.fields}
        field_names = list(field_schemas.keys())

        def _coerce(value: Any, field_name: str) -> Any:
            if value is not None:
                return value
            field = field_schemas.get(field_name)
            if field is None:
                return None
            if field.dtype == DataType.INT64:
                return 0
            if field.dtype == DataType.VARCHAR:
                return ""
            if field.dtype == DataType.BOOL:
                return False
            if field.dtype == DataType.FLOAT_VECTOR:
                return []
            return None

        insert_data: list[list[Any]] = []
        for field_name in field_names:
            column: list[Any] = []
            for doc in docs:
                column.append(_coerce(doc.get(field_name), field_name))
            insert_data.append(column)

        try:
            result = coll.insert(insert_data)
            try:
                coll.flush()
            except Exception as flush_err:
                logger.warning("Milvus bulk_index flush failed (non-fatal): %s", flush_err)
            return int(result.insert_count), 0
        except Exception as e:
            logger.error("Milvus bulk_index failed: %s", e)
            return 0, len(docs)

    def index_one(
        self,
        doc_id: str,
        doc: dict[str, Any],
        collection: str | None = None,
    ) -> None:
        coll = self._get_collection(collection)
        pk = self._pk(coll)
        payload = {**doc, pk: doc_id}
        field_schemas = {field.name: field for field in coll.schema.fields}
        field_names = list(field_schemas.keys())
        insert_data: list[list[Any]] = []

        for field_name in field_names:
            value = payload.get(field_name)
            if value is None:
                field = field_schemas.get(field_name)
                if field is not None:
                    if field.dtype == DataType.INT64:
                        value = 0
                    elif field.dtype == DataType.VARCHAR:
                        value = ""
                    elif field.dtype == DataType.BOOL:
                        value = False
                    elif field.dtype == DataType.FLOAT_VECTOR:
                        value = []
            insert_data.append([value])

        try:
            if hasattr(coll, "upsert"):
                coll.upsert(insert_data)
            else:
                coll.insert(insert_data)
            try:
                coll.flush()
            except Exception as flush_err:
                logger.warning("Milvus index_one flush failed (non-fatal): %s", flush_err)
        except Exception as e:
            logger.error("Milvus index_one failed: %s", e)
            raise

    def delete_by_document_id(
        self,
        document_id: str,
        collection: str | None = None,
    ) -> None:
        coll = self._get_collection(collection)
        coll.delete(expr=f'document_id == "{document_id}"')

    def delete_by_ids(
        self,
        ids: list[str],
        collection: str | None = None,
    ) -> None:
        if not ids:
            return
        coll = self._get_collection(collection)
        quoted = ", ".join(f'"{doc_id}"' for doc_id in ids)
        coll.delete(expr=f"chunk_id in [{quoted}]")

    def delete_by_filter(
        self,
        filter: FilterExpr,
        collection: str | None = None,
    ) -> int:
        coll = self._get_collection(collection)
        expr = filter.to_milvus() if filter else ""
        if not expr:
            return 0
        result = coll.delete(expr=expr)
        return int(result.delete_count or 0) if hasattr(result, "delete_count") else 0

    def vector_search(
        self,
        query_vector: list[float],
        collection: str | None = None,
        filter: FilterExpr | None = None,
        top_k: int = 5,
        top_n: int = 20,
    ) -> list[dict[str, Any]]:
        coll = self._get_collection(collection)
        search_params = {"metric_type": "COSINE", "params": {"ef": max(64, top_n)}}
        expr = filter.to_milvus() if filter else None
        try:
            results = coll.search(
                data=[query_vector],
                anns_field="embedding_vector",
                param=search_params,
                limit=top_k,
                expr=expr,
                output_fields=["*"],
            )
        except Exception as e:
            logger.error("Milvus vector_search failed: %s", e)
            return []

        hits: list[dict[str, Any]] = []
        if not results:
            return hits
        for hit in results[0]:
            entity = hit.entity.to_dict() if hasattr(hit, "entity") else {}
            hits.append(
                {
                    "_id": str(hit.id),
                    "_score": float(hit.distance) if hit.distance is not None else 0.0,
                    "_source": entity,
                }
            )
        return hits

    def text_search(
        self,
        query: str,
        collection: str | None = None,
        filter: FilterExpr | None = None,
        top_k: int = 5,
    ) -> list[dict[str, Any]]:
        coll = self._get_collection(collection)
        safe_query = query.replace('"', "").strip()
        if not safe_query:
            return []

        expr_parts: list[str] = [f'TEXT_MATCH(chunk_text, "{safe_query}")']
        if filter:
            expr_parts.append(filter.to_milvus())
        expr = " and ".join(expr_parts)
        zero_vec = [0.0] * self._default_dim()

        try:
            results = coll.search(
                data=[zero_vec],
                anns_field="embedding_vector",
                param={"metric_type": "COSINE", "params": {"ef": max(64, top_k * 4)}},
                limit=top_k,
                expr=expr,
                output_fields=["*"],
            )
        except Exception as e:
            logger.error("Milvus text_search failed: %s", e)
            return []

        hits: list[dict[str, Any]] = []
        if not results:
            return hits
        for hit in results[0]:
            entity = hit.entity.to_dict() if hasattr(hit, "entity") else {}
            hits.append(
                {
                    "_id": str(hit.id),
                    "_score": float(hit.distance) if hit.distance is not None else 0.0,
                    "_source": entity,
                }
            )
        return hits

    def _default_dim(self) -> int:
        try:
            from app.embeddings import _resolve_default_dim  # type: ignore

            return int(_resolve_default_dim())
        except Exception:
            return DEFAULT_DIM

    def get_by_id(
        self,
        doc_id: str,
        collection: str | None = None,
    ) -> dict[str, Any] | None:
        coll = self._get_collection(collection)
        pk = self._pk(coll)
        try:
            results = coll.query(
                expr=f'{pk} == "{doc_id}"',
                output_fields=["*"],
                limit=1,
            )
        except Exception as e:
            logger.error("Milvus get_by_id failed: %s", e)
            return None
        if not results:
            return None
        return results[0]

    def update_by_id(
        self,
        doc_id: str,
        partial: dict[str, Any],
        collection: str | None = None,
    ) -> None:
        coll = self._get_collection(collection)
        pk = self._pk(coll)
        existing = self.get_by_id(doc_id, collection)
        if existing is None:
            logger.warning("Milvus update_by_id: %s=%s not found", pk, doc_id)
            return

        merged = {**existing, **partial, pk: doc_id}
        field_schemas = {field.name: field for field in coll.schema.fields}
        field_names = list(field_schemas.keys())
        insert_data: list[list[Any]] = []

        for field_name in field_names:
            value = merged.get(field_name)
            if value is None:
                field = field_schemas.get(field_name)
                if field is not None:
                    if field.dtype == DataType.INT64:
                        value = 0
                    elif field.dtype == DataType.VARCHAR:
                        value = ""
                    elif field.dtype == DataType.BOOL:
                        value = False
                    elif field.dtype == DataType.FLOAT_VECTOR:
                        value = []
            insert_data.append([value])

        try:
            if hasattr(coll, "upsert"):
                coll.upsert(insert_data)
            else:
                coll.delete(expr=f'{pk} == "{doc_id}"')
                coll.insert(insert_data)
        except Exception as e:
            logger.error("Milvus update_by_id failed: %s", e)
            raise

    def count(
        self,
        filter: FilterExpr | None = None,
        collection: str | None = None,
    ) -> int:
        coll = self._get_collection(collection)
        expr = filter.to_milvus() if filter else ""
        try:
            results = coll.query(
                expr=expr or "",
                output_fields=["count(*)"],
                limit=1,
            )
        except Exception as e:
            logger.error("Milvus count failed: %s", e)
            return 0
        if not results:
            return 0
        return int(results[0].get("count(*)", 0))

    def update_by_filter(
        self,
        filter: FilterExpr,
        script_source: str,
        script_params: dict[str, Any] | None = None,
        collection: str | None = None,
    ) -> int:
        coll = self._get_collection(collection)
        if "active = false" in script_source and "active" in script_source:
            try:
                results = coll.query(
                    expr=filter.to_milvus(),
                    output_fields=["*"],
                    limit=10000,
                )
            except Exception as e:
                logger.error("Milvus update_by_filter query failed: %s", e)
                return 0
            if not results:
                return 0

            now = (script_params or {}).get("now", "")
            field_schemas = {field.name: field for field in coll.schema.fields}
            field_names = list(field_schemas.keys())
            update_data: list[list[Any]] = []

            for entity in results:
                entity["active"] = False
                if now:
                    entity["updated_at"] = now
                row: list[Any] = []
                for field_name in field_names:
                    value = entity.get(field_name)
                    if value is None:
                        field = field_schemas.get(field_name)
                        if field is not None:
                            if field.dtype == DataType.INT64:
                                value = 0
                            elif field.dtype == DataType.VARCHAR:
                                value = ""
                            elif field.dtype == DataType.BOOL:
                                value = False
                            elif field.dtype == DataType.FLOAT_VECTOR:
                                value = []
                    row.append(value)
                update_data.append(row)

            try:
                if hasattr(coll, "upsert"):
                    coll.upsert(update_data)
                else:
                    coll.insert(update_data)
                return len(results)
            except Exception as e:
                logger.error("Milvus update_by_filter upsert failed: %s", e)
                return 0

        logger.warning("Milvus update_by_filter only supports simple soft delete: %s", script_source)
        return 0

    def health_check(self) -> bool:
        try:
            return utility.get_server_version() is not None
        except Exception as e:
            logger.warning("Milvus health check failed: %s", e)
            return False
