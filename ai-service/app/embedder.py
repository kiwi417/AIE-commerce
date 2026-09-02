"""文本向量化：本地 BGE 模型惰性单例。

模型在第一次使用时加载（首次运行会从 HuggingFace 下载约 130MB，
网络不通可设置环境变量 HF_ENDPOINT=https://hf-mirror.com）。
模型未装好/加载失败时，知识库降级为不可用，聊天链路不受影响。
"""

import logging
import os

log = logging.getLogger("ai.embedder")

_embedder = None


def get_embedder(settings):
    """惰性单例：函数内 import，未安装 sentence-transformers 前服务可正常启动。"""
    global _embedder
    if _embedder is None:
        # 模型缓存目录重定向到 D 盘（须在 huggingface 库首次读取环境变量前设置）
        if settings.hf_home:
            os.environ.setdefault("HF_HOME", settings.hf_home)
        # 直连不通时走镜像；hf-mirror 不支持 xet 协议，须禁用
        if settings.hf_endpoint:
            os.environ.setdefault("HF_ENDPOINT", settings.hf_endpoint)
            os.environ.setdefault("HF_HUB_DISABLE_XET", "1")
        from sentence_transformers import SentenceTransformer

        log.info("加载向量模型 %s（首次运行需下载模型文件）...", settings.embedding_model)
        _embedder = SentenceTransformer(settings.embedding_model, device="cpu")
        log.info("向量模型加载完成")
    return _embedder


def encode_batch(embedder, texts):
    """批量编码并归一化（bge 系列推荐 normalize，配合内积=余弦）。"""
    import numpy as np

    vectors = embedder.encode(texts, normalize_embeddings=True, convert_to_numpy=True)
    return np.asarray(vectors, dtype=np.float32)
