"""会话/上下文存储：Redis 优先，故障回退进程内存。

Redis 键：
- ai:session:{sessionId}  列表，JSON 消息（cap 20 条，TTL 30 分钟滑动刷新）
- ai:ctx:{sessionId}      字符串，JSON（检索上下文：products/categories/toolCalled）

内存兜底为 app.session_store.SessionStore + 本类内的 dict（ctx）。
Redis 故障熔断 30 秒后自动重试，服务无需重启。
"""

import json
import threading
import time

import redis

from app.session_store import SessionStore

SESSION_KEY_PREFIX = "ai:session:"
CTX_KEY_PREFIX = "ai:ctx:"


class RedisSessionStore:
    """Redis 版会话历史 + 上下文存储。"""

    def __init__(self, host, port, db, password, ttl_seconds=1800, max_messages=20):
        self._host = host
        self._port = port
        self._db = db
        self._password = password or None
        self._ttl = ttl_seconds
        self._max_messages = max_messages
        self._client = None
        self._client_lock = threading.Lock()

    def _get_client(self):
        with self._client_lock:
            if self._client is None:
                self._client = redis.Redis(
                    host=self._host,
                    port=self._port,
                    db=self._db,
                    password=self._password,
                    socket_connect_timeout=2,
                    socket_timeout=2,
                    decode_responses=True,
                    # 本机 Windows 版 Redis 6.2 对 RESP3 的 HELLO 握手会挂起不响应，
                    # redis-py 8 默认 RESP3，必须强制 RESP2（实测 RESP2 秒连）
                    protocol=2,
                )
            return self._client

    def add(self, session_id, role, content):
        key = SESSION_KEY_PREFIX + session_id
        client = self._get_client()
        pipe = client.pipeline()
        pipe.rpush(key, json.dumps({"role": role, "content": content}, ensure_ascii=False))
        pipe.ltrim(key, -self._max_messages, -1)
        pipe.expire(key, self._ttl)
        pipe.execute()

    def history(self, session_id):
        key = SESSION_KEY_PREFIX + session_id
        client = self._get_client()
        raw = client.lrange(key, 0, -1)
        if not raw:
            return []
        client.expire(key, self._ttl)  # 滑动过期
        return [json.loads(item) for item in raw]

    def save_ctx(self, session_id, payload):
        client = self._get_client()
        client.set(CTX_KEY_PREFIX + session_id, json.dumps(payload, ensure_ascii=False), ex=self._ttl)

    def get_ctx(self, session_id):
        raw = self._get_client().get(CTX_KEY_PREFIX + session_id)
        if not raw:
            return {}
        try:
            return json.loads(raw)
        except Exception:
            return {}

    def clear(self, session_id):
        self._get_client().delete(SESSION_KEY_PREFIX + session_id)


class HybridSessionStore:
    """Redis 优先 + 内存兜底。注意：Redis 故障期间新增消息只进内存，
    恢复后历史可能不完整，属可接受的降级行为。"""

    def __init__(self, settings, memory_store=None):
        self._redis_store = RedisSessionStore(
            host=settings.redis_host,
            port=settings.redis_port,
            db=settings.redis_db,
            password=settings.redis_password,
            ttl_seconds=settings.redis_session_ttl_seconds,
        )
        self._memory_store = memory_store or SessionStore(ttl_seconds=settings.redis_session_ttl_seconds)
        self._memory_ctx = {}
        self._down_until = 0.0
        self._lock = threading.Lock()

    def _redis_available(self):
        with self._lock:
            return time.time() >= self._down_until

    def _mark_down(self):
        with self._lock:
            self._down_until = time.time() + 30

    def _call(self, redis_op, memory_op):
        """先试 Redis，失败标记熔断并回退内存。"""
        if self._redis_available():
            try:
                return redis_op()
            except Exception:
                self._mark_down()
        return memory_op()

    def add(self, session_id, role, content):
        self._call(
            lambda: self._redis_store.add(session_id, role, content),
            lambda: self._memory_store.add(session_id, role, content),
        )

    def history(self, session_id):
        return self._call(
            lambda: self._redis_store.history(session_id),
            lambda: self._memory_store.history(session_id),
        )

    def save_ctx(self, session_id, payload):
        def _mem():
            with self._lock:
                self._memory_ctx[session_id] = payload

        self._call(lambda: self._redis_store.save_ctx(session_id, payload), _mem)

    def get_ctx(self, session_id):
        def _mem():
            with self._lock:
                return self._memory_ctx.get(session_id, {})

        return self._call(lambda: self._redis_store.get_ctx(session_id), _mem)

    def clear(self, session_id):
        self._call(
            lambda: self._redis_store.clear(session_id),
            lambda: self._memory_store.clear(session_id),
        )
