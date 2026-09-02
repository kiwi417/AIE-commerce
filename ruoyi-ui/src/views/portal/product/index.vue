<template>
   <div class="portal-container">
      <!-- 顶部导航栏 -->
      <div class="portal-header">
         <div class="header-left">
            <span class="header-title">&#x1f6d2; AI智能导购</span>
         </div>
         <div class="header-center"></div>
         <div class="header-right">
            <el-link :underline="false" @click="goToChat" class="chat-link">
               &#x1f4ac; AI对话
            </el-link>
            <el-badge :value="cartCount" :hidden="cartCount === 0"><el-button size="small" @click="goToCart">🛒 购物车</el-button></el-badge>
            <el-button type="primary" size="small" @click="goToChat">去提问</el-button>
         </div>
      </div>

      <!-- 搜索栏 -->
      <div class="search-bar">
         <el-input
            v-model="searchKeyword"
            placeholder="搜索商品名称..."
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            class="search-input"
         />
         <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
      </div>

      <!-- 分类筛选 -->
      <div class="category-bar" v-if="categoryList.length > 0">
         <el-button
            :type="currentCategoryId === '' ? 'primary' : ''"
            size="small"
            @click="onCategoryChange('')"
         >全部</el-button>
         <el-button
            v-for="cat in categoryList"
            :key="cat.id"
            :type="currentCategoryId === cat.id ? 'primary' : ''"
            size="small"
            @click="onCategoryChange(cat.id)"
         >{{ cat.categoryName }}</el-button>
      </div>

      <!-- 商品网格 -->
      <div v-loading="loading" class="product-section">
         <template v-if="productList.length > 0">
            <el-row :gutter="16">
               <el-col
                  v-for="product in productList"
                  :key="product.id"
                  :xs="24"
                  :sm="12"
                  :md="8"
                  :lg="6"
                  class="product-col"
               >
                  <el-card shadow="hover" class="product-card" @click="goToDetail(product)">
                     <div class="product-image">
                        <img
                           v-if="product.imageUrl"
                           :src="product.imageUrl"
                           :alt="product.productName"
                           class="product-img"
                        />
                        <div v-else class="product-placeholder">
                           {{ product.productName ? product.productName.charAt(0) : '?' }}
                        </div>
                     </div>
                     <div class="product-info">
                        <div class="product-name" :title="product.productName">
                           {{ product.productName }}
                        </div>
                        <div class="product-area" v-if="product.shelfArea">
                           {{ product.shelfArea }}
                        </div>
                        <div class="product-bottom">
                           <span class="product-price">
                              &#xa5;{{ product.price ? product.price.toFixed(2) : '0.00' }}
                           </span>
                           <el-button size="small" type="primary" :loading="!!addingMap[product.id]" :disabled="!!addingMap[product.id]" @click.stop="handleAddCart(product)">+购物车</el-button>
                        </div>
                     </div>
                  </el-card>
               </el-col>
            </el-row>

            <!-- 分页 -->
            <div class="pagination-wrapper">
               <el-pagination
                  v-model:current-page="queryParams.pageNum"
                  v-model:page-size="queryParams.pageSize"
                  :page-sizes="[12, 24, 48]"
                  :total="total"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="loadProducts"
                  @current-change="loadProducts"
               />
            </div>
         </template>

         <!-- 空状态 -->
         <el-empty v-else description="暂无商品数据" />
      </div>
   </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { listPortalCategories, pagePortalProducts, addToCart, getCartList } from '@/api/portal/index'

const { proxy } = getCurrentInstance()

const router = useRouter()
const route = useRoute()

// 分类数据
const categoryList = ref([])
const currentCategoryId = ref('')

// 搜索
const searchKeyword = ref('')

// 商品数据
const productList = ref([])
const total = ref(0)
const loading = ref(false)

// 防快速点击：记录正在加入购物车的商品ID
const addingMap = reactive({})

// 查询参数
const queryParams = reactive({
   pageNum: 1,
   pageSize: 12,
   productName: '',
   categoryId: ''
})

// 加载分类列表
function loadCategories() {
   listPortalCategories().then(res => {
      if (res.code === 200) {
         categoryList.value = res.data || []
      }
   })
}

// 加载商品列表
function loadProducts() {
   loading.value = true
   queryParams.categoryId = currentCategoryId.value
   queryParams.productName = searchKeyword.value
   pagePortalProducts(queryParams).then(res => {
      if (res.code === 200) {
         productList.value = res.rows || []
         total.value = res.total || 0
      }
   }).finally(() => {
      loading.value = false
   })
}

// 分类切换
function onCategoryChange(categoryId) {
   currentCategoryId.value = categoryId
   queryParams.pageNum = 1
   loadProducts()
}

// 搜索
function handleSearch() {
   queryParams.pageNum = 1
   loadProducts()
}

// 跳转详情
function goToDetail(product) {
   router.push({
      path: '/portal/product/' + product.id
   })
}

function getSessionId() {
  let sid = localStorage.getItem('cdc_chat_session_id')
  if (!sid) { sid = Date.now().toString(36); localStorage.setItem('cdc_chat_session_id', sid) }
  return sid
}

function handleAddCart(product) {
  if (addingMap[product.id]) return
  addingMap[product.id] = true
  addToCart({ sessionId: getSessionId(), productId: product.id, quantity: 1 }).then(() => {
    proxy.$modal.msgSuccess('已加入购物车')
    loadCartCount()
  }).catch(err => {
    proxy.$modal.msgError(err?.message || '加入购物车失败')
  }).finally(() => {
    delete addingMap[product.id]
  })
}

const cartCount = ref(0)
function loadCartCount() {
  getCartList(getSessionId()).then(res => { const items = res.data && res.data.cartItems ? res.data.cartItems : []; cartCount.value = items.reduce((s,i) => s + i.quantity, 0) })
}

function goToCart() {
   router.push('/portal/cart')
}

function goToChat() {
   router.push({
      path: '/portal/chat'
   })
}

onMounted(() => {
   loadCategories()
   loadProducts()
})
</script>

<style scoped>
.portal-container {
   max-width: 1200px;
   margin: 0 auto;
   padding: 16px;
   min-height: 100vh;
   background: #f5f6fa;
}

/* 顶部导航 */
.portal-header {
   display: flex;
   align-items: center;
   justify-content: space-between;
   background: #fff;
   padding: 12px 20px;
   border-radius: 8px;
   margin-bottom: 16px;
   box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
   flex-wrap: wrap;
   gap: 12px;
}

.header-title {
   font-size: 20px;
   font-weight: 700;
   color: #303133;
}

.header-center {
   flex: 1;
   display: flex;
   justify-content: center;
}

.header-right {
   display: flex;
   align-items: center;
   gap: 12px;
}

.chat-link {
   font-size: 15px;
}

/* 搜索栏 */
.search-bar {
   display: flex;
   gap: 12px;
   margin-bottom: 16px;
}

.search-input {
   flex: 1;
}

/* 分类筛选 */
.category-bar {
   display: flex;
   flex-wrap: wrap;
   gap: 8px;
   margin-bottom: 16px;
}

/* 商品网格 */
.product-section {
   min-height: 300px;
}

.product-col {
   margin-bottom: 16px;
}

.product-card {
   cursor: pointer;
   border-radius: 8px;
   transition: transform 0.2s, box-shadow 0.2s;
   overflow: hidden;
}

.product-card:hover {
   transform: translateY(-4px);
}

.product-image {
   width: 100%;
   height: 180px;
   display: flex;
   align-items: center;
   justify-content: center;
   background: #f5f7fa;
   border-radius: 6px;
   overflow: hidden;
}

.product-img {
   width: 100%;
   height: 100%;
   object-fit: cover;
}

.product-placeholder {
   font-size: 56px;
   font-weight: 700;
   color: #c0c4cc;
   user-select: none;
}

.product-info {
   padding-top: 12px;
}

.product-name {
   font-size: 15px;
   font-weight: 500;
   color: #303133;
   white-space: nowrap;
   overflow: hidden;
   text-overflow: ellipsis;
   margin-bottom: 6px;
}

.product-area {
   font-size: 12px;
   color: #909399;
   margin-bottom: 10px;
}

.product-bottom {
   display: flex;
   align-items: center;
   justify-content: space-between;
}

.product-price {
   font-size: 18px;
   font-weight: 700;
   color: #f56c6c;
}

.product-meta {
   font-size: 12px;
   color: #909399;
}

/* 分页 */
.pagination-wrapper {
   display: flex;
   justify-content: center;
   margin-top: 24px;
   padding-bottom: 24px;
}

/* 响应式 */
@media (max-width: 768px) {
   .portal-container {
      padding: 8px;
   }

   .portal-header {
      flex-direction: column;
      align-items: stretch;
      padding: 12px;
   }

   .header-center {
      justify-content: flex-start;
   }

   .header-right {
      justify-content: flex-end;
   }

   .header-title {
      font-size: 18px;
   }

   .search-bar {
      flex-direction: column;
   }

   .product-image {
      height: 140px;
   }

   .product-placeholder {
      font-size: 42px;
   }
}
</style>
