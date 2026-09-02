<template>
   <div class="chat-box">
      <!-- 标题栏 -->
      <div class="chat-title">
         <span>&#x1f916; AI导购助手</span>
         <el-button text size="small" type="danger" @click="clearChat" :disabled="chatLoading">
            &#x1f5d1; 清除对话
         </el-button>
      </div>

      <!-- 消息列表 -->
      <div class="chat-messages" ref="chatBodyRef">
         <div v-for="(msg, idx) in messages" :key="idx" class="chat-msg"
            :class="msg.role === 'user' ? 'msg-user' : 'msg-ai'">
            <div class="msg-content">{{ msg.content }}</div>
            <!-- 用户上传的图片 -->
            <div v-if="msg.imageUrl" class="msg-image">
               <img :src="msg.imageUrl" />
            </div>
            <!-- 商品卡片 -->
            <div v-if="showToolCards && msg.products && msg.products.length > 0" class="mini-cards">
               <div v-for="p in msg.products" :key="p.id" class="mini-card"
                  @click="onNavigate('product', p.id)">
                  <div class="mc-name">{{ p.productName }}</div>
                  <div class="mc-row">
                     <span class="mc-price">&yen;{{ p.price }}</span>
                     <span class="mc-stock">库存{{ p.stock }}</span>
                  </div>
               </div>
            </div>
            <!-- 分类标签 -->
            <div v-if="showToolCards && msg.categories && msg.categories.length > 0" class="mini-tags">
               <el-tag v-for="c in msg.categories" :key="c.id" size="small"
                  @click="$emit('categoryChange', c.id)">{{ c.categoryName }}</el-tag>
            </div>
            <!-- 购物车卡片 -->
            <div v-if="showToolCards && msg.cartItems && msg.cartItems.length > 0" class="cart-summary-card">
               <div class="cs-title">&#x1f6d2; 购物车</div>
               <div v-for="item in msg.cartItems" :key="item.productId" class="cs-row">
                  <span class="cs-name">{{ item.productName }}</span>
                  <span class="cs-qty">×{{ item.quantity }}</span>
                  <span class="cs-price">&yen;{{ (item.price * item.quantity).toFixed(2) }}</span>
               </div>
               <div class="cs-total">
                  合计：&yen;{{ msg.cartItems.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2) }}
               </div>
               <el-button size="small" type="primary" @click="onNavigate('cart')">去购物车结算</el-button>
            </div>
            <!-- 订单卡片 -->
            <div v-if="showToolCards && msg.order" class="order-card">
               <div class="oc-title">&#x2705; 下单成功</div>
               <div class="oc-row">订单号：<b>{{ msg.order.orderNo }}</b></div>
               <div class="oc-row">金额：<b>&yen;{{ msg.order.totalAmount }}</b></div>
               <div class="oc-row">状态：<el-tag size="small" :type="orderStatusType(msg.order.status)">{{ orderStatusText(msg.order.status) }}</el-tag></div>
               <div v-if="msg.order.customerPhone" class="oc-row">手机号：{{ msg.order.customerPhone }}</div>
            </div>
         </div>
         <div v-if="chatLoading" class="chat-msg msg-ai">
            <div class="typing"><span></span><span></span><span></span></div>
         </div>
      </div>

      <!-- 快捷问题 -->
      <div v-if="showQuickQuestions" class="quick-row">
         <el-tag v-for="q in quickQuestions" :key="q" class="quick-tag"
            @click="sendQuick(q)">{{ q }}</el-tag>
      </div>

      <!-- 图片预览 -->
      <div v-if="pendingImageUrl" class="pending-preview">
         <img :src="pendingImageUrl" />
         <span class="pending-remove" @click="clearPendingImage">✕</span>
      </div>

      <!-- 输入区 -->
      <div class="chat-input-row">
         <input v-if="showImageUpload" ref="imageInputRef" type="file" hidden accept="image/*" @change="handleImageUpload" />
         <el-button v-if="showImageUpload" size="small" :disabled="chatLoading" @click="pickImage" circle
            :type="pendingImage ? 'warning' : 'default'" title="上传图片搜索">
            <el-icon><Picture /></el-icon>
         </el-button>
         <el-input ref="inputRef" v-model="inputText" placeholder="问AI或上传图片搜索..." :maxlength="300"
            @keyup.enter="sendMessage" size="small" />
         <el-button type="primary" size="small"
            :disabled="(!inputText.trim() && !pendingImage) || chatLoading" @click="sendMessage">发送</el-button>
      </div>
   </div>
</template>

<script setup>
import { Picture } from '@element-plus/icons-vue'
import { portalChatToolData, clearAiSession } from '@/api/portal/index'
import { getPortalSessionId } from '@/utils/portalSession'

// ==================== Props ====================
const props = defineProps({
   showImageUpload: { type: Boolean, default: false },
   showQuickQuestions: { type: Boolean, default: true },
   quickQuestions: {
      type: Array,
      default: () => ['有什么推荐？', '帮我找零食', '最便宜的是什么？']
   },
   showToolCards: { type: Boolean, default: true }
})

// ==================== Events ====================
const emit = defineEmits(['navigate', 'categoryChange'])

function onNavigate(type, id) {
   emit('navigate', { type, id })
}

// ==================== 状态 ====================
const sessionId = ref('')
const messages = ref([])
const inputText = ref('')
const chatLoading = ref(false)
const chatBodyRef = ref(null)
const imageInputRef = ref(null)
const inputRef = ref(null)
const pendingImage = ref(null)
const pendingImageUrl = ref('')

const CHAT_MSGS_KEY = 'cdc_chat_messages'

// ==================== 会话管理 ====================
function saveMsgs() {
   const slim = messages.value.map(m => ({
      role: m.role, content: m.content,
      products: m.products || [], categories: m.categories || [],
      cartItems: m.cartItems || [], order: m.order || null, orderItems: m.orderItems || []
   }))
   localStorage.setItem(CHAT_MSGS_KEY, JSON.stringify(slim))
}

function initSession() {
   try { const s = localStorage.getItem(CHAT_MSGS_KEY); if (s) messages.value = JSON.parse(s) } catch (e) { }
   sessionId.value = getPortalSessionId()
}

function clearChat() {
   messages.value = []
   localStorage.removeItem(CHAT_MSGS_KEY)
   // 会话 id 保持不变：购物车也挂在这个 id 下，换 id 会导致购物车"消失"
   // 服务端历史记录通过 clear-session 接口清掉，避免下次对话带入旧上下文
   clearAiSession(sessionId.value).catch(() => {})
}

// ==================== 流式对话 ====================
async function sendMessage() {
   const text = inputText.value.trim()
   const hasImage = !!pendingImage.value
   if ((!text && !hasImage) || chatLoading.value) return

   if (hasImage) {
      await uploadImageAndChat(text)
      return
   }

   messages.value.push({ role: 'user', content: text }); saveMsgs()
   inputText.value = ''
   chatLoading.value = true
   // 保持输入框焦点，让用户可以不中断地连续打字+回车
   setTimeout(() => {
      const el = inputRef.value?.$el || inputRef.value
      const inp = el?.querySelector?.('input') || el
      inp?.focus()
   }, 50)
   messages.value.push({ role: 'ai', content: '', products: [], categories: [] })
   const aiIdx = messages.value.length - 1
   scrollChat()

   try {
      const res = await fetch(`${import.meta.env.VITE_APP_BASE_API}/ai/chat/stream`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify({ sessionId: sessionId.value, message: text })
      })
      const reader = res.body.getReader()
      const dec = new TextDecoder()

      while (true) {
         const { done, value } = await reader.read()
         if (done) break
         const segment = dec.decode(value, { stream: true })
         // ★ 逐字追加（通过 messages.value[aiIdx] Proxy 触发 Vue 响应式）
         for (let i = 0; i < segment.length; i++) {
            messages.value[aiIdx].content += segment[i]
            if (i % 2 === 1) {
               scrollChat()
               await new Promise(r => setTimeout(r, 20))
            }
         }
      }
   } catch (e) {
      if (!messages.value[aiIdx].content) messages.value[aiIdx].content = '抱歉，网络连接失败'
   } finally {
      chatLoading.value = false
      // 轮询工具卡片数据
      try {
         const td = await portalChatToolData(sessionId.value)
         if (td.code === 200 && td.data) {
            const d = td.data
            if (d.products && d.products.length > 0) messages.value[aiIdx].products = d.products
            if (d.categories && d.categories.length > 0) messages.value[aiIdx].categories = d.categories
            if (d.cartItems && d.cartItems.length > 0) messages.value[aiIdx].cartItems = d.cartItems
            if (d.order) messages.value[aiIdx].order = d.order
            if (d.orderItems && d.orderItems.length > 0) messages.value[aiIdx].orderItems = d.orderItems
            if (d.toolCalled) messages.value[aiIdx].toolCalled = d.toolCalled
         }
      } catch (e) { /* 忽略 */ }
      saveMsgs()
      scrollChat()
   }
}

function sendQuick(q) { inputText.value = q; sendMessage() }

// ==================== 图片上传 ====================
function pickImage() { imageInputRef.value?.click() }

function handleImageUpload(e) {
   const file = e.target.files[0]
   if (!file) return
   pendingImage.value = file
   pendingImageUrl.value = URL.createObjectURL(file)
}

function clearPendingImage() {
   pendingImage.value = null
   URL.revokeObjectURL(pendingImageUrl.value)
   pendingImageUrl.value = ''
   if (imageInputRef.value) imageInputRef.value.value = ''
}

async function uploadImageAndChat(text) {
   const file = pendingImage.value
   const previewUrl = pendingImageUrl.value
   messages.value.push({
      role: 'user',
      content: text || '请识别图片中的商品',
      imageUrl: previewUrl
   })
   saveMsgs()
   inputText.value = ''
   clearPendingImage()
   chatLoading.value = true
   scrollChat()

   try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('message', text || '')
      formData.append('sessionId', sessionId.value)

      const res = await fetch(`${import.meta.env.VITE_APP_BASE_API}/ai/chat/image`, {
         method: 'POST',
         body: formData
      })
      const json = await res.json()

      if (json.code === 200 && json.data) {
         messages.value.push({
            role: 'ai',
            content: json.data.answer || '没能识别出来，换个角度试试？',
            products: json.data.products || [],
            imageDescription: json.data.imageDescription || ''
         })
      } else {
         messages.value.push({ role: 'ai', content: json.msg || '图片识别失败，请重试' })
      }
      saveMsgs()
      scrollChat()
   } catch (e) {
      messages.value.push({ role: 'ai', content: '抱歉，图片上传失败，请检查网络' })
      saveMsgs()
   } finally {
      chatLoading.value = false
   }
}

// ==================== 工具函数 ====================
function scrollChat() {
   nextTick(() => {
      if (chatBodyRef.value) chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
   })
}

function orderStatusText(status) {
   const map = { pending: '待处理', confirmed: '已确认', completed: '已完成', cancelled: '已取消' }
   return map[status] || status
}

function orderStatusType(status) {
   const map = { pending: 'warning', confirmed: '', completed: 'success', cancelled: 'danger' }
   return map[status] || ''
}

// ==================== 暴露方法 ====================
function getSessionId() {
   if (sessionId.value) return sessionId.value
   sessionId.value = getPortalSessionId()
   return sessionId.value
}

function send(text) { inputText.value = text; sendMessage() }

defineExpose({ getSessionId, send })

// ==================== 初始化 ====================
initSession()
</script>

<style scoped>
.chat-box { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.chat-title {
   font-size: 14px; font-weight: 600; color: #303133;
   padding: 10px 12px; border-bottom: 1px solid #ebeef5; flex-shrink: 0;
   display: flex; align-items: center; justify-content: space-between;
}

.chat-messages { flex: 1; overflow-y: auto; padding: 12px; }

.chat-msg { margin-bottom: 12px; }
.msg-user { text-align: right; }
.msg-user .msg-content {
   display: inline-block; background: #409EFF; color: #fff;
   padding: 8px 12px; border-radius: 10px 10px 2px 10px;
   max-width: 85%; font-size: 13px; text-align: left;
}
.msg-ai .msg-content {
   font-size: 13px; color: #303133; padding: 8px 12px;
   background: #f5f6fa; border-radius: 10px 10px 10px 2px;
   display: inline-block; max-width: 85%;
}

/* 商品卡片 */
.mini-cards { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.mini-card {
   background: #f8f9fc; border: 1px solid #e4e7ed; border-radius: 6px;
   padding: 6px 10px; cursor: pointer; font-size: 12px; min-width: 100px;
   transition: border-color 0.15s;
}
.mini-card:hover { border-color: #409EFF; }
.mc-name { font-weight: 600; }
.mc-row { display: flex; gap: 8px; margin-top: 2px; }
.mc-price { color: #e4393c; font-weight: 600; }
.mc-stock { color: #909399; }
.mini-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }

/* 购物车卡片 */
.cart-summary-card {
   background: #f0f9eb; border: 1px solid #c2e7b0; border-radius: 8px;
   padding: 10px 12px; margin-top: 6px; font-size: 12px;
}
.cs-title { font-weight: 600; color: #67c23a; margin-bottom: 6px; }
.cs-row { display: flex; justify-content: space-between; padding: 2px 0; }
.cs-name { flex: 1; }
.cs-qty { color: #909399; margin: 0 8px; }
.cs-price { color: #e4393c; font-weight: 600; }
.cs-total { text-align: right; font-weight: 700; color: #e4393c; margin: 6px 0; border-top: 1px dashed #c2e7b0; padding-top: 4px; }

/* 订单卡片 */
.order-card {
   background: #ecf5ff; border: 1px solid #b3d8ff; border-radius: 8px;
   padding: 10px 12px; margin-top: 6px; font-size: 12px;
}
.oc-title { font-weight: 600; color: #409EFF; margin-bottom: 4px; font-size: 14px; }
.oc-row { margin: 3px 0; color: #303133; }

/* 打字动画 */
.typing { display: flex; gap: 4px; padding: 8px 12px; }
.typing span { width: 6px; height: 6px; border-radius: 50%; background: #c0c4cc; animation: bounce 1.4s infinite ease-in-out both; }
.typing span:nth-child(1) { animation-delay: -0.32s; }
.typing span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%,80%,100% { transform: scale(0.6); opacity: 0.4; } 40% { transform: scale(1); opacity: 1; } }

/* 快捷问题 */
.quick-row { display: flex; flex-wrap: wrap; gap: 6px; justify-content: center; flex-shrink: 0; padding: 4px 12px; }
.quick-tag { cursor: pointer; font-size: 12px; }

/* 图片预览 */
.pending-preview { position: relative; display: inline-block; margin: 0 12px; }
.pending-preview img { width: 60px; height: 60px; object-fit: cover; border-radius: 6px; border: 2px solid #e6a23c; }
.pending-remove {
   position: absolute; top: -8px; right: -8px;
   width: 18px; height: 18px; border-radius: 50%; background: #f56c6c; color: #fff;
   font-size: 12px; display: flex; align-items: center; justify-content: center; cursor: pointer;
}

/* 用户图片消息 */
.msg-image { margin-top: 4px; }
.msg-image img { max-width: 120px; max-height: 120px; border-radius: 6px; object-fit: cover; }

/* 输入区 */
.chat-input-row {
   display: flex; gap: 6px; padding: 8px 12px; border-top: 1px solid #ebeef5; flex-shrink: 0;
}
.chat-input-row :deep(.el-input) { flex: 1; }
</style>
