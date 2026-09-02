"""FastAPI 端点：智能客服对话（RAG 商品推荐）。

供 Java（RuoYi）服务端内部调用，接口契约与 ruoyi-ui 前端保持一致：
- /ai/chat/stream 返回裸文本流（前端 AiChatBox 逐字符追加渲染，不能是 SSE 格式）
- intent/agent 通过响应头 X-Intent/X-Agent 传出（流式）；耗时由 Java 侧测量
- 检索结果经 ai:ctx:{sessionId}（Redis/内存）传给 /ai/chat/tool-data，由前端渲染商品卡片
"""

import threading

from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app.agents import (
    build_rag_context,
    chat_answer,
    chat_stream,
    classify_intent,
    get_chat_llm,
    get_classify_llm,
)
from app.config import get_settings
from app.product_sync import ProductKB
from app.redis_store import HybridSessionStore

app = FastAPI(title="ai-service")

# LLM 调用失败时的兜底文案（纯文本，前端会直接展示）
SORRY_TEXT = "抱歉，AI服务暂时不可用，请稍后重试。"

settings = get_settings()
store = HybridSessionStore(settings)
chat_llm = get_chat_llm(settings)
classify_llm = get_classify_llm(settings)

# 商品知识库：先加载落盘索引，后台线程立即同步 + 定期刷新（失败绝不崩服务）
kb = ProductKB(settings)
kb.load()
threading.Thread(target=kb.background_loop, daemon=True).start()


class ChatRequest(BaseModel):
    session_id: str
    question: str


class StreamRequest(BaseModel):
    session_id: str
    message: str


def distinct_categories(products):
    """检索结果 → 去重分类列表（前端分类标签点选联动商品网格）。"""
    seen, result = set(), []
    for p in products:
        cid = p.get("categoryId")
        if cid is None or cid in seen:
            continue
        seen.add(cid)
        result.append({"id": cid, "categoryName": p.get("categoryName", "")})
    return result


def retrieve_for(intent, question):
    """仅咨询类意图触发检索；失败/无结果返回空列表，绝不阻塞聊天。"""
    if intent != "CONSULTATION":
        return []
    try:
        return kb.search(question)
    except Exception as e:
        return []


def save_context(session_id, products):
    """检索上下文写入会话（tool-data 读取），流开始前必须先完成。"""
    store.save_ctx(session_id, {
        "products": products,
        "categories": distinct_categories(products),
        "toolCalled": bool(products),
    })


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/ai/kb/refresh")
def kb_refresh():
    """手动重建商品知识库（同步执行，含重试，最长约 15 秒）。"""
    try:
        ok, msg = kb.build()
        return {"status": "ok" if ok else "error", "productCount": len(kb.products), "message": msg}
    except Exception as e:
        return {"status": "error", "productCount": len(kb.products), "message": str(e)}


@app.post("/ai/chat")
def chat(req: ChatRequest):
    """非流式对话：门户 chat 页使用。"""
    store.add(req.session_id, "user", req.question)
    history = store.history(req.session_id)
    intent = classify_intent(classify_llm, req.question)
    products = retrieve_for(intent, req.question)
    rag = build_rag_context(products)
    try:
        answer = chat_answer(chat_llm, history, rag)
    except Exception:
        answer = SORRY_TEXT
    store.add(req.session_id, "assistant", answer)
    save_context(req.session_id, products)
    return {
        "answer": answer,
        "toolCalled": bool(products),
        "products": products,
        "categories": distinct_categories(products),
        "intent": intent,
        "agent": intent,
    }


@app.post("/ai/chat/stream")
def stream(req: StreamRequest):
    """流式对话：裸文本流 + 响应头传意图元数据；检索上下文在流开始前落库。"""
    store.add(req.session_id, "user", req.message)
    history = store.history(req.session_id)
    # 分类与检索必须先同步完成：响应头/上下文在流开始前固定
    intent = classify_intent(classify_llm, req.message)
    products = retrieve_for(intent, req.message)
    rag = build_rag_context(products)
    save_context(req.session_id, products)

    def generate():
        acc = []
        try:
            for chunk in chat_stream(chat_llm, history, rag):
                acc.append(chunk)
                yield chunk
        except Exception:
            # LLM 调用失败：若还没产出任何内容，产出道歉文案，保证前端有内容可看
            if not acc:
                yield SORRY_TEXT
        store.add(req.session_id, "assistant", "".join(acc))

    return StreamingResponse(
        generate(),
        media_type="text/plain; charset=utf-8",
        headers={
            "X-Intent": intent,
            "X-Agent": intent,
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


@app.get("/ai/chat/tool-data")
def tool_data(session_id: str):
    """工具卡片数据：检索上下文（products/categories/toolCalled）；
    cartItems 由 Java 端合并其 Redis 购物车后返回前端。"""
    ctx = store.get_ctx(session_id)
    return {
        "products": ctx.get("products", []),
        "categories": ctx.get("categories", []),
        "cartItems": [],
        "order": None,
        "orderItems": [],
        "toolCalled": bool(ctx.get("toolCalled", False)),
    }
