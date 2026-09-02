<template>
   <div class="portal-shell">
      <!-- 顶部导航栏 -->
      <div class="shell-header">
         <span class="header-title">&#x1f6d2; AI智能导购</span>
         <div class="header-actions">
            <el-badge :value="cartCount" :hidden="cartCount === 0">
               <el-button size="small" @click="goToCart">&#x1f6d2; 购物车</el-button>
            </el-badge>
         </div>
      </div>

      <!-- 主体：左商品 + 右对话，2:1 -->
      <div class="shell-body">
         <!-- ==================== 左侧：商品区 ==================== -->
         <div class="left-panel">
            <!-- 搜索栏 -->
            <div class="search-bar">
               <el-input v-model="searchKeyword" placeholder="搜索商品..." clearable
                  :prefix-icon="Search" @keyup.enter="handleSearch" class="search-input" />
               <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            </div>

            <!-- 分类筛选 -->
            <div class="category-bar" v-if="categoryList.length > 0">
               <el-button :type="currentCategoryId === '' ? 'primary' : ''" size="small"
                  @click="onCategoryChange('')">全部</el-button>
               <el-button v-for="cat in categoryList" :key="cat.id" size="small"
                  :type="currentCategoryId === cat.id ? 'primary' : ''"
                  @click="onCategoryChange(cat.id)">{{ cat.categoryName }}</el-button>
            </div>

            <!-- 商品网格 -->
            <div v-loading="loading" class="product-area">
               <template v-if="productList.length > 0">
                  <div class="product-grid">
                     <div v-for="p in productList" :key="p.id" class="product-cell"
                        @click="goToDetail(p)">
                        <div class="product-img-box">
                           <img v-if="p.imageUrl" :src="p.imageUrl" :alt="p.productName" />
                           <div v-else class="img-placeholder">{{ p.productName.charAt(0) }}</div>
                        </div>
                        <div class="product-text">
                           <div class="p-name">{{ p.productName }}</div>
                           <div class="p-shelf" v-if="p.shelfArea">&#x1f4cd; {{ p.shelfArea }}</div>
                           <div class="p-bottom">
                              <span class="p-price">&yen;{{ p.price ? p.price.toFixed(2) : '0.00' }}</span>
                              <el-button size="small" type="primary"
                                 :loading="!!addingMap[p.id]" :disabled="!!addingMap[p.id]"
                                 @click.stop="handleAddCart(p)">+购物车</el-button>
                           </div>
                        </div>
                     </div>
                  </div>
                  <div class="pagination-wrap">
                     <el-pagination v-model:current-page="queryParams.pageNum"
                        v-model:page-size="queryParams.pageSize" :page-sizes="[12, 24, 48]"
                        :total="total" layout="total, sizes, prev, pager, next"
                        @size-change="loadProducts" @current-change="loadProducts" />
                  </div>
               </template>
               <el-empty v-else description="暂无商品" />
            </div>
         </div>

         <!-- ==================== 右侧：AI对话 ==================== -->
         <div class="right-panel">
            <AiChatBox ref="chatBoxRef" :show-image-upload="true"
               @navigate="onChatNavigate" @category-change="onCategoryChange" />
         </div>
      </div>
   </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { listPortalCategories, pagePortalProducts, addToCart, getCartList } from '@/api/portal/index'
import { getPortalSessionId } from '@/utils/portalSession'
import AiChatBox from '@/components/AiChatBox/index.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()

// ==================== 商品区 ====================
const categoryList = ref([])
const currentCategoryId = ref('')
const searchKeyword = ref('')
const productList = ref([])
const total = ref(0)
const loading = ref(false)
const addingMap = reactive({})
const queryParams = reactive({ pageNum: 1, pageSize: 12, productName: '', categoryId: '' })

function loadCategories() {
   listPortalCategories().then(res => { if (res.code === 200) categoryList.value = res.data || [] })
}
function loadProducts() {
   loading.value = true
   queryParams.categoryId = currentCategoryId.value
   queryParams.productName = searchKeyword.value
   pagePortalProducts(queryParams).then(res => {
      if (res.code === 200) { productList.value = res.rows || []; total.value = res.total || 0 }
   }).finally(() => { loading.value = false })
}
function onCategoryChange(catId) { currentCategoryId.value = catId; queryParams.pageNum = 1; loadProducts() }
function handleSearch() { queryParams.pageNum = 1; loadProducts() }
function goToDetail(p) { router.push({ path: '/portal/product/detail/' + p.id }) }

const cartCount = ref(0)
function loadCartCount() {
   getCartList(getSessionId()).then(res => {
      const items = res.data && res.data.cartItems ? res.data.cartItems : []
      cartCount.value = items.reduce((s, i) => s + i.quantity, 0)
   })
}
function getSessionId() {
   return getPortalSessionId()
}
function handleAddCart(product) {
   if (addingMap[product.id]) return
   addingMap[product.id] = true
   addToCart({ sessionId: getSessionId(), productId: product.id, quantity: 1 }).then(() => {
      proxy.$modal.msgSuccess('已加入购物车'); loadCartCount()
   }).finally(() => { delete addingMap[product.id] })
}
function goToCart() { router.push('/portal/cart') }

// ==================== AI对话 ====================
const chatBoxRef = ref(null)

function onChatNavigate({ type, id }) {
   if (type === 'product') router.push({ path: '/portal/product/detail/' + id })
   else if (type === 'cart') router.push('/portal/cart')
   else if (type === 'order') router.push('/portal/order/query')
}

onMounted(() => {
   loadCategories(); loadProducts(); loadCartCount()
})
</script>

<style scoped>
.portal-shell { height: 100vh; display: flex; flex-direction: column; background: #f5f6fa; }

/* 顶部 */
.shell-header {
   display: flex; align-items: center; justify-content: space-between;
   padding: 10px 20px; background: #fff; border-bottom: 1px solid #ebeef5; flex-shrink: 0;
}
.header-title { font-size: 18px; font-weight: 700; color: #303133; }
.header-actions { display: flex; gap: 10px; }

/* 主体 2:1 */
.shell-body { flex: 1; display: flex; overflow: hidden; gap: 0; }

/* ===== 左侧商品 ===== */
.left-panel { flex: 3; display: flex; flex-direction: column; overflow: hidden; padding: 12px; }
.search-bar { display: flex; gap: 8px; margin-bottom: 10px; flex-shrink: 0; }
.search-input { flex: 1; }
.category-bar { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; flex-shrink: 0; }
.product-area { flex: 1; overflow-y: auto; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.product-cell {
   background: #fff; border-radius: 8px; overflow: hidden;
   box-shadow: 0 1px 4px rgba(0,0,0,0.06); cursor: pointer; transition: transform 0.15s;
}
.product-cell:hover { transform: translateY(-2px); }
.product-img-box {
   width: 100%; height: 130px; background: #f5f7fa;
   display: flex; align-items: center; justify-content: center;
}
.product-img-box img { width: 100%; height: 100%; object-fit: cover; }
.img-placeholder { font-size: 42px; font-weight: 700; color: #c0c4cc; }
.product-text { padding: 8px 10px; }
.p-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.p-shelf { font-size: 11px; color: #67c23a; margin-top: 2px; }
.p-bottom { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.p-price { font-size: 15px; font-weight: 700; color: #f56c6c; }
.pagination-wrap { display: flex; justify-content: center; margin-top: 16px; }

/* ===== 右侧AI面板 ===== */
.right-panel {
   flex: 1; display: flex; flex-direction: column;
   border-left: 1px solid #e4e7ed; background: #fff; min-width: 320px;
}

/* 响应式 */
@media (max-width: 768px) {
   .shell-body { flex-direction: column; }
   .right-panel { border-left: none; border-top: 1px solid #e4e7ed; min-width: unset; height: 40vh; }
   .product-grid { grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); }
}
</style>
