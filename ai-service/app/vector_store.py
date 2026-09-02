"""向量库：faiss（IndexFlatIP 余弦）优先，numpy 暴力检索兜底，语义一致。

持久化目录 {DATA_DIR}/kb/：
- faiss 后端：index.faiss + ids.npy
- numpy 后端：matrix.npy + ids.npy
ids 与矩阵行序对齐（存商品id）。
"""

import logging
import os

import numpy as np

log = logging.getLogger("ai.vector_store")


class FaissVectorStore:
    def __init__(self, dim):
        import faiss

        self.dim = dim
        self.index = faiss.IndexFlatIP(dim)
        self.ids = []

    def add_vectors(self, ids, vectors):
        vectors = np.asarray(vectors, dtype=np.float32)
        self.index.add(vectors)
        self.ids.extend(ids)

    def search(self, query_vec, k):
        query_vec = np.asarray([query_vec], dtype=np.float32)
        scores, indices = self.index.search(query_vec, k)
        return [(int(self.ids[i]), float(s)) for i, s in zip(indices[0], scores[0]) if i != -1]

    def count(self):
        return int(self.index.ntotal)

    def save(self, path):
        import faiss

        os.makedirs(path, exist_ok=True)
        faiss.write_index(self.index, os.path.join(path, "index.faiss"))
        np.save(os.path.join(path, "ids.npy"), np.asarray(self.ids, dtype=np.int64))

    @classmethod
    def load(cls, path, dim):
        import faiss

        store = cls(dim)
        store.index = faiss.read_index(os.path.join(path, "index.faiss"))
        ids_path = os.path.join(path, "ids.npy")
        store.ids = np.load(ids_path).tolist() if os.path.exists(ids_path) else []
        return store


class NumpyVectorStore:
    """兜底实现：矩阵点积 + argpartition 取 top-k，与 faiss 语义一致。"""

    def __init__(self, dim):
        self.dim = dim
        self.matrix = np.zeros((0, dim), dtype=np.float32)
        self.ids = []

    def add_vectors(self, ids, vectors):
        vectors = np.asarray(vectors, dtype=np.float32)
        self.matrix = np.vstack([self.matrix, vectors])
        self.ids.extend(ids)

    def search(self, query_vec, k):
        if self.matrix.shape[0] == 0:
            return []
        scores = self.matrix @ np.asarray(query_vec, dtype=np.float32)
        k = min(k, len(scores))
        top = np.argpartition(scores, -k)[-k:]
        top = top[np.argsort(scores[top])[::-1]]
        return [(int(self.ids[i]), float(scores[i])) for i in top]

    def count(self):
        return len(self.ids)

    def save(self, path):
        os.makedirs(path, exist_ok=True)
        np.save(os.path.join(path, "matrix.npy"), self.matrix)
        np.save(os.path.join(path, "ids.npy"), np.asarray(self.ids, dtype=np.int64))

    @classmethod
    def load(cls, path, dim):
        store = cls(dim)
        matrix_path = os.path.join(path, "matrix.npy")
        if os.path.exists(matrix_path):
            store.matrix = np.load(matrix_path)
            ids_path = os.path.join(path, "ids.npy")
            store.ids = np.load(ids_path).tolist() if os.path.exists(ids_path) else []
        return store


def create_vector_store(backend, dim):
    """创建空向量库；faiss 不可用时自动回退 numpy。"""
    if backend == "faiss":
        try:
            return FaissVectorStore(dim)
        except Exception as e:
            log.warning("faiss 不可用（%s），回退 numpy 暴力检索", e)
    return NumpyVectorStore(dim)


def load_vector_store(backend, dim, path):
    """从磁盘加载；失败返回 None（由调用方重建）。"""
    try:
        if backend == "faiss":
            return FaissVectorStore.load(path, dim)
        return NumpyVectorStore.load(path, dim)
    except Exception as e:
        log.warning("加载向量库失败，将重建: %s", e)
        return None
