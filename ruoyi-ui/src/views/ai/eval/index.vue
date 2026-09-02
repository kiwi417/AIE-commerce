<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-num">{{ stats.avgScore }}</div>
            <div class="stat-label">平均评分</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-num">{{ stats.totalCount }}</div>
            <div class="stat-label">评测总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-num">{{ stats.ragAvg }}</div>
            <div class="stat-label">RAG 平均分</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-num">{{ stats.sqlAvg }}</div>
            <div class="stat-label">SQL 平均分</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索 -->
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="意图">
        <el-select v-model="queryParams.intent" placeholder="全部" clearable style="width:150px" @change="handleQuery">
          <el-option label="CONSULTATION" value="CONSULTATION" />
          <el-option label="ANALYTICS" value="ANALYTICS" />
          <el-option label="OPERATION" value="OPERATION" />
        </el-select>
      </el-form-item>
      <el-form-item label="评分">
        <el-select v-model="queryParams.score" placeholder="全部" clearable style="width:100px" @change="handleQuery">
          <el-option v-for="s in [1,2,3,4,5,6,7,8,9,10]" :key="s" :label="s+'分'" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作栏 -->
    <el-row class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="!ids.length" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="用户问题" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <a class="link-type" @click="handleView(row)">{{ row.question }}</a>
        </template>
      </el-table-column>
      <el-table-column label="意图" prop="intent" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="row.intent === 'CONSULTATION' ? 'success' : row.intent === 'ANALYTICS' ? 'warning' : 'info'" size="small">{{ row.intent }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评分" prop="score" width="80" align="center">
        <template #default="{ row }">
          <span :style="{ color: row.score >= 8 ? '#67c23a' : row.score >= 6 ? '#e6a23c' : '#f56c6c', fontWeight: 'bold' }">{{ row.score }}/10</span>
        </template>
      </el-table-column>
      <el-table-column label="评分理由" :show-overflow-tooltip="true" prop="judgeReason" />
      <el-table-column label="耗时" prop="latencyMs" width="80" align="center">
        <template #default="{ row }">{{ row.latencyMs }}ms</template>
      </el-table-column>
      <el-table-column label="时间" prop="createTime" width="160" align="center" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="评测详情" width="700px">
      <div class="detail-section">
        <div class="detail-label">用户问题</div>
        <div class="detail-content">{{ currentRow.question }}</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">AI 回答</div>
        <div class="detail-content answer-box">{{ currentRow.answer }}</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">意图 / 评分 / 耗时</div>
        <div class="detail-content">{{ currentRow.intent }} &nbsp;|&nbsp; {{ currentRow.score }}/10 &nbsp;|&nbsp; {{ currentRow.latencyMs }}ms</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">评分理由</div>
        <div class="detail-content">{{ currentRow.judgeReason }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listEvalLog, getEvalLog, delEvalLog, getEvalStats } from '@/api/ai/eval'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const ids = ref([])
const detailVisible = ref(false)
const currentRow = ref({})

const queryParams = reactive({ pageNum: 1, pageSize: 10, intent: '', score: '' })
const stats = reactive({ avgScore: '--', totalCount: 0, ragAvg: '--', sqlAvg: '--' })

onMounted(() => { getList(); loadStats() })

function getList() {
  loading.value = true
  listEvalLog(queryParams).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => loading.value = false)
}

function loadStats() {
  getEvalStats().then(res => {
    if (res.code === 200 && res.data) {
      stats.avgScore = res.data.avgScore || '--'
      stats.totalCount = res.data.totalCount || 0
    }
  })
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  queryParams.intent = ''; queryParams.score = ''; queryParams.pageNum = 1
  getList()
}

function handleSelectionChange(val) { ids.value = val.map(r => r.id) }

function handleView(row) {
  getEvalLog(row.id).then(res => {
    currentRow.value = res.data || res
    detailVisible.value = true
  })
}

function handleDelete() {
  ElMessageBox.confirm('确认删除选中的评测记录？', '提示', { type: 'warning' }).then(() => {
    delEvalLog(ids.value.join(',')).then(() => { getList(); loadStats(); ElMessage.success('删除成功') })
  })
}
</script>

<style scoped>
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 10px 0; }
.stat-num { font-size: 28px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.search-form { margin-top: 8px; }
.detail-section { margin-bottom: 14px; }
.detail-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.detail-content { font-size: 14px; color: #303133; line-height: 1.6; }
.answer-box { background: #f5f7fa; padding: 10px; border-radius: 6px; white-space: pre-wrap; }
.link-type { color: #409EFF; cursor: pointer; }
.link-type:hover { text-decoration: underline; }
</style>
