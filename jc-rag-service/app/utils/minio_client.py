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


"""MinIO 客户端封装模块"""
from __future__ import annotations

import tempfile

from minio import Minio

from app.core.config import get_settings


def get_minio_client() -> Minio:
    """获取 MinIO 客户端（单例）"""
    settings = get_settings()
    endpoint = str(settings.storage.minio.endpoint or "").strip()
    secure = bool(settings.storage.minio.secure)
    
    if endpoint.startswith("http://"):
        endpoint = endpoint[len("http://"):]
        secure = False
    elif endpoint.startswith("https://"):
        endpoint = endpoint[len("https://"):]
        secure = True
    
    return Minio(
        endpoint,
        access_key=settings.storage.minio.access_key,
        secret_key=settings.storage.minio.secret_key,
        secure=secure,
    )


def download_object_to_temp_file(bucket: str, object_name: str, file_ext: str) -> str:
    """下载对象到临时文件并返回文件路径"""
    client = get_minio_client()
    suffix = f".{file_ext}" if file_ext else ".bin"
    
    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
    response = None
    
    try:
        response = client.get_object(bucket, object_name)
        for chunk in response.stream(1024 * 1024):
            if chunk:
                temp_file.write(chunk)
        temp_file.flush()
        return temp_file.name
    finally:
        temp_file.close()
        if response is not None:
            response.close()
            response.release_conn()


def ensure_bucket(bucket_name: str) -> None:
    """确保 Bucket 存在"""
    client = get_minio_client()
    if not client.bucket_exists(bucket_name):
        client.make_bucket(bucket_name)


def list_objects(bucket_name: str, prefix: str = "") -> list[str]:
    """列出 Bucket 中的对象"""
    client = get_minio_client()
    objects = []
    
    for obj in client.list_objects(bucket_name, prefix=prefix, recursive=True):
        objects.append(obj.object_name)
    
    return objects
