<template>
  <div class="portal-shell">
    <div class="shell-header">
      <div class="header-left">
        <el-link underline="never" @click="goBack">&#x2190; 返回购物车</el-link>
        <span class="header-title">&#x1f4cb; 确认订单</span>
      </div>
      <div></div>
    </div>

    <div class="shell-body">
      <!-- 左侧：订单内容 -->
      <div class="left-panel">
        <!-- 订单商品 -->
        <el-card shadow="never" style="margin-bottom:16px">
          <template #header>订单商品</template>
          <el-table :data="cartItems" size="small">
            <el-table-column label="商品" prop="productName" />
            <el-table-column label="单价" width="100" align="center"><template #default="{row}">&yen;{{ row.price }}</template></el-table-column>
            <el-table-column label="数量" width="80" align="center" prop="quantity" />
            <el-table-column label="小计" width="100" align="center"><template #default="{row}">&yen;{{ (row.price * row.quantity).toFixed(2) }}</template></el-table-column>
          </el-table>
          <div style="text-align:right;padding:12px;font-size:16px">合计：<b style="color:#f56c6c;font-size:22px">&yen;{{ totalAmount.toFixed(2) }}</b></div>
        </el-card>

        <!-- 取货信息 -->
        <el-card shadow="never" v-if="!submitted">
          <template #header>取货信息</template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:500px">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="取货核验用" maxlength="11" />
            </el-form-item>
            <el-form-item label="姓名" prop="customerName">
              <el-input v-model="form.customerName" placeholder="选填" maxlength="64" />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" placeholder="选填" maxlength="256" rows="2" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" @click="submitOrder" :loading="submitting">提交订单</el-button>
              <el-button @click="goBack">返回修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 下单成功 -->
        <el-card shadow="never" v-if="submitted">
          <div style="text-align:center;padding:40px">
            <div style="font-size:48px;margin-bottom:16px">&#x2705;</div>
            <h3>订单提交成功！</h3>
            <p>订单号：<b>{{ result.orderNo }}</b></p>
            <p>取货手机：{{ result.phoneMasked }}</p>
            <p>订单金额：<b style="color:#f56c6c">&yen;{{ result.totalAmount }}</b></p>
            <p style="color:#999">请凭手机号到店取货</p>
            <div style="margin-top:24px">
              <el-button type="primary" @click="goProduct">继续购物</el-button>
              <el-button @click="goQuery">查看订单</el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧：AI对话 -->
      <div class="right-panel">
        <AiChatBox ref="chatBoxRef" :show-quick-questions="false"
           @navigate="onChatNavigate" />
      </div>
    </div>
  </div>
</template>

<script setup name="PortalOrderConfirm">
import { getCartList, createPortalOrder } from "@/api/portal/index"
import { getPortalSessionId } from '@/utils/portalSession'
import AiChatBox from '@/components/AiChatBox/index.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()
const cartItems = ref([])
const submitting = ref(false)
const submitted = ref(false)
const result = ref({})
const chatBoxRef = ref(null)

const form = reactive({ phone: '', customerName: '', remark: '' })
const rules = { phone: [{ required: true, message: '请输入取货手机号', trigger: 'blur' }, { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }] }
const totalAmount = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0))

function getSessionId() {
  return getPortalSessionId()
}
function loadCart() {
  getCartList(getSessionId()).then(res => {
    cartItems.value = (res.data && res.data.cartItems) ? res.data.cartItems : []
    if (cartItems.value.length === 0) router.push('/portal/cart')
  })
}
function submitOrder() {
  proxy.$refs['formRef'].validate(valid => {
    if (!valid) return
    submitting.value = true
    const items = cartItems.value.map(i => ({ productId: i.productId, quantity: i.quantity }))
    createPortalOrder({ sessionId: getSessionId(), phone: form.phone, customerName: form.customerName, items, remark: form.remark })
      .then(res => { submitted.value = true; result.value = res.data; submitting.value = false })
      .catch(() => { submitting.value = false })
  })
}

function onChatNavigate({ type, id }) {
  if (type === 'product') router.push({ path: '/portal/product/detail/' + id })
  else if (type === 'cart') router.push('/portal/cart')
  else if (type === 'order') router.push('/portal/order/query')
}

function goBack() { router.push('/portal/cart') }
function goProduct() { router.push('/portal/product') }
function goQuery() { router.push('/portal/order/query') }

// 与购物车页同理：setup 阶段 ref 未挂载，必须在 onMounted 加载
onMounted(loadCart)
</script>

<style scoped>
.portal-shell { height: 100vh; display: flex; flex-direction: column; background: #f5f6fa; }
.shell-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 20px; background: #fff; border-bottom: 1px solid #ebeef5; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 16px; }
.header-title { font-size: 16px; font-weight: 600; }
.shell-body { flex: 1; display: flex; overflow: hidden; }
.left-panel { flex: 3; overflow-y: auto; padding: 16px; }
.right-panel { flex: 1; display: flex; flex-direction: column; border-left: 1px solid #e4e7ed; background: #fff; min-width: 300px; }

@media (max-width: 768px) {
  .shell-body { flex-direction: column; }
  .right-panel { border-left: none; border-top: 1px solid #e4e7ed; min-width: unset; height: 35vh; }
}
</style>
