<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
         <el-form-item label="订单号" prop="orderNo">
            <el-input
               v-model="queryParams.orderNo"
               placeholder="请输入订单号"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="客户手机号" prop="customerPhone">
            <el-input
               v-model="queryParams.customerPhone"
               placeholder="请输入客户手机号"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="请选择状态"
               clearable
               style="width: 200px"
            >
               <el-option
                  v-for="item in orderStatus"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="orderList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="订单号" align="center" prop="orderNo" width="150" :show-overflow-tooltip="true" />
         <el-table-column label="客户手机号" align="center" prop="customerPhone" width="120" />
         <el-table-column label="客户姓名" align="center" prop="customerName" width="100" />
         <el-table-column label="订单金额" align="center" prop="totalAmount" width="100" />
         <el-table-column label="商品数量" align="center" prop="itemCount" width="80" />
         <el-table-column label="状态" align="center" prop="status" width="90">
            <template #default="scope">
               <el-tag :type="orderTagType(scope.row.status)">{{ orderLabel(scope.row.status) }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="160">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="View" @click="handleDetail(scope.row)">详情</el-button>
               <el-button
                  v-if="scope.row.status === 'pending'"
                  link
                  type="success"
                  icon="Check"
                  @click="handleComplete(scope.row)"
                  v-hasPermi="['business:order:complete']"
               >完成</el-button>
               <el-button
                  v-if="scope.row.status === 'pending'"
                  link
                  type="danger"
                  icon="Close"
                  @click="handleCancel(scope.row)"
                  v-hasPermi="['business:order:cancel']"
               >取消</el-button>
            </template>
         </el-table-column>
      </el-table>

      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />

      <!-- 订单详情对话框 -->
      <el-dialog title="订单详情" v-model="detailOpen" width="700px" append-to-body>
         <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ detailForm.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="客户姓名">{{ detailForm.customerName }}</el-descriptions-item>
            <el-descriptions-item label="客户手机号">{{ detailForm.customerPhone }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">{{ detailForm.totalAmount }}</el-descriptions-item>
            <el-descriptions-item label="商品数量">{{ detailForm.itemCount }}</el-descriptions-item>
            <el-descriptions-item label="状态">
               <el-tag :type="orderTagType(detailForm.status)">{{ orderLabel(detailForm.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ parseTime(detailForm.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detailForm.remark || '-' }}</el-descriptions-item>
         </el-descriptions>

         <div style="margin-top: 20px;">
            <h4>订单商品</h4>
         </div>
         <el-table :data="detailForm.orderItems" border style="margin-top: 10px;">
            <el-table-column label="商品名称" align="center" prop="productName" />
            <el-table-column label="单价" align="center" prop="price" width="100" />
            <el-table-column label="数量" align="center" prop="quantity" width="80" />
            <el-table-column label="货架区域" align="center" prop="shelfArea" width="120" />
            <el-table-column label="小计" align="center" width="100">
               <template #default="scope">
                  <span>{{ ((scope.row.price || 0) * (scope.row.quantity || 0)).toFixed(2) }}</span>
               </template>
            </el-table-column>
         </el-table>

         <template #footer>
            <div class="dialog-footer">
               <el-button @click="detailOpen = false">关 闭</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Order">
import { listOrder, getOrder, completeOrder, cancelOrder } from "@/api/business/order"

const { proxy } = getCurrentInstance()

const orderList = ref([])
const detailOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const total = ref(0)
const detailForm = ref({})

const orderStatus = ref([
  { label: "待取货", value: "pending" },
  { label: "已完成", value: "completed" },
  { label: "已取消", value: "cancelled" }
])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    customerPhone: undefined,
    status: undefined
  }
})

const { queryParams } = toRefs(data)

/** 订单状态标签类型 */
function orderTagType(status) {
  const map = { pending: "warning", completed: "success", cancelled: "danger" }
  return map[status] || ""
}

/** 订单状态标签文本 */
function orderLabel(status) {
  const item = orderStatus.value.find(s => s.value === status)
  return item ? item.label : status
}

/** 查询订单列表 */
function getList() {
  loading.value = true
  listOrder(queryParams.value).then(response => {
    orderList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
}

/** 详情按钮操作 */
function handleDetail(row) {
  const id = row.id
  getOrder(id).then(response => {
    detailForm.value = response.data
    if (!detailForm.value.orderItems) {
      detailForm.value.orderItems = []
    }
    detailOpen.value = true
  })
}

/** 完成订单 */
function handleComplete(row) {
  proxy.$modal.confirm('是否确认完成订单"' + row.orderNo + '"？').then(function() {
    return completeOrder(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("订单已完成")
  }).catch(() => {})
}

/** 取消订单 */
function handleCancel(row) {
  proxy.$modal.confirm('是否确认取消订单"' + row.orderNo + '"？').then(function() {
    return cancelOrder(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("订单已取消")
  }).catch(() => {})
}

getList()
</script>
