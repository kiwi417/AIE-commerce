<template>
  <div class="portal-shell">
    <div class="shell-header">
      <div class="header-left">
        <el-link underline="never" @click="goBack">&#x2190; 返回首页</el-link>
        <span class="header-title">&#x1f4e6; 订单查询</span>
      </div>
      <div></div>
    </div>

    <div class="shell-body">
      <!-- 左侧：订单查询 -->
      <div class="left-panel">
        <el-card shadow="never" style="margin-bottom:16px">
          <el-form :inline="true">
            <el-form-item label="手机号">
              <el-input v-model="phone" placeholder="取货手机号" clearable @keyup.enter="handleSearch" style="width:180px" />
            </el-form-item>
            <el-form-item label="订单号">
              <el-input v-model="orderNo" placeholder="订单号" clearable @keyup.enter="handleSearch" style="width:200px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">查询</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-empty v-if="searched && orders.length === 0" description="未找到订单" />

        <el-card v-for="order in orders" :key="order.orderNo" shadow="hover" style="margin-bottom:12px;cursor:pointer" @click="showDetail(order)">
          <div style="display:flex;justify-content:space-between;align-items:center">
            <div>
              <div style="font-weight:500;margin-bottom:4px">{{ order.orderNo }}</div>
              <div style="color:#999;font-size:12px">{{ order.createTime }}</div>
            </div>
            <div style="text-align:center">
              <div style="color:#f56c6c;font-size:18px;font-weight:bold">&yen;{{ order.totalAmount }}</div>
              <div style="color:#999;font-size:12px">{{ order.itemCount }}件</div>
            </div>
            <div><el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag></div>
            <el-button link type="primary">详情 &#x2192;</el-button>
          </div>
        </el-card>

        <!-- 详情对话框 -->
        <el-dialog v-model="detailOpen" title="订单详情" width="600px">
          <template v-if="detail">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
              <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
              <el-descriptions-item label="手机号">{{ detail.customerPhone }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ detail.customerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="金额">&yen;{{ detail.totalAmount }}</el-descriptions-item>
              <el-descriptions-item label="件数">{{ detail.itemCount }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
              <el-descriptions-item label="下单时间" :span="2">{{ detail.createTime }}</el-descriptions-item>
            </el-descriptions>
            <h4 style="margin-top:16px">商品明细</h4>
            <el-table :data="detail.items" size="small">
              <el-table-column label="商品" prop="productName" />
              <el-table-column label="单价" width="80"><template #default="{row}">&yen;{{ row.price }}</template></el-table-column>
              <el-table-column label="数量" width="60" prop="quantity" />
              <el-table-column label="小计" width="80"><template #default="{row}">&yen;{{ row.subtotal }}</template></el-table-column>
            </el-table>
          </template>
        </el-dialog>
      </div>

      <!-- 右侧：AI对话 -->
      <div class="right-panel">
        <AiChatBox ref="chatBoxRef" :show-quick-questions="false"
           @navigate="onChatNavigate" />
      </div>
    </div>
  </div>
</template>

<script setup name="PortalOrderQuery">
import { queryPortalOrder, getPortalOrder } from "@/api/portal/index"
import AiChatBox from '@/components/AiChatBox/index.vue'

const { proxy } = getCurrentInstance()
const phone = ref('')
const orderNo = ref('')
const orders = ref([])
const searched = ref(false)
const detailOpen = ref(false)
const detail = ref(null)
const chatBoxRef = ref(null)
const router = useRouter()

function handleSearch() {
  searched.value = true
  queryPortalOrder({ phone: phone.value, orderNo: orderNo.value }).then(res => {
    const data = res.data
    orders.value = Array.isArray(data) ? data : (data && data.order ? [data.order] : [])
  })
}
function showDetail(order) {
  getPortalOrder(order.orderNo).then(res => {
    const data = res.data
    detail.value = { ...(data.order || data), items: data.items || [] }
    detailOpen.value = true
  })
}
function statusType(s) { return { pending: 'warning', confirmed: '', completed: 'success', cancelled: 'danger' }[s] || '' }
function statusLabel(s) { return { pending: '待取货', confirmed: '已确认', completed: '已完成', cancelled: '已取消' }[s] || s }

function onChatNavigate({ type, id }) {
  if (type === 'product') router.push({ path: '/portal/product/detail/' + id })
  else if (type === 'cart') router.push('/portal/cart')
  else if (type === 'order') router.push('/portal/order/query')
}

function goBack() { router.push('/portal/product') }
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
