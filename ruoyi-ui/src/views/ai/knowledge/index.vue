<template>
  <div class="app-container">
    <!-- 搜索 + 操作 -->
    <el-form :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="标题">
        <el-input v-model="queryParams.title" placeholder="搜索标题" clearable style="width:180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="queryParams.category" placeholder="全部" clearable style="width:140px" @change="handleQuery">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="切片状态">
        <el-select v-model="queryParams.chunked" placeholder="全部" clearable style="width:110px" @change="handleQuery">
          <el-option label="已切片" value="1" />
          <el-option label="未切片" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" plain icon="Plus" @click="handleAdd">新增</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="标题" :show-overflow-tooltip="true">
        <template #default="{ row }">
          <a class="link-type" @click="handleView(row)">{{ row.title }}</a>
        </template>
      </el-table-column>
      <el-table-column label="分类" prop="category" width="110" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.category || '未分类' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件大小" prop="fileSize" width="100" align="center">
        <template #default="{ row }">{{ row.fileSize }} KB</template>
      </el-table-column>
      <el-table-column label="切片状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.chunked === '1' ? 'success' : 'info'" size="small">
            {{ row.chunked === '1' ? '已切片' : '未切片' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="切片数" prop="chunkCount" width="80" align="center">
        <template #default="{ row }">{{ row.chunked === '1' ? row.chunkCount : '-' }}</template>
      </el-table-column>
      <el-table-column label="切片大小" prop="chunkSize" width="90" align="center">
        <template #default="{ row }">{{ row.chunked === '1' ? row.chunkSize : '-' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="View" @click="handleView(row)">详情</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 上传弹窗 -->
    <el-dialog v-model="uploadVisible" title="新增文档" width="550px" :close-on-click-modal="false">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="选择文件" required>
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".docx"
          >
            <el-button icon="Upload">选择 .docx 文件</el-button>
            <template #tip><div class="el-upload__tip">仅支持 .docx 格式，文件将解析为纯文本</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="uploadForm.category" placeholder="如：产品手册、规章制度" clearable />
        </el-form-item>
        <el-form-item label="是否切片">
          <el-switch v-model="uploadForm.doChunk" />
          <span class="form-tip">{{ uploadForm.doChunk ? '上传后自动切片并写入向量库' : '仅存储原文，不入向量库' }}</span>
        </el-form-item>
        <template v-if="uploadForm.doChunk">
          <el-form-item label="切片大小">
            <el-input-number v-model="uploadForm.chunkSize" :min="100" :max="2000" :step="50" />
            <span class="form-tip">每块字符数，默认 300</span>
          </el-form-item>
          <el-form-item label="重叠">
            <el-input-number v-model="uploadForm.overlap" :min="0" :max="500" :step="10" />
            <span class="form-tip">相邻块重叠字符，防止边界信息丢失，默认 50</span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" icon="UploadFilled" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">
          {{ uploading ? '上传中...' : '确认上传' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="文档详情" width="750px">
      <div class="detail-section">
        <div class="detail-label">标题 / 分类</div>
        <div class="detail-content">{{ currentRow.title }} &nbsp;|&nbsp; {{ currentRow.category || '未分类' }}</div>
      </div>
      <div class="detail-section">
        <div class="detail-label">切片信息</div>
        <div class="detail-content">
          <el-tag :type="currentRow.chunked === '1' ? 'success' : 'info'" size="small">{{ currentRow.chunked === '1' ? '已切片' : '未切片' }}</el-tag>
          <template v-if="currentRow.chunked === '1'">
            &nbsp;|&nbsp; {{ currentRow.chunkCount }} 个切片 &nbsp;|&nbsp; chunkSize={{ currentRow.chunkSize }} &nbsp;|&nbsp; overlap={{ currentRow.overlap }}
          </template>
        </div>
      </div>
      <div class="detail-section">
        <div class="detail-label">文档全文（{{ currentRow.content ? currentRow.content.length : 0 }} 字）</div>
        <div class="detail-content content-box">{{ currentRow.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { uploadDoc, listDoc, getDoc, delDoc } from '@/api/ai/knowledge'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const uploading = ref(false)
const list = ref([])
const total = ref(0)
const uploadVisible = ref(false)
const detailVisible = ref(false)
const currentRow = ref({})
const selectedFile = ref(null)
const categories = ref([])

const queryParams = reactive({ pageNum: 1, pageSize: 10, title: '', category: '', chunked: '' })
const uploadForm = reactive({ category: '', doChunk: true, chunkSize: 300, overlap: 50 })

onMounted(() => getList())

function getList() {
  loading.value = true
  const params = { ...queryParams }
  // 空字符串不传
  Object.keys(params).forEach(k => { if (params[k] === '') delete params[k] })
  listDoc(params).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
    // 收集分类
    const cats = new Set(categories.value)
    list.value.forEach(r => { if (r.category) cats.add(r.category) })
    categories.value = [...cats]
  }).finally(() => loading.value = false)
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

function handleFileRemove() {
  selectedFile.value = null
}

function handleAdd() {
  // 重置表单
  uploadForm.category = ''
  uploadForm.doChunk = true
  uploadForm.chunkSize = 300
  uploadForm.overlap = 50
  selectedFile.value = null
  uploadVisible.value = true
}

async function handleUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('category', uploadForm.category || '')
    formData.append('doChunk', uploadForm.doChunk)
    formData.append('chunkSize', uploadForm.chunkSize || 300)
    formData.append('overlap', uploadForm.overlap || 50)

    const res = await uploadDoc(formData)
    if (res.code === 200) {
      ElMessage.success(`上传成功！${res.data.chunked ? '切为 ' + res.data.chunks + ' 个切片' : '未切片'}`)
      uploadVisible.value = false
      selectedFile.value = null
      getList()
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败: ' + (e.message || '网络错误'))
  } finally {
    uploading.value = false
  }
}

function handleQuery() { queryParams.pageNum = 1; getList() }

function resetQuery() {
  queryParams.title = ''; queryParams.category = ''; queryParams.chunked = ''; queryParams.pageNum = 1
  getList()
}

function handleView(row) {
  getDoc(row.id).then(res => {
    currentRow.value = res.data || res
    detailVisible.value = true
  })
}

function handleDelete(row) {
  const hasChunks = row.chunked === '1'
  const msg = hasChunks
    ? `确定删除「${row.title}」？\n该文档已切片，删除后 Milvus 向量库中的 ${row.chunkCount} 个切片也将一并删除。`
    : `确定删除「${row.title}」？`

  ElMessageBox.confirm(msg, '提示', { type: 'warning', confirmButtonText: '确认删除' }).then(() => {
    delDoc(row.id).then(res => {
      if (res.code === 200) {
        ElMessage.success(`已删除「${row.title}」${res.data.chunksDeleted ? '（含向量切片）' : ''}`)
        getList()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
  })
}
</script>

<style scoped>
.search-form { margin-top: 8px; }
.form-tip { margin-left: 8px; font-size: 12px; color: #909399; }
.detail-section { margin-bottom: 14px; }
.detail-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.detail-content { font-size: 14px; color: #303133; line-height: 1.6; }
.content-box { background: #f5f7fa; padding: 12px; border-radius: 6px; white-space: pre-wrap; max-height: 400px; overflow-y: auto; }
.link-type { color: #409EFF; cursor: pointer; }
.link-type:hover { text-decoration: underline; }
</style>
