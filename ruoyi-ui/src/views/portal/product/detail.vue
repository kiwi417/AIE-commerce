<template>
   <div class="portal-shell">
      <!-- 顶部导航 -->
      <div class="shell-header">
         <div class="header-left">
            <el-link underline="never" @click="goBack" class="back-link">
               &#x2190; 返回商品列表
            </el-link>
            <span class="header-title" v-if="product">{{ product.productName }}</span>
         </div>
         <div class="header-actions">
            <el-badge :value="cartCount" :hidden="cartCount === 0">
               <el-button size="small" @click="goToCart">&#x1f6d2; 购物车</el-button>
            </el-badge>
         </div>
      </div>

      <!-- 主体：左详情 + 右对话，3:1 -->
      <div class="shell-body">
         <!-- ==================== 左侧：商品详情 ==================== -->
         <div class="left-panel" v-loading="loading">
            <template v-if="product">
               <!-- 基本信息 -->
               <div class="detail-card">
                  <div class="detail-row">
                     <div class="detail-img-box">
                        <img v-if="product.imageUrl" :src="product.imageUrl" :alt="product.productName" />
                        <div v-else class="img-placeholder">{{ product.productName.charAt(0) }}</div>
                     </div>
                     <div class="detail-info">
                        <h2 class="p-title">{{ product.productName }}</h2>
                        <div class="p-price-row">
                           <span class="p-price">&yen;{{ product.price ? product.price.toFixed(2) : '0.00' }}</span>
                           <el-tag size="small">{{ product.stock !== null ? '库存 '+product.stock+(product.unit||'') : '无库存' }}</el-tag>
                        </div>
                        <el-divider />
                        <el-descriptions :column="1" border size="small">
                           <el-descriptions-item label="条码">{{ product.barcode || '-' }}</el-descriptions-item>
                           <el-descriptions-item label="货架">{{ product.shelfArea || '-' }}</el-descriptions-item>
                           <el-descriptions-item label="分类">{{ product.categoryId || '-' }}</el-descriptions-item>
                        </el-descriptions>
                        <div class="action-row">
                           <el-input-number v-model="buyQty" :min="1" :max="99" size="default" style="width:100px" />
                           <el-button type="warning" :loading="adding" :disabled="adding" @click="handleAddCart">&#x1f6d2; 加入购物车</el-button>
                           <el-button type="primary" @click="askAI">&#x1f4ac; 问问AI</el-button>
                        </div>
                     </div>
                  </div>
               </div>
               <!-- 商品描述 -->
               <div class="desc-card" v-if="product.description">
                  <div class="desc-label">商品描述</div>
                  <div class="desc-text">{{ product.description }}</div>
               </div>
            </template>
            <el-empty v-else-if="!loading" description="商品不存在" />
         </div>

         <!-- ==================== 右侧：AI对话 ==================== -->
         <div class="right-panel">
            <AiChatBox ref="chatBoxRef" :quick-questions="['这个商品怎么样？', '有类似的推荐吗？', '有什么优惠吗？']"
               @navigate="onChatNavigate" />
         </div>
      </div>
   </div>
</template>

<script setup>
import { getPortalProduct, addToCart, getCartList } from '@/api/portal/index'
import { getPortalSessionId } from '@/utils/portalSession'
import AiChatBox from '@/components/AiChatBox/index.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
let productId = route.params.id

// ==================== 商品详情 ====================
const product = ref(null)
const buyQty = ref(1)
const loading = ref(false)
const adding = ref(false)
const cartCount = ref(0)

function loadDetail() {
   if (!productId) return
   loading.value = true
   getPortalProduct(productId).then(res => { if (res.code === 200) product.value = res.data })
    .finally(() => { loading.value = false })
}
function goBack() { router.push({ path: '/portal/product' }) }
function goToCart() { router.push('/portal/cart') }
function getSessionId() {
   return getPortalSessionId()
}
function loadCartCount() {
   getCartList(getSessionId()).then(res => {
      const items = res.data && res.data.cartItems ? res.data.cartItems : []
      cartCount.value = items.reduce((s, i) => s + i.quantity, 0)
   })
}
function handleAddCart() {
   if (!product.value || adding.value) return
   adding.value = true
   addToCart({ sessionId: getSessionId(), productId: product.value.id, quantity: buyQty.value }).then(() => {
      proxy.$modal.msgSuccess('已加入购物车'); loadCartCount()
   }).finally(() => { adding.value = false })
}

// ==================== AI对话 ====================
const chatBoxRef = ref(null)

function askAI() {
   const name = product.value?.productName || '这个商品'
   chatBoxRef.value?.send('帮我看看 "' + name + '" 怎么样？')
}

function onChatNavigate({ type, id }) {
   if (type === 'product') router.push({ path: '/portal/product/detail/' + id })
   else if (type === 'cart') router.push('/portal/cart')
   else if (type === 'order') router.push('/portal/order/query')
}

onMounted(() => { loadDetail(); loadCartCount() })

// 监听路由参数变化（同一组件跳转时，Vue复用组件，需手动重新加载）
watch(() => route.params.id, (newId) => {
   if (newId) { productId = newId; loadDetail() }
})
</script>

<style scoped>
.portal-shell { height: 100vh; display: flex; flex-direction: column; background: #f5f6fa; }

.shell-header {
   display: flex; align-items: center; justify-content: space-between;
   padding: 10px 20px; background: #fff; border-bottom: 1px solid #ebeef5; flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 16px; }
.back-link { font-size: 14px; color: #409EFF; }
.header-title { font-size: 16px; font-weight: 600; color: #303133; max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.header-actions { display: flex; gap: 10px; }

.shell-body { flex: 1; display: flex; overflow: hidden; }

/* ===== 左侧详情 ===== */
.left-panel { flex: 3; overflow-y: auto; padding: 16px; }
.detail-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.detail-row { display: flex; gap: 24px; flex-wrap: wrap; }
.detail-img-box { width: 240px; height: 240px; flex-shrink: 0; background: #f5f7fa; border-radius: 8px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.detail-img-box img { width: 100%; height: 100%; object-fit: cover; }
.img-placeholder { font-size: 72px; font-weight: 700; color: #c0c4cc; }
.detail-info { flex: 1; min-width: 240px; }
.p-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0 0 12px 0; }
.p-price-row { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.p-price { font-size: 24px; font-weight: 700; color: #f56c6c; }
.action-row { display: flex; align-items: center; gap: 10px; margin-top: 16px; flex-wrap: wrap; }
.desc-card { background: #fff; border-radius: 8px; padding: 16px 20px; margin-top: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.desc-label { font-size: 15px; font-weight: 600; margin-bottom: 8px; }
.desc-text { font-size: 14px; color: #606266; line-height: 1.8; white-space: pre-wrap; }

/* ===== 右侧AI面板 ===== */
.right-panel { flex: 1; display: flex; flex-direction: column; border-left: 1px solid #e4e7ed; background: #fff; min-width: 300px; }

@media (max-width: 768px) {
   .shell-body { flex-direction: column; }
   .right-panel { border-left: none; border-top: 1px solid #e4e7ed; min-width: unset; height: 35vh; }
   .detail-img-box { width: 100%; height: 200px; }
}
</style>
