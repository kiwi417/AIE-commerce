<template>
   <div class="chat-container">
      <!-- 顶部导航栏 -->
      <div class="chat-header">
         <div class="header-left">
            <el-link underline="never" @click="goBack" class="back-link">
               &#x2190; 商品浏览
            </el-link>
            <span class="header-title">&#x1f916; AI导购助手</span>
         </div>
         <div class="header-center"></div>
         <div class="header-right">
            <el-link underline="never" type="danger" @click="clearChat" class="clear-link">
               清空对话
            </el-link>
         </div>
      </div>

      <!-- 聊天消息区域 -->
      <div class="chat-body" ref="chatBodyRef">
         <!-- 欢迎状态 -->
         <div v-if="messages.length === 0" class="welcome-area">
            <div class="welcome-icon">&#x1f916;</div>
            <h2 class="welcome-title">你好，我是AI导购助手</h2>
            <p class="welcome-subtitle">我可以帮你查找商品、推荐好物、解答购物疑问</p>
            <div class="quick-questions">
               <el-tag
                  v-for="q in quickQuestions"
                  :key="q"
                  class="quick-tag"
                  @click="sendQuickQuestion(q)"
               >{{ q }}</el-tag>
            </div>
         </div>

         <!-- 消息列表 -->
         <div v-for="(msg, idx) in messages" :key="idx" class="message-wrapper">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" class="message-row user-row">
               <div class="message-bubble user-bubble">
                  <div class="message-text">{{ msg.content }}</div>
                  <div class="message-time">{{ msg.time }}</div>
               </div>
               <div class="message-avatar user-avatar">&#x1f464;</div>
            </div>

            <!-- AI消息 -->
            <div v-else class="message-row ai-row">
               <div class="message-avatar ai-avatar">&#x1f916;</div>
               <div class="message-bubble ai-bubble">
                  <div class="message-text">{{ msg.content }}</div>
                  <!-- 商品卡片 -->
                  <div v-if="msg.products && msg.products.length > 0" class="product-cards">
                     <div v-for="p in msg.products" :key="p.id" class="product-card"
                          @click="goProductDetail(p.id)">
                        <div class="card-name">{{ p.productName }}</div>
                        <div class="card-info">
                           <span class="card-price">&yen;{{ p.price }}</span>
                           <span class="card-stock">库存: {{ p.stock }}</span>
                        </div>
                        <div class="card-shelf" v-if="p.shelfArea">
                           &#x1f4cd; {{ p.shelfArea }}
                        </div>
                     </div>
                  </div>
                  <!-- 分类标签 -->
                  <div v-if="msg.categories && msg.categories.length > 0" class="category-tags">
                     <el-tag v-for="c in msg.categories" :key="c.id" class="cat-tag">
                        {{ c.categoryName }}
                     </el-tag>
                  </div>
                  <div class="message-time">{{ msg.time }}</div>
               </div>
            </div>
         </div>

         <!-- 加载中指示器 -->
         <div v-if="isLoading" class="message-row ai-row">
            <div class="message-avatar ai-avatar">&#x1f916;</div>
            <div class="message-bubble ai-bubble loading-bubble">
               <div class="typing-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
               </div>
            </div>
         </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-footer">
         <div class="input-area">
            <el-input
               v-model="inputText"
               placeholder="输入你的问题..."
               :maxlength="500"
               show-word-limit
               @keyup.enter="sendMessage"
               :disabled="isLoading"
               class="chat-input"
            >
               <template #append>
                  <el-button
                     type="primary"
                     :icon="Promotion"
                     @click="sendMessage"
                     :disabled="!inputText.trim() || isLoading"
                  >发送</el-button>
               </template>
            </el-input>
         </div>
         <div class="input-hint">
            输入商品相关问题，AI助手将为你智能解答
         </div>
      </div>
   </div>
</template>

<script setup>
import { Promotion } from "@element-plus/icons-vue"
import { portalChat, portalChatSession } from '@/api/portal/index'

const router = useRouter()
const route = useRoute()

// 聊天数据
const sessionId = ref('')
const messages = ref([])
const inputText = ref('')
const isLoading = ref(false)
const chatBodyRef = ref(null)

// 快捷问题
const quickQuestions = ['有什么商品推荐？', '帮我找一下零食在哪', '最新上架了什么？', '全场最便宜的是什么？']

// 初始化会话ID
function initSession() {
   portalChatSession().then(res => {
      if (res.code === 200 && res.data) {
         sessionId.value = res.data.sessionId
         // 如果有预设问题，自动发送
         if (route.query.q) {
            nextTick(() => {
               inputText.value = route.query.q
               sendMessage()
            })
         }
      }
   })
}

// 发送消息
function sendMessage() {
   const text = inputText.value.trim()
   if (!text || isLoading.value) return

   // 添加用户消息
   const userMsg = {
      role: 'user',
      content: text,
      time: formatTime(new Date())
   }
   messages.value.push(userMsg)
   inputText.value = ''
   isLoading.value = true
   scrollToBottom()

   // 调用API
   portalChat({
      sessionId: sessionId.value,
      question: text
   }).then(res => {
      if (res.code === 200 && res.data) {
         const aiMsg = {
            role: 'ai',
            content: res.data.answer || '抱歉，我暂时无法回答这个问题。',
            time: formatTime(new Date()),
            toolCalled: res.data.toolCalled,
            products: res.data.products || [],
            categories: res.data.categories || []
         }
         messages.value.push(aiMsg)
      } else {
         const aiMsg = {
            role: 'ai',
            content: '抱歉，服务出现异常，请稍后再试。',
            time: formatTime(new Date())
         }
         messages.value.push(aiMsg)
      }
   }).catch(() => {
      const aiMsg = {
         role: 'ai',
         content: '网络连接失败，请检查网络后重试。',
         time: formatTime(new Date())
      }
      messages.value.push(aiMsg)
   }).finally(() => {
      isLoading.value = false
      scrollToBottom()
   })
}

// 发送快捷问题
function sendQuickQuestion(question) {
   inputText.value = question
   sendMessage()
}

// 清空对话
function clearChat() {
   messages.value = []
   initSession()
}

// 跳转商品详情
function goProductDetail(productId) {
   router.push({ path: '/portal/product', query: { id: productId } })
}

// 返回商品浏览
function goBack() {
   router.push({
      path: '/portal/product'
   })
}

// 滚动到底部
function scrollToBottom() {
   nextTick(() => {
      if (chatBodyRef.value) {
         chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
      }
   })
}

// 格式化时间
function formatTime(date) {
   const h = String(date.getHours()).padStart(2, '0')
   const m = String(date.getMinutes()).padStart(2, '0')
   return h + ':' + m
}

onMounted(() => {
   initSession()
})
</script>

<style scoped>
.chat-container {
   display: flex;
   flex-direction: column;
   height: 100vh;
   max-width: 800px;
   margin: 0 auto;
   background: #fff;
   overflow: hidden;
}

/* 顶部导航 */
.chat-header {
   display: flex;
   align-items: center;
   justify-content: space-between;
   padding: 10px 16px;
   background: #fff;
   border-bottom: 1px solid #ebeef5;
   flex-shrink: 0;
   flex-wrap: wrap;
   gap: 8px;
}

.header-left {
   display: flex;
   align-items: center;
   gap: 12px;
}

.back-link {
   font-size: 14px;
   color: #409EFF;
}

.header-title {
   font-size: 17px;
   font-weight: 600;
   color: #303133;
   white-space: nowrap;
}

.header-center {
   display: flex;
   align-items: center;
}

.clear-link {
   font-size: 13px;
}

/* 聊天消息区域 */
.chat-body {
   flex: 1;
   overflow-y: auto;
   background: #f5f6fa;
   padding: 16px;
}

.chat-body::-webkit-scrollbar {
   width: 6px;
}

.chat-body::-webkit-scrollbar-thumb {
   background: #dcdfe6;
   border-radius: 3px;
}

/* 欢迎区 */
.welcome-area {
   display: flex;
   flex-direction: column;
   align-items: center;
   justify-content: center;
   padding-top: 80px;
   text-align: center;
}

.welcome-icon {
   font-size: 64px;
   margin-bottom: 16px;
}

.welcome-title {
   font-size: 22px;
   font-weight: 600;
   color: #303133;
   margin: 0 0 8px 0;
}

.welcome-subtitle {
   font-size: 14px;
   color: #909399;
   margin: 0 0 32px 0;
}

.quick-questions {
   display: flex;
   flex-wrap: wrap;
   gap: 10px;
   justify-content: center;
   max-width: 500px;
}

.quick-tag {
   cursor: pointer;
   font-size: 13px;
   padding: 6px 14px;
   border-radius: 20px;
   transition: all 0.2s;
}

.quick-tag:hover {
   background: #409EFF;
   color: #fff;
   border-color: #409EFF;
}

/* 消息行 */
.message-wrapper {
   margin-bottom: 16px;
}

.message-row {
   display: flex;
   gap: 10px;
   align-items: flex-start;
}

.user-row {
   justify-content: flex-end;
}

.ai-row {
   justify-content: flex-start;
}

/* 头像 */
.message-avatar {
   width: 36px;
   height: 36px;
   border-radius: 50%;
   display: flex;
   align-items: center;
   justify-content: center;
   font-size: 18px;
   flex-shrink: 0;
}

.user-avatar {
   background: #409EFF;
   order: 2;
}

.ai-avatar {
   background: #e6f0ff;
   order: 1;
}

/* 消息气泡 */
.message-bubble {
   max-width: 70%;
   padding: 10px 14px;
   border-radius: 12px;
   position: relative;
}

.user-bubble {
   background: #409EFF;
   color: #fff;
   border-bottom-right-radius: 4px;
   order: 1;
}

.ai-bubble {
   background: #fff;
   border: 1px solid #e4e7ed;
   border-bottom-left-radius: 4px;
   order: 2;
}

.message-text {
   font-size: 14px;
   line-height: 1.6;
   white-space: pre-wrap;
   word-break: break-word;
}

.message-time {
   font-size: 11px;
   margin-top: 4px;
   opacity: 0.7;
   text-align: right;
}

.user-bubble .message-time {
   color: rgba(255, 255, 255, 0.8);
}

.ai-bubble .message-time {
   color: #c0c4cc;
}

/* 加载动画 */
.loading-bubble {
   min-width: 60px;
   min-height: 36px;
   display: flex;
   align-items: center;
   justify-content: center;
}

.typing-dots {
   display: flex;
   gap: 4px;
   align-items: center;
}

.dot {
   width: 7px;
   height: 7px;
   border-radius: 50%;
   background: #c0c4cc;
   animation: dot-bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
   animation-delay: -0.32s;
}

.dot:nth-child(2) {
   animation-delay: -0.16s;
}

.dot:nth-child(3) {
   animation-delay: 0s;
}

@keyframes dot-bounce {
   0%, 80%, 100% {
      transform: scale(0.6);
      opacity: 0.4;
   }
   40% {
      transform: scale(1);
      opacity: 1;
   }
}

/* 输入区域 */
.chat-footer {
   padding: 12px 16px;
   background: #fff;
   border-top: 1px solid #ebeef5;
   flex-shrink: 0;
}

.input-area {
   display: flex;
   gap: 8px;
}

.chat-input {
   flex: 1;
}

.input-hint {
   font-size: 12px;
   color: #c0c4cc;
   text-align: center;
   margin-top: 6px;
}

/* 商品卡片 */
.product-cards {
   display: flex;
   flex-wrap: wrap;
   gap: 8px;
   margin-top: 10px;
}

.product-card {
   background: #f8f9fc;
   border: 1px solid #e4e7ed;
   border-radius: 8px;
   padding: 8px 12px;
   cursor: pointer;
   min-width: 150px;
   flex: 1;
   max-width: 220px;
   transition: all 0.2s;
}

.product-card:hover {
   border-color: #409EFF;
   background: #ecf5ff;
}

.card-name {
   font-size: 13px;
   font-weight: 600;
   color: #303133;
   margin-bottom: 4px;
}

.card-info {
   display: flex;
   gap: 8px;
   align-items: center;
   font-size: 12px;
}

.card-price {
   color: #e4393c;
   font-weight: 600;
}

.card-stock {
   color: #909399;
}

.card-shelf {
   font-size: 11px;
   color: #67c23a;
   margin-top: 4px;
}

/* 分类标签 */
.category-tags {
   display: flex;
   flex-wrap: wrap;
   gap: 6px;
   margin-top: 10px;
}

.cat-tag {
   cursor: pointer;
}

/* 响应式 */
@media (max-width: 768px) {
   .chat-container {
      max-width: 100%;
   }

   .chat-header {
      padding: 8px 12px;
   }

   .header-left {
      gap: 8px;
   }

   .header-title {
      font-size: 15px;
   }

   .chat-body {
      padding: 12px;
   }

   .message-bubble {
      max-width: 85%;
   }

   .welcome-icon {
      font-size: 48px;
   }

   .welcome-title {
      font-size: 18px;
   }

   .welcome-area {
      padding-top: 40px;
   }

   .chat-footer {
      padding: 8px 12px;
   }
}
</style>
