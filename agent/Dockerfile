# syntax=docker/dockerfile:1
# ---- 构建阶段：uv 安装依赖到独立虚拟环境 ----
# 国内服务器上 ghcr.io 拉取 uv 镜像常超时，改从 PyPI 镜像安装 uv
FROM python:3.11-slim AS builder

WORKDIR /app
ENV TZ=Asia/Shanghai \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/ \
    UV_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/

RUN --mount=type=cache,target=/root/.cache/pip pip install --no-cache-dir uv

# 先装依赖（利用层缓存），再拷源码
COPY pyproject.toml uv.lock README.md ./
RUN --mount=type=cache,target=/root/.cache/uv \
    uv sync --index-url https://mirrors.aliyun.com/pypi/simple/ --no-install-project --no-dev --no-progress

COPY app ./app

# ---- 运行阶段：slim 运行时，仅带 venv 与源码 ----
FROM python:3.11-slim

WORKDIR /app
ENV PATH="/app/.venv/bin:$PATH" PYTHONUNBUFFERED=1

COPY --from=builder /app/.venv /app/.venv
COPY app ./app
# 品类洞察知识库文档（启动时灌入 RAG 向量库）
COPY knowledge ./knowledge

# 运行时数据目录（会话/偏好；向量库走 QDRANT_URL 指向 qdrant 服务）
RUN mkdir -p /app/data
ENV DATA_DIR=/app/data

EXPOSE 8000
# P0-6：绑定地址由 HOST 环境变量决定，默认仅回环；
# 容器内需要对外时由 compose 显式设置 HOST=0.0.0.0（且端口只映射到宿主机回环）
CMD ["sh", "-c", "uvicorn app.presentation.server:app --host ${HOST:-127.0.0.1} --port ${PORT:-8000}"]
