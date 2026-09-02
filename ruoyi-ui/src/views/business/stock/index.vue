<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="商品名称" prop="productName">
        <el-input v-model="queryParams.productName" placeholder="请输入商品名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable>
          <el-option v-for="c in categoryOptions" :key="c.id" :label="c.categoryName" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="库存状态" prop="stockStatus">
        <el-select v-model="queryParams.stockStatus" placeholder="请选择" clearable>
          <el-option label="库存不足(<10)" value="low" />
          <el-option label="库存充足(≥10)" value="ok" />
          <el-option label="已售罄(0)" value="zero" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button icon="Refresh" @click="getList">刷新库存</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 库存汇总卡片 -->
    <el-row :gutter="16" class="mb16">
      <el-col :span="6">
        <el-card shadow="hover" class="stock-stat-card">
          <div class="stat-label">商品总数</div>
          <div class="stat-value">{{ totalProducts }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stock-stat-card">
          <div class="stat-label">库存总量</div>
          <div class="stat-value" style="color:#67c23a">{{ totalStock }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stock-stat-card">
          <div class="stat-label">低库存预警(&lt;10)</div>
          <div class="stat-value" style="color:#e6a23c">{{ lowStockCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stock-stat-card">
          <div class="stat-label">已售罄</div>
          <div class="stat-value" style="color:#f56c6c">{{ zeroStockCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="productList">
      <el-table-column label="商品ID" align="center" prop="id" width="80" />
      <el-table-column label="商品名称" align="center" prop="productName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="条码" align="center" prop="barcode" width="140" />
      <el-table-column label="分类" align="center" width="100">
        <template #default="{row}">{{ getCategoryName(row.categoryId) }}</template>
      </el-table-column>
      <el-table-column label="单价" align="center" width="100"><template #default="{row}">¥{{ row.price }}</template></el-table-column>
      <el-table-column label="当前库存" align="center" width="120" sortable prop="stock">
        <template #default="{row}">
          <span :style="{ color: row.stock === 0 ? '#f56c6c' : row.stock < 10 ? '#e6a23c' : '#67c23a', fontWeight: 'bold', fontSize: '16px' }">
            {{ row.stock || 0 }}
          </span>
          <span style="color:#999;font-size:12px"> {{ row.unit || '件' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="库存状态" align="center" width="100">
        <template #default="{row}">
          <el-tag v-if="row.stock === 0" type="danger">已售罄</el-tag>
          <el-tag v-else-if="row.stock < 10" type="warning">库存不足</el-tag>
          <el-tag v-else type="success">充足</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="货架位置" align="center" prop="shelfArea" width="140" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160">
        <template #default="{row}">{{ row.updateTime || row.createTime }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Stock">
import { listProduct } from "@/api/business/product"
import { listCategory } from "@/api/business/category"

const { proxy } = getCurrentInstance()

const productList = ref([])
const categoryOptions = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const totalProducts = ref(0)
const totalStock = ref(0)
const lowStockCount = ref(0)
const zeroStockCount = ref(0)

const data = reactive({
  queryParams: { pageNum: 1, pageSize: 20, productName: undefined, categoryId: undefined, stockStatus: undefined }
})
const { queryParams } = toRefs(data)

function getCategoryName(id) {
  const cat = categoryOptions.value.find(c => c.id === id)
  return cat ? cat.categoryName : ''
}

function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm("queryRef"); handleQuery() }

function getList() {
  loading.value = true
  const params = { ...queryParams.value }
  // 按库存状态筛选
  if (params.stockStatus === 'zero') {
    // 需要查所有数据再前端过滤
    params.pageSize = 9999
  }
  delete params.stockStatus

  listProduct(params).then(response => {
    let rows = response.rows
    // 前端过滤库存状态
    if (queryParams.value.stockStatus === 'low') {
      rows = rows.filter(r => (r.stock || 0) > 0 && (r.stock || 0) < 10)
    } else if (queryParams.value.stockStatus === 'zero') {
      rows = rows.filter(r => (r.stock || 0) === 0)
    } else if (queryParams.value.stockStatus === 'ok') {
      rows = rows.filter(r => (r.stock || 0) >= 10)
    }
    productList.value = rows
    total.value = rows.length

    // 汇总统计（查全部商品）
    loadSummary()
    loading.value = false
  })
}

function loadSummary() {
  listProduct({ pageNum: 1, pageSize: 9999 }).then(res => {
    const all = res.rows
    totalProducts.value = all.length
    totalStock.value = all.reduce((s, r) => s + (r.stock || 0), 0)
    lowStockCount.value = all.filter(r => (r.stock || 0) > 0 && (r.stock || 0) < 10).length
    zeroStockCount.value = all.filter(r => (r.stock || 0) === 0).length
  })
}

function loadCategories() {
  listCategory({ pageNum: 1, pageSize: 999 }).then(res => { categoryOptions.value = res.rows })
}

loadCategories()
getList()
</script>

<style scoped>
.stock-stat-card { text-align: center; cursor: default }
.stat-label { font-size: 13px; color: #999; margin-bottom: 8px }
.stat-value { font-size: 28px; font-weight: 700; color: #409EFF }
.mb16 { margin-bottom: 16px }
</style>
