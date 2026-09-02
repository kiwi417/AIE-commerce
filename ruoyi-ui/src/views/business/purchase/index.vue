<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
         <el-form-item label="采购单号" prop="orderNo">
            <el-input
               v-model="queryParams.orderNo"
               placeholder="请输入采购单号"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="供应商" prop="supplierId">
            <el-select
               v-model="queryParams.supplierId"
               placeholder="请选择供应商"
               clearable
               style="width: 200px"
            >
               <el-option
                  v-for="item in supplierOptions"
                  :key="item.id"
                  :label="item.supplierName"
                  :value="item.id"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="请选择状态"
               clearable
               style="width: 200px"
            >
               <el-option
                  v-for="item in purchaseStatus"
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
         <el-col :span="1.5">
            <el-button
               type="primary"
               plain
               icon="Plus"
               @click="handleAdd"
               v-hasPermi="['business:purchase:add']"
            >新增采购单</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="purchaseList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="采购单号" align="center" prop="orderNo" width="140" :show-overflow-tooltip="true" />
         <el-table-column label="供应商" align="center" prop="supplierName" :show-overflow-tooltip="true">
            <template #default="scope">
               <span>{{ supplierMap[scope.row.supplierId] || scope.row.supplierName || '-' }}</span>
            </template>
         </el-table-column>
         <el-table-column label="采购金额" align="center" prop="totalAmount" width="100" />
         <el-table-column label="状态" align="center" prop="status" width="90">
            <template #default="scope">
               <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="采购日期" align="center" prop="orderDate" width="110">
            <template #default="scope">
               <span>{{ parseTime(scope.row.orderDate, '{y}-{m}-{d}') }}</span>
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="160">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
            <template #default="scope">
               <template v-if="scope.row.status === 'draft'">
                  <el-button link type="primary" icon="Check" @click="handleConfirm(scope.row)" v-hasPermi="['business:purchase:confirm']">确认</el-button>
                  <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['business:purchase:edit']">修改</el-button>
                  <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['business:purchase:remove']">删除</el-button>
               </template>
               <template v-if="scope.row.status === 'confirmed'">
                  <el-button link type="primary" icon="Box" @click="handleReceive(scope.row)" v-hasPermi="['business:purchase:receive']">收货</el-button>
                  <el-button link type="primary" icon="Close" @click="handleCancel(scope.row)" v-hasPermi="['business:purchase:cancel']">取消</el-button>
               </template>
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

      <!-- 新增/修改采购单对话框 -->
      <el-dialog :title="title" v-model="open" width="800px" append-to-body>
         <el-form ref="purchaseRef" :model="form" :rules="rules" label-width="100px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="供应商" prop="supplierId">
                     <el-select
                        v-model="form.supplierId"
                        placeholder="请选择供应商"
                        style="width: 100%"
                     >
                        <el-option
                           v-for="item in supplierOptions"
                           :key="item.id"
                           :label="item.supplierName"
                           :value="item.id"
                        />
                     </el-select>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="采购日期" prop="orderDate">
                     <el-date-picker
                        v-model="form.orderDate"
                        type="date"
                        placeholder="选择日期"
                        value-format="YYYY-MM-DD"
                        style="width: 100%"
                     />
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>

         <div style="margin-bottom: 10px;">
            <el-button type="primary" plain icon="Plus" @click="addItem">添加商品</el-button>
         </div>

         <el-table :data="form.purchaseItems" border>
            <el-table-column label="商品" align="center" min-width="160">
               <template #default="scope">
                  <el-select
                     v-model="scope.row.productId"
                     placeholder="请选择商品"
                     style="width: 100%"
                     @change="onProductChange(scope.row)"
                  >
                     <el-option
                        v-for="p in productOptions"
                        :key="p.id"
                        :label="p.productName"
                        :value="p.id"
                     />
                  </el-select>
               </template>
            </el-table-column>
            <el-table-column label="采购单价" align="center" width="130">
               <template #default="scope">
                  <el-input-number
                     v-model="scope.row.price"
                     :precision="2"
                     :min="0"
                     style="width: 100%"
                     @change="calcSubtotal(scope.row)"
                  />
               </template>
            </el-table-column>
            <el-table-column label="数量" align="center" width="100">
               <template #default="scope">
                  <el-input-number
                     v-model="scope.row.quantity"
                     :min="1"
                     style="width: 100%"
                     @change="calcSubtotal(scope.row)"
                  />
               </template>
            </el-table-column>
            <el-table-column label="小计" align="center" width="120">
               <template #default="scope">
                  <span>{{ (scope.row.price * scope.row.quantity).toFixed(2) }}</span>
               </template>
            </el-table-column>
            <el-table-column label="操作" align="center" width="80">
               <template #default="scope">
                  <el-button link type="danger" icon="Delete" @click="removeItem(scope.$index)">删除</el-button>
               </template>
            </el-table-column>
         </el-table>

         <div style="text-align: right; margin-top: 10px; font-size: 14px; font-weight: bold;">
            <span>合计金额：</span>
            <span style="color: #f56c6c;">{{ computedTotal.toFixed(2) }}</span>
         </div>

         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Purchase">
import { listPurchase, getPurchase, addPurchase, updatePurchase, delPurchase, confirmPurchase, receivePurchase, cancelPurchase } from "@/api/business/purchase"
import { listSupplier } from "@/api/business/supplier"
import { listProduct } from "@/api/business/product"

const { proxy } = getCurrentInstance()

const purchaseList = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const supplierOptions = ref([])
const productOptions = ref([])
const supplierMap = ref({})

const purchaseStatus = ref([
  { label: "草稿", value: "draft" },
  { label: "已确认", value: "confirmed" },
  { label: "已收货", value: "received" },
  { label: "已取消", value: "cancelled" }
])

const data = reactive({
  form: {
    purchaseItems: []
  },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    supplierId: undefined,
    status: undefined
  },
  rules: {}
})

const { queryParams, form, rules } = toRefs(data)

const computedTotal = computed(() => {
  if (!form.value.purchaseItems || form.value.purchaseItems.length === 0) return 0
  return form.value.purchaseItems.reduce((sum, item) => {
    const price = parseFloat(item.price) || 0
    const qty = parseInt(item.quantity) || 0
    return sum + price * qty
  }, 0)
})

/** 状态标签类型映射 */
function statusTagType(status) {
  const map = { draft: "", confirmed: "warning", received: "success", cancelled: "danger" }
  return map[status] || ""
}

/** 状态标签文本 */
function statusLabel(status) {
  const item = purchaseStatus.value.find(s => s.value === status)
  return item ? item.label : status
}

/** 加载供应商选项并构建映射 */
function loadSuppliers() {
  listSupplier({ pageNum: 1, pageSize: 999 }).then(response => {
    supplierOptions.value = response.rows
    const map = {}
    response.rows.forEach(s => {
      map[s.id] = s.supplierName
    })
    supplierMap.value = map
  })
}

/** 加载商品选项 */
function loadProducts() {
  listProduct({ pageNum: 1, pageSize: 9999 }).then(response => {
    productOptions.value = response.rows
  })
}

/** 查询采购单列表 */
function getList() {
  loading.value = true
  listPurchase(queryParams.value).then(response => {
    purchaseList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    supplierId: undefined,
    orderDate: undefined,
    purchaseItems: []
  }
  if (proxy.$refs["purchaseRef"]) {
    proxy.resetForm("purchaseRef")
  }
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

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "新增采购单"
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row.id || ids.value
  getPurchase(id).then(response => {
    form.value = response.data
    if (!form.value.purchaseItems) {
      form.value.purchaseItems = []
    }
    open.value = true
    title.value = "修改采购单"
  })
}

/** 添加商品行 */
function addItem() {
  if (!form.value.purchaseItems) {
    form.value.purchaseItems = []
  }
  form.value.purchaseItems.push({
    productId: undefined,
    productName: undefined,
    price: 0,
    quantity: 1
  })
}

/** 移除商品行 */
function removeItem(index) {
  form.value.purchaseItems.splice(index, 1)
}

/** 商品切换时自动填充名称 */
function onProductChange(row) {
  const product = productOptions.value.find(p => p.id === row.productId)
  if (product) {
    row.productName = product.productName
  }
}

/** 计算小计 */
function calcSubtotal(row) { /* 由computed自动计算，无需额外逻辑 */ }

/** 提交按钮 */
function submitForm() {
  if (!form.value.supplierId) {
    proxy.$modal.msgError("请选择供应商")
    return
  }
  if (!form.value.purchaseItems || form.value.purchaseItems.length === 0) {
    proxy.$modal.msgError("请至少添加一个采购商品")
    return
  }
  form.value.totalAmount = computedTotal.value
  if (form.value.id != undefined) {
    updatePurchase(form.value).then(response => {
      proxy.$modal.msgSuccess("修改成功")
      open.value = false
      getList()
    })
  } else {
    addPurchase(form.value).then(response => {
      proxy.$modal.msgSuccess("新增成功")
      open.value = false
      getList()
    })
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  const purchaseIds = row.id || ids.value
  proxy.$modal.confirm('是否确认删除采购单编号为"' + (row.orderNo || purchaseIds) + '"的数据项？').then(function() {
    return delPurchase(purchaseIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 确认采购单 */
function handleConfirm(row) {
  proxy.$modal.confirm('是否确认采购单"' + row.orderNo + '"？确认后将无法修改。').then(function() {
    return confirmPurchase(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("确认成功")
  }).catch(() => {})
}

/** 收货操作 */
function handleReceive(row) {
  proxy.$modal.confirm('是否确认收货采购单"' + row.orderNo + '"？收货后将更新库存。').then(function() {
    return receivePurchase(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("收货成功")
  }).catch(() => {})
}

/** 取消采购单 */
function handleCancel(row) {
  proxy.$modal.confirm('是否确认取消采购单"' + row.orderNo + '"？').then(function() {
    return cancelPurchase(row.id)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("已取消")
  }).catch(() => {})
}

loadSuppliers()
loadProducts()
getList()
</script>
