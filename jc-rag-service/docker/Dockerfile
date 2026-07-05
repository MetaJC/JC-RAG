# =====================================================================
# JC RAG 开源版 Python 服务 镜像 (port 8000)
# 作者: JC
# 用途: 开源版 RAG 主服务 (FastAPI) - 解析/检索/嵌入/LLM 路由
# 构建: docker build -f jc-rag-community/jc-rag-service/docker/Dockerfile -t jc-rag-python-community:0.0.1-SNAPSHOT .
# =====================================================================

# ============== Stage 1: Build ==============
FROM python:3.11-slim AS builder
WORKDIR /src

# 仅复制 community Python 服务源码
COPY jc-rag-community/jc-rag-service/requirements.txt /src/requirements.txt
COPY jc-rag-community/jc-rag-service/app             /src/app
COPY jc-rag-community/jc-rag-service/main.py         /src/main.py
COPY jc-rag-community/jc-rag-service/rag_config.yml  /src/rag_config.yml

# 依赖装到 /install
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# ============== Stage 2: Runtime ==============
FROM python:3.11-slim
WORKDIR /app

COPY --from=builder /install /usr/local
COPY --from=builder /src/app            /app/app
COPY --from=builder /src/main.py        /app/main.py
COPY --from=builder /src/rag_config.yml /app/rag_config.yml

ENV TZ=Asia/Shanghai \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
