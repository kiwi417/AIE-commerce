"""LLM 实例与意图分类。"""

import re

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage
from langchain_deepseek import ChatDeepSeek
from pydantic import BaseModel

SYSTEM_PROMPT = (
    "你是电商平台的AI导购助手，语气亲切友好，回答简洁、使用中文。"
    "你可以进行商品咨询、推荐与对比，解答活动、售后等常见问题。"
    "当消息中包含【商品库检索结果】时，优先推荐其中与用户需求匹配的真实商品，"
    "推荐时给出商品名、价格与库存，并说明推荐理由；"
    "若检索结果为空或与需求不相关，明确告知未找到匹配商品，禁止编造商品库中不存在的商品。"
    "没有检索结果时按你的知识正常回答。"
    "本阶段你只能进行对话咨询，不能执行下单、加购等操作，"
    "遇到操作类请求时请引导用户前往购物车或订单页面自行完成。"
)

CLASSIFY_PROMPT = (
    "你是电商客服意图分类器。请将用户的问题严格分为以下三类，只输出分类：\n"
    "CONSULTATION：商品咨询、推荐、对比，活动/售后政策咨询等纯对话问题\n"
    "ANALYTICS：销量统计、排行榜、数据分析、报表类问题\n"
    "OPERATION：下单、加购、改地址、退换货等操作类请求\n"
    "用户问题："
)

# 本阶段 agent 与 intent 同值（RAG/TextToSQL/Tools 分派是后续阶段）
INTENTS = ("CONSULTATION", "ANALYTICS", "OPERATION")


class IntentResult(BaseModel):
    intent: str


_chat_llm = None
_classify_llm = None


def get_chat_llm(settings):
    """对话模型：惰性单例，ChatDeepSeek 线程安全可并发调用。"""
    global _chat_llm
    if _chat_llm is None:
        _chat_llm = ChatDeepSeek(
            model=settings.model,
            api_key=settings.deepseek_api_key,
            api_base=settings.deepseek_base_url,
            temperature=0.7,
            timeout=settings.answer_timeout_seconds,
            max_retries=2,
        )
    return _chat_llm


def get_classify_llm(settings):
    """分类模型：低温度、短输出，独立超时，避免拖慢主链路。"""
    global _classify_llm
    if _classify_llm is None:
        _classify_llm = ChatDeepSeek(
            model=settings.model,
            api_key=settings.deepseek_api_key,
            api_base=settings.deepseek_base_url,
            temperature=0,
            max_tokens=32,
            timeout=settings.classify_timeout_seconds,
            max_retries=1,
        )
    return _classify_llm


def classify_intent(llm, question):
    """分类失败（超时/网络/解析）一律降级 CONSULTATION，绝不让分类阻塞主链路。"""
    try:
        result = llm.with_structured_output(IntentResult).invoke(CLASSIFY_PROMPT + question)
        intent = (result.intent or "").strip().upper()
        if intent in INTENTS:
            return intent
    except Exception:
        pass
    # 兜底：纯文本输出 + 正则匹配
    try:
        text = llm.invoke(CLASSIFY_PROMPT + question).content or ""
        match = re.search("|".join(INTENTS), text.upper())
        if match:
            return match.group(0)
    except Exception:
        pass
    return "CONSULTATION"


def build_rag_context(products):
    """检索结果 → RAG 上下文块（无结果返回 None）。"""
    if not products:
        return None
    lines = ["【商品库检索结果】以下是与你问题相关的在售商品，请结合真实信息推荐："]
    for i, p in enumerate(products, 1):
        desc = (p.get("description") or "")[:80]
        lines.append(
            f"{i}. {p.get('productName')}｜价格 ¥{p.get('price')}｜库存 {p.get('stock')}"
            f"｜货架 {p.get('shelfArea')}｜{desc}"
        )
    return "\n".join(lines)


def build_messages(history, rag_context=None):
    """会话历史（已含当前用户问题）→ LangChain 消息列表；有检索结果时注入 RAG 块。"""
    messages = [SystemMessage(content=SYSTEM_PROMPT)]
    if rag_context:
        messages.append(SystemMessage(content=rag_context))
    for item in history:
        if item["role"] == "user":
            messages.append(HumanMessage(content=item["content"]))
        else:
            messages.append(AIMessage(content=item["content"]))
    return messages


def chat_answer(llm, history, rag_context=None):
    """非流式回答。"""
    return llm.invoke(build_messages(history, rag_context)).content


def chat_stream(llm, history, rag_context=None):
    """流式回答：逐块产出文本。"""
    for chunk in llm.stream(build_messages(history, rag_context)):
        if chunk.content:
            yield chunk.content
