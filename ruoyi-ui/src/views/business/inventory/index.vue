<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
         <el-form-item label="商品ID" prop="productId">
            <el-input-number
               v-model="queryParams.productId"
               placeholder="请输入商品ID"
               :min="0"
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="变动类型" prop="changeType">
            <el-select
               v-model="queryParams.changeType"
               placeholder="请选择变动类型"
               clearable
               style="width: 200px"
            >
               <el-option
                  v-for="item in logTypes"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="变动时间">
            <el-date-picker
               v-model="dateRange"
               type="daterange"
               range-separator="至"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
               value-format="YYYY-MM-DD"
               style="width: 260px"
            />
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="inventoryList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="日志编号" align="center" prop="id" width="80" />
         <el-table-column label="商品ID" align="center" prop="productId" width="100" />
         <el-table-column label="变动类型" align="center" prop="changeType" width="110">
            <template #default="scope">
               <el-tag :type="logTagType(scope.row.changeType)">{{ logLabel(scope.row.changeType) }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="变动数量" align="center" prop="changeQuantity" width="100">
            <template #default="scope">
               <span :style="{ color: scope.row.changeQuantity > 0 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
                  {{ scope.row.changeQuantity > 0 ? '+' : '' }}{{ scope.row.changeQuantity }}
               </span>
            </template>
         </el-table-column>
         <el-table-column label="变动前库存" align="center" prop="stockBefore" width="100" />
         <el-table-column label="变动后库存" align="center" width="130">
            <template #default="scope">
               <span>{{ scope.row.stockBefore }}</span>
               <span style="margin: 0 6px; color: #909399;">→</span>
               <span>{{ scope.row.stockAfter }}</span>
            </template>
         </el-table-column>
         <el-table-column label="关联单号" align="center" prop="relatedOrderNo" width="130" :show-overflow-tooltip="true" />
         <el-table-column label="操作人" align="center" prop="createBy" width="100" />
         <el-table-column label="操作时间" align="center" prop="createTime" width="160">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="备注" align="center" prop="remark" :show-overflow-tooltip="true" />
      </el-table>

      <pagination
         v-show="total > 0"
         :total="total"
         v-model:page="queryParams.pageNum"
         v-model:limit="queryParams.pageSize"
         @pagination="getList"
      />
   </div>
</template>

<script setup name="Inventory">
import { listInventory } from "@/api/business/inventory"

const { proxy } = getCurrentInstance()

const inventoryList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const total = ref(0)
const dateRange = ref([])

const logTypes = ref([
  { label: "采购入库", value: "PURCHASE_IN" },
  { label: "销售出库", value: "SALE_OUT" },
  { label: "手动调整", value: "MANUAL_ADJUST" },
  { label: "取消退回", value: "CANCEL_RESTORE" }
])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    productId: undefined,
    changeType: undefined,
    beginTime: undefined,
    endTime: undefined
  }
})

const { queryParams } = toRefs(data)

/** 日志类型标签颜色 */
function logTagType(type) {
  const map = { PURCHASE_IN: "success", SALE_OUT: "danger", MANUAL_ADJUST: "warning", CANCEL_RESTORE: "info" }
  return map[type] || ""
}

/** 日志类型标签文本 */
function logLabel(type) {
  const item = logTypes.value.find(t => t.value === type)
  return item ? item.label : type
}

/** 查询库存日志列表 */
function getList() {
  loading.value = true
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.value.beginTime = dateRange.value[0]
    queryParams.value.endTime = dateRange.value[1]
  } else {
    queryParams.value.beginTime = undefined
    queryParams.value.endTime = undefined
  }
  listInventory(queryParams.value).then(response => {
    inventoryList.value = response.rows
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
  dateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
}

getList()
</script>
