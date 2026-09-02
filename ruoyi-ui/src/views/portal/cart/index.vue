<template>
  <div class="portal-shell">
    <div class="shell-header">
      <div class="header-left">
        <el-link underline="never" @click="goBack">&#x2190; 继续购物</el-link>
        <span class="header-title">&#x1f6d2; 购物车</span>
      </div>
      <div class="header-actions">
        <el-button type="danger" text @click="handleClear" v-if="cartItems.length">清空</el-button>
      </div>
    </div>

    <div class="shell-body">
      <!-- 左侧：购物车 -->
      <div class="left-panel">
        <div v-if="cartItems.length === 0" style="text-align:center;padding:80px 0">
          <el-empty description="购物车是空的"><el-button type="primary" @click="goBack">去逛逛</el-button></el-empty>
        </div>
        <div v-else>
          <el-table :data="cartItems" style="width:100%">
            <el-table-column label="商品" min-width="160">
              <template #default="{row}"><div style="font-weight:500">{{ row.productName }}</div><div style="color:#999;font-size:12px">{{ row.shelfArea }}</div></template>
            </el-table-column>
            <el-table-column label="单价" width="80" align="center"><template #default="{row}">&yen;{{ row.price }}</template></el-table-column>
            <el-table-column label="数量" width="120" align="center">
              <template #default="{row}"><el-input-number v-model="row.quantity" :min="1" :max="99" size="small" @change="handleUpdate(row)" /></template>
            </el-table-column>
            <el-table-column label="小计" width="90" align="center"><template #default="{row}">&yen;{{ (row.price * row.quantity).toFixed(2) }}</template></el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{row}"><el-button link type="danger" @click="handleRemove(row)">删除</el-button></template>
            </el-table-column>
          </el-table>
          <div style="text-align:right;padding:20px 0;font-size:16px">
            共 <b>{{ totalCount }}</b> 件，合计：<b style="color:#f56c6c;font-size:22px">&yen;{{ totalAmount.toFixed(2) }}</b>
          </div>
          <div style="text-align:right">
            <el-button type="primary" size="large" @click="goCheckout">去下单</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：AI对话 -->
      <div class="right-panel">
        <AiChatBox ref="chatBoxRef" :quick-questions="['有什么推荐？', '帮我对比一下', '还有什么优惠？']"
           @navigate="onChatNavigate" />
      </div>
    </div>
  </div>
</template>

<script setup name="PortalCart">
import { getCartList, updateCartItem, removeCartItem, clearCart } from "@/api/portal/index"
import { getPortalSessionId } from '@/utils/portalSession'
import AiChatBox from '@/components/AiChatBox/index.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const cartItems = ref([])
const chatBoxRef = ref(null)

const totalCount = computed(() => cartItems.value.reduce((s, i) => s + i.quantity, 0))
const totalAmount = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

function getSessionId() {
  return getPortalSessionId()
}
function loadCart() {
  getCartList(getSessionId()).then(res => { cartItems.value = (res.data && res.data.cartItems) ? res.data.cartItems : [] })
}

let updateTimer = null
function handleUpdate(row) {
  if (updateTimer) clearTimeout(updateTimer)
  updateTimer = setTimeout(() => {
    updateCartItem({ sessionId: getSessionId(), productId: row.productId, quantity: row.quantity }).then(loadCart)
  }, 300)
}
function handleRemove(row) {
  proxy.$modal.confirm('确定删除该商品？').then(() => {
    removeCartItem({ sessionId: getSessionId(), productId: row.productId }).then(loadCart)
  }).catch(() => {})
}
function handleClear() {
  proxy.$modal.confirm('确定清空购物车？').then(() => {
    clearCart(getSessionId()).then(loadCart)
  }).catch(() => {})
}

function onChatNavigate({ type, id }) {
  if (type === 'product') router.push({ path: '/portal/product/detail/' + id })
  else if (type === 'cart') router.push('/portal/cart')
  else if (type === 'order') router.push('/portal/order/query')
}

function goBack() { router.push('/portal/product') }
function goCheckout() { router.push('/portal/order/confirm') }

// 注意：必须在 onMounted 里加载——setup 阶段 AiChatBox 尚未挂载（历史 bug：拿到空 sessionId 导致购物车永远为空）
onMounted(loadCart)
</script>

<style scoped>
.portal-shell { height: 100vh; display: flex; flex-direction: column; background: #f5f6fa; }
.shell-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 20px; background: #fff; border-bottom: 1px solid #ebeef5; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 16px; }
.header-title { font-size: 16px; font-weight: 600; }
.header-actions { display: flex; gap: 10px; }
.shell-body { flex: 1; display: flex; overflow: hidden; }
.left-panel { flex: 3; overflow-y: auto; padding: 16px; }
.right-panel { flex: 1; display: flex; flex-direction: column; border-left: 1px solid #e4e7ed; background: #fff; min-width: 300px; }

@media (max-width: 768px) {
  .shell-body { flex-direction: column; }
  .right-panel { border-left: none; border-top: 1px solid #e4e7ed; min-width: unset; height: 35vh; }
}
</style>
