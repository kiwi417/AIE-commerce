// 门户匿名会话ID：所有门户页面共享 localStorage 同一个 key
// 购物车、AI 聊天会话、检索上下文都基于这个 id，各页面必须用同一份工具函数获取
// （历史教训：从 AiChatBox 子组件 ref 上取 id 会在组件挂载前拿到空串，导致购物车查不到）
const PORTAL_SESSION_KEY = 'cdc_chat_session_id'

export function getPortalSessionId() {
   let sid = localStorage.getItem(PORTAL_SESSION_KEY)
   if (!sid) {
      sid = Date.now().toString(36)
      localStorage.setItem(PORTAL_SESSION_KEY, sid)
   }
   return sid
}

// 清空聊天时换新会话（购物车不随之清空，仍按旧 id 读取）
export function refreshPortalSessionId() {
   const sid = Date.now().toString(36)
   localStorage.setItem(PORTAL_SESSION_KEY, sid)
   return sid
}
