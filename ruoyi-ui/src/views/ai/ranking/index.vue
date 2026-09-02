<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">

      <!-- ==================== Tab 1: 热销排行榜 ==================== -->
      <el-tab-pane label="热销排行榜" name="hot">
        <el-form :model="hotQuery" ref="hotQueryRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="商品名称" prop="productName">
            <el-input v-model="hotQuery.productName" placeholder="请输入" clearable style="width:200px" @keyup.enter="getHotList" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getHotList">搜索</el-button>
            <el-button icon="Refresh" @click="resetHotQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="hotLoading" :data="hotList">
          <el-table-column label="商品" align="center" prop="productName" :show-overflow-tooltip="true" min-width="160" />
          <el-table-column label="价格" align="center" width="90">
            <template #default="s">¥{{ s.row.productPrice }}</template>
          </el-table-column>
          <el-table-column label="销量" align="center" prop="salesCount" width="100" sortable />
          <el-table-column label="周期" align="center" prop="period" width="80">
            <template #default="s">{{ s.row.period || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="180">
            <template #default="s">
              <el-button link type="primary" icon="Edit" @click="handleHotEdit(s.row)">设置销量</el-button>
              <el-button v-if="s.row.id" link type="primary" icon="Delete" @click="handleHotDelete(s.row)">清空</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="hotTotal>0" :total="hotTotal" v-model:page="hotQuery.pageNum" v-model:limit="hotQuery.pageSize" @pagination="getHotList" />
      </el-tab-pane>

      <!-- ==================== Tab 2: 运营推荐榜 ==================== -->
      <el-tab-pane label="运营推荐榜" name="recommend">
        <el-form :model="recQuery" ref="recQueryRef" :inline="true" v-show="showSearch" label-width="80px">
          <el-form-item label="商品名称" prop="productName">
            <el-input v-model="recQuery.productName" placeholder="请输入" clearable style="width:200px" @keyup.enter="getRecList" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getRecList">搜索</el-button>
            <el-button icon="Refresh" @click="resetRecQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="recLoading" :data="recList">
          <el-table-column label="商品" align="center" prop="productName" :show-overflow-tooltip="true" min-width="160" />
          <el-table-column label="价格" align="center" width="90">
            <template #default="s">¥{{ s.row.productPrice }}</template>
          </el-table-column>
          <el-table-column label="推荐指数" align="center" prop="score" width="110" sortable />
          <el-table-column label="推荐理由" align="center" prop="reason" :show-overflow-tooltip="true" min-width="140" />
          <el-table-column label="操作" align="center" width="180">
            <template #default="s">
              <el-button link type="primary" icon="Edit" @click="handleRecEdit(s.row)">设置推荐</el-button>
              <el-button v-if="s.row.id" link type="primary" icon="Delete" @click="handleRecDelete(s.row)">清空</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="recTotal>0" :total="recTotal" v-model:page="recQuery.pageNum" v-model:limit="recQuery.pageSize" @pagination="getRecList" />
      </el-tab-pane>

      <!-- ==================== Tab 3: 融合展示榜 ==================== -->
      <el-tab-pane label="融合展示榜" name="display">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="warning" plain icon="RefreshRight" :loading="regenerating" @click="handleRegenerate">重新生成</el-button>
          </el-col>
        </el-row>
        <el-table v-loading="dispLoading" :data="dispList">
          <el-table-column label="商品" align="center" prop="productName" :show-overflow-tooltip="true" min-width="160" />
          <el-table-column label="价格" align="center" width="90">
            <template #default="s">¥{{ s.row.productPrice }}</template>
          </el-table-column>
          <el-table-column label="最终得分" align="center" prop="finalScore" width="110" sortable />
          <el-table-column label="热销分" align="center" prop="hotScore" width="90" />
          <el-table-column label="推荐分" align="center" prop="recScore" width="90" />
          <el-table-column label="来源" align="center" width="120">
            <template #default="s">
              <el-tag :type="sourceTag(s.row.source)" size="small">{{ sourceLabel(s.row.source) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生成时间" align="center" width="170">
            <template #default="s">{{ s.row.generateTime || '-' }}</template>
          </el-table-column>
        </el-table>
        <pagination v-show="dispTotal>0" :total="dispTotal" v-model:page="dispQuery.pageNum" v-model:limit="dispQuery.pageSize" @pagination="getDispList" />
      </el-tab-pane>

    </el-tabs>

    <!-- ==================== 热销设置弹窗 ==================== -->
    <el-dialog v-model="hotOpen" title="设置销量" width="450px" append-to-body>
      <el-form ref="hotRef" :model="hotForm" :rules="hotRules" label-width="100px">
        <el-form-item label="商品">
          <span>{{ hotForm.productName }}</span>
        </el-form-item>
        <el-form-item label="销量" prop="salesCount">
          <el-input-number v-model="hotForm.salesCount" :min="0" controls-position="right" style="width:200px" />
        </el-form-item>
        <el-form-item label="周期" prop="period">
          <el-select v-model="hotForm.period" style="width:150px">
            <el-option label="日榜" value="DAILY" />
            <el-option label="周榜" value="WEEKLY" />
            <el-option label="月榜" value="MONTHLY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitHotForm">确 定</el-button>
        <el-button @click="hotOpen=false">取 消</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 推荐设置弹窗 ==================== -->
    <el-dialog v-model="recOpen" title="设置推荐" width="450px" append-to-body>
      <el-form ref="recRef" :model="recForm" :rules="recRules" label-width="100px">
        <el-form-item label="商品">
          <span>{{ recForm.productName }}</span>
        </el-form-item>
        <el-form-item label="推荐指数" prop="score">
          <el-input-number v-model="recForm.score" :min="0" :max="100" controls-position="right" style="width:200px" />
          <span class="form-tip">0-100，越高越优先</span>
        </el-form-item>
        <el-form-item label="推荐理由">
          <el-input v-model="recForm.reason" placeholder="如：新品上市、季节热销" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitRecForm">确 定</el-button>
        <el-button @click="recOpen=false">取 消</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup name="Ranking">
import { ref, reactive, getCurrentInstance } from "vue"
import { listHotRanking, saveHotRanking, delHotRanking,
         listRecommend, saveRecommend, delRecommend,
         listDisplayRanking, regenerateDisplay } from "@/api/ai/ranking"

const { proxy } = getCurrentInstance()
const { sys_normal_disable } = proxy.useDict("sys_normal_disable")

const activeTab = ref("hot")
const showSearch = ref(true)
const regenerating = ref(false)

function sourceLabel(v) { return { BOTH:"热销+推荐", HOT_ONLY:"仅热销", REC_ONLY:"仅推荐" }[v] || v }
function sourceTag(v)  { return { BOTH:"success", HOT_ONLY:"warning", REC_ONLY:"info" }[v] || "" }

// ========== 热销榜 ==========
const hotList = ref([])
const hotLoading = ref(false)
const hotTotal = ref(0)
const hotOpen = ref(false)
const hotQuery = reactive({ pageNum:1, pageSize:10, productName:undefined, period:undefined })
const hotForm = reactive({ id:undefined, productId:undefined, productName:"", salesCount:0, period:"WEEKLY" })
const hotRules = { salesCount:[{ required:true, message:"销量不能为空", trigger:"blur" }] }

function getHotList() {
  hotLoading.value = true
  listHotRanking(hotQuery).then(r => { hotList.value = r.rows; hotTotal.value = r.total; hotLoading.value = false })
}
function resetHotQuery() { proxy.resetForm("hotQueryRef"); getHotList() }
function handleHotEdit(row) {
  Object.assign(hotForm, { id:row.id, productId:row.productId, productName:row.productName, salesCount:row.salesCount||0, period:row.period||"WEEKLY" })
  hotOpen.value = true
}
function submitHotForm() {
  proxy.$refs["hotRef"].validate(v => {
    if (v) {
      saveHotRanking({ productId:hotForm.productId, salesCount:hotForm.salesCount, period:hotForm.period, status:"0" }).then(() => {
        proxy.$modal.msgSuccess("保存成功"); hotOpen.value = false; getHotList(); getDispList()
      })
    }
  })
}
function handleHotDelete(row) {
  proxy.$modal.confirm("确认清空该商品的销量数据？").then(() => delHotRanking(row.id)).then(() => {
    getHotList(); getDispList(); proxy.$modal.msgSuccess("已清空")
  }).catch(() => {})
}

// ========== 推荐榜 ==========
const recList = ref([])
const recLoading = ref(false)
const recTotal = ref(0)
const recOpen = ref(false)
const recQuery = reactive({ pageNum:1, pageSize:10, productName:undefined })
const recForm = reactive({ id:undefined, productId:undefined, productName:"", score:50, reason:"" })
const recRules = { score:[{ required:true, message:"推荐指数不能为空", trigger:"blur" }] }

function getRecList() {
  recLoading.value = true
  listRecommend(recQuery).then(r => { recList.value = r.rows; recTotal.value = r.total; recLoading.value = false })
}
function resetRecQuery() { proxy.resetForm("recQueryRef"); getRecList() }
function handleRecEdit(row) {
  Object.assign(recForm, { id:row.id, productId:row.productId, productName:row.productName, score:row.score||50, reason:row.reason||"" })
  recOpen.value = true
}
function submitRecForm() {
  proxy.$refs["recRef"].validate(v => {
    if (v) {
      saveRecommend({ productId:recForm.productId, score:recForm.score, reason:recForm.reason, status:"0" }).then(() => {
        proxy.$modal.msgSuccess("保存成功"); recOpen.value = false; getRecList(); getDispList()
      })
    }
  })
}
function handleRecDelete(row) {
  proxy.$modal.confirm("确认清空该商品的推荐数据？").then(() => delRecommend(row.id)).then(() => {
    getRecList(); getDispList(); proxy.$modal.msgSuccess("已清空")
  }).catch(() => {})
}

// ========== 展示榜 ==========
const dispList = ref([])
const dispLoading = ref(false)
const dispTotal = ref(0)
const dispQuery = reactive({ pageNum:1, pageSize:10 })

function getDispList() {
  dispLoading.value = true
  listDisplayRanking(dispQuery).then(r => { dispList.value = r.rows; dispTotal.value = r.total; dispLoading.value = false })
}
function handleRegenerate() {
  regenerating.value = true
  regenerateDisplay().then(() => { proxy.$modal.msgSuccess("展示榜已重新生成"); getDispList(); regenerating.value = false })
}

// ========== Tab切换 ==========
function handleTabClick(t) {
  if (t.props.name === "hot") getHotList()
  else if (t.props.name === "recommend") getRecList()
  else getDispList()
}

getHotList()
</script>

<style scoped>
.form-tip { color:#909399; font-size:12px; margin-left:8px; }
</style>
