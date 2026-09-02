"""商品知识库（RAG 数据源）：从 Java 拉商品 → BGE 向量化 → 向量库检索。

- 同步触发：启动时后台线程立即 build + 每 KB_REFRESH_INTERVAL_SECONDS 一次 + 手动 POST /ai/kb/refresh
- Java 未启动/拉取失败：保留旧知识库，绝不因同步失败崩服务
- 检索：embed 问题 → 向量库 top-k → 分数阈值过滤 → 商品 dict（含前端卡片所需字段）
"""

import json
import logging
import os
import threading
import time

import httpx

from app.embedder import encode_batch, get_embedder
from app.vector_store import create_vector_store, load_vector_store

log = logging.getLogger("ai.product_sync")

# bge-small-zh-v1.5 向量维度
EMBED_DIM = 512


def doc_text(p, categories):
    """商品 → 检索文本（名称/标签/描述主导向量；价格库存简写）。"""
    name = p.get("productName") or ""
    cat = categories.get(p.get("categoryId"), "")
    price = p.get("price")
    stock = p.get("stock")
    unit = p.get("unit") or ""
    tags = p.get("tags") or ""
    shelf = p.get("shelfArea") or ""
    desc = p.get("description") or ""
    return f"名称:{name} 分类:{cat} 价格:{price}元 库存:{stock}{unit} 标签:{tags} 货架:{shelf} 描述:{desc}"


class ProductKB:
    def __init__(self, settings):
        self._settings = settings
        self._kb_dir = os.path.join(settings.data_dir, "kb")
        self._lock = threading.Lock()
        self._embedder = None
        self._embed_failed = False
        self.store = None      # VectorStore（faiss/numpy）
        self.products = []     # 商品全量 dict（与 store.ids 行序对齐）
        self.categories = {}   # categoryId -> categoryName

    # ---------- 内部 ----------

    def _embedder_or_none(self):
        """模型加载失败只熔断知识库，不抛异常。"""
        if self._embed_failed:
            return None
        if self._embedder is None:
            try:
                self._embedder = get_embedder(self._settings)
            except Exception as e:
                log.warning("向量模型加载失败，知识库暂不可用: %s", e)
                self._embed_failed = True
                return None
        return self._embedder

    def _fetch_once(self):
        base = self._settings.java_base_url.rstrip("/")
        with httpx.Client(timeout=10) as client:
            resp = client.get(f"{base}/portal/category/list")
            resp.raise_for_status()
            cats = {}
            for c in resp.json().get("data") or []:
                if c.get("id") is not None:
                    cats[c.get("id")] = c.get("categoryName")
            products = []
            page_num, page_size, total = 1, 100, None
            while True:
                resp = client.get(f"{base}/portal/product/page",
                                  params={"pageNum": page_num, "pageSize": page_size})
                resp.raise_for_status()
                page = resp.json()
                rows = page.get("rows") or []
                products.extend(rows)
                if total is None:
                    total = page.get("total") or 0
                if not rows or len(products) >= total:
                    break
                page_num += 1
            return products, cats

    def _fetch(self):
        """带重试的拉取，最终失败抛异常。"""
        last = None
        for attempt in range(3):
            try:
                return self._fetch_once()
            except Exception as e:
                last = e
                time.sleep(2 * (attempt + 1))
        raise last

    # ---------- 对外 ----------

    def load(self):
        """启动时先加载落盘索引（Java 未启动时知识库仍有旧数据）。"""
        try:
            meta_path = os.path.join(self._kb_dir, "products.json")
            if not os.path.exists(meta_path):
                return False
            with open(meta_path, encoding="utf-8") as f:
                products = json.load(f)
            cats_path = os.path.join(self._kb_dir, "categories.json")
            categories = {}
            if os.path.exists(cats_path):
                with open(cats_path, encoding="utf-8") as f:
                    categories = json.load(f)
            store = load_vector_store(self._settings.vector_backend, EMBED_DIM, self._kb_dir)
            if store is None or store.count() == 0 or len(store.ids) != len(products):
                log.warning("落盘知识库不完整，等待后台重建")
                return False
            with self._lock:
                self.store = store
                self.products = products
                self.categories = categories
            log.info("知识库从磁盘加载: %s 个商品", len(products))
            return True
        except Exception as e:
            log.warning("加载落盘知识库失败: %s", e)
            return False

    def build(self):
        """拉取→向量化→落盘→换入。失败保留旧知识库，返回 (ok, message)。"""
        products, cats = self._fetch()
        if not products:
            return False, "Java 返回商品为空，保留现有知识库"
        embedder = self._embedder_or_none()
        if embedder is None:
            return False, "向量模型不可用"
        texts = [doc_text(p, cats) for p in products]
        try:
            vectors = encode_batch(embedder, texts)
        except Exception as e:
            return False, f"向量化失败: {e}"

        new_store = create_vector_store(self._settings.vector_backend, EMBED_DIM)
        ids = [p.get("id") for p in products]
        new_store.add_vectors(ids, vectors)

        try:
            new_store.save(self._kb_dir)
            with open(os.path.join(self._kb_dir, "products.json"), "w", encoding="utf-8") as f:
                json.dump(products, f, ensure_ascii=False)
            with open(os.path.join(self._kb_dir, "categories.json"), "w", encoding="utf-8") as f:
                json.dump(cats, f, ensure_ascii=False)
        except Exception as e:
            log.warning("知识库落盘失败（不影响内存使用）: %s", e)

        with self._lock:
            self.store = new_store
            self.products = products
            self.categories = cats
        return True, f"已同步 {len(products)} 个商品"

    def search(self, question, top_k=None, threshold=None):
        """检索与问题相关的商品（无结果/模型不可用返回空列表，绝不抛异常）。"""
        top_k = top_k or self._settings.retrieval_top_k
        threshold = threshold if threshold is not None else self._settings.retrieval_score_threshold
        with self._lock:
            store = self.store
        if store is None or store.count() == 0:
            return []
        embedder = self._embedder_or_none()
        if embedder is None:
            return []
        try:
            vec = encode_batch(embedder, [question])[0]
        except Exception as e:
            log.warning("问题向量化失败: %s", e)
            return []
        try:
            hits = store.search(vec, top_k)
        except Exception as e:
            log.warning("向量检索失败: %s", e)
            return []

        with self._lock:
            by_id = {p.get("id"): p for p in self.products}
            cats = dict(self.categories)
        results = []
        for pid, score in hits:
            if score < threshold:
                continue
            p = by_id.get(pid)
            if p is None:
                continue
            results.append({
                "id": p.get("id"),
                "productId": p.get("id"),
                "productName": p.get("productName"),
                "price": p.get("price"),
                "stock": p.get("stock"),
                "shelfArea": p.get("shelfArea"),
                "categoryId": p.get("categoryId"),
                "categoryName": cats.get(p.get("categoryId"), ""),
                "description": p.get("description"),
            })
        return results

    def background_loop(self):
        """守护线程：立即同步一次，之后按间隔重试（Java 未启动时静默重试）。"""
        while True:
            try:
                ok, msg = self.build()
                log.info("知识库同步: %s", msg)
            except Exception as e:
                log.warning("知识库同步失败（Java 未启动时属正常）: %s", e)
            time.sleep(self._settings.kb_refresh_interval_seconds)
