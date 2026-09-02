"""会话历史存储：进程内存 + 惰性 TTL + 条数上限。

注意：uvicorn 必须单 worker（默认即是），多 worker 会各持一份内存导致会话丢失。
扩展点：数据量上来后可替换为 Redis 实现，保持接口不变。
"""

import threading
import time
from collections import deque


class _Session:
    def __init__(self, max_messages):
        self.messages = deque(maxlen=max_messages)
        self.last_access = time.time()


class SessionStore:
    """线程安全的会话历史存储。锁内只做纯内存操作，不做 I/O。"""

    def __init__(self, ttl_seconds=1800, max_messages=20):
        self._ttl = ttl_seconds
        self._max_messages = max_messages
        self._data = {}
        self._lock = threading.RLock()

    def add(self, session_id, role, content):
        """追加一条消息，role 为 user / assistant。会话不存在时惰性创建。"""
        with self._lock:
            session = self._data.get(session_id)
            if session is None:
                session = self._data[session_id] = _Session(self._max_messages)
            session.messages.append({"role": role, "content": content})
            session.last_access = time.time()

    def history(self, session_id):
        """返回该会话的消息列表（拷贝），同时做惰性过期清理。"""
        with self._lock:
            session = self._data.get(session_id)
            if session is None:
                return []
            if time.time() - session.last_access > self._ttl:
                del self._data[session_id]
                return []
            session.last_access = time.time()
            return list(session.messages)

    def clear(self, session_id):
        with self._lock:
            self._data.pop(session_id, None)
