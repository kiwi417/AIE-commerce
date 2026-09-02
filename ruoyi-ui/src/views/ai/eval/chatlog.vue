<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true">
      <el-form-item label="意图">
        <el-select v-model="queryParams.intent" placeholder="全部" clearable style="width:150px" @change="handleQuery">
          <el-option label="CONSULTATION" value="CONSULTATION" />
          <el-option label="ANALYTICS" value="ANALYTICS" />
          <el-option label="OPERATION" value="OPERATION" />
        </el-select>
      </el-form-item>
      <el-form-item label="Agent">
        <el-select v-model="queryParams.agent" placeholder="全部" clearable style="width:130px" @change="handleQuery">
          <el-option label="RAG" value="CONSULTATION" />
          <el-option label="TextToSQL" value="ANALYTICS" />
          <el-option label="Tools" value="OPERATION" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="!ids.length" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" align="center" />
      <el-table-column label="用户问题" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <a class="link-type" @click="handleView(row)">{{ row.question }}</a>
        </template>
      </el-table-column>
      <el-table-column label="意图" prop="intent" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.intent === 'CONSULTATION' ? 'success' : row.intent === 'ANALYTICS' ? 'warning' : 'info'" size="small">{{ row.intent }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Agent" prop="agent" width="100" align="center" />
      <el-table-column label="耗时" prop="latencyMs" width="80" align="center">
        <template #default="{ row }">{{ row.latencyMs }}ms</template>
      </el-table-column>
      <el-table-column label="时间" prop="createTime" width="160" align="center" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="detailVisible" title="对话详情" width="700px">
      <div class="detail-section">
        <div class="detail-label">用户问题</div>
        <div class="detail-content">{{ currentRow.question }}</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">AI 回答</div>
        <div class="detail-content answer-box">{{ currentRow.answer }}</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">意图 / Agent / 耗时</div>
        <div class="detail-content">{{ currentRow.intent }} &nbsp;|&nbsp; {{ currentRow.agent }} &nbsp;|&nbsp; {{ currentRow.latencyMs }}ms</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listChatLog, getChatLog, delChatLog } from '@/api/ai/chatlog'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const ids = ref([])
const detailVisible = ref(false)
const currentRow = ref({})

const queryParams = reactive({ pageNum: 1, pageSize: 10, intent: '', agent: '' })

onMounted(() => getList())

function getList() {
  loading.value = true
  listChatLog(queryParams).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => loading.value = false)
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { queryParams.intent = ''; queryParams.agent = ''; queryParams.pageNum = 1; getList() }

function handleSelectionChange(val) { ids.value = val.map(r => r.id) }

function handleView(row) {
  getChatLog(row.id).then(res => {
    currentRow.value = res.data || res
    detailVisible.value = true
  })
}

function handleDelete() {
  ElMessageBox.confirm('确认删除选中的对话记录？', '提示', { type: 'warning' }).then(() => {
    delChatLog(ids.value.join(',')).then(() => { getList(); ElMessage.success('删除成功') })
  })
}
</script>

<style scoped>
.detail-section { margin-bottom: 14px; }
.detail-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.detail-content { font-size: 14px; color: #303133; line-height: 1.6; }
.answer-box { background: #f5f7fa; padding: 10px; border-radius: 6px; white-space: pre-wrap; }
.link-type { color: #409EFF; cursor: pointer; }
.link-type:hover { text-decoration: underline; }
</style>
