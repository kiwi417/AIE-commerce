<template>
  <div class="app-container">
    <!-- 搜索 -->
    <el-form :model="queryParams" :inline="true">
      <el-form-item label="问题">
        <el-input v-model="queryParams.question" placeholder="搜索问题" clearable style="width:200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="queryParams.category" placeholder="全部" clearable style="width:130px" @change="handleQuery">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.enabled" placeholder="全部" clearable style="width:100px" @change="handleQuery">
          <el-option label="启用" value="1" />
          <el-option label="禁用" value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" plain icon="Plus" @click="handleAdd" v-hasPermi="['business:faq:add']">新增</el-button>
        <el-button plain icon="Upload" @click="handleImport" v-hasPermi="['business:faq:import']">导入</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="list">
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="问题" :show-overflow-tooltip="true" prop="question" min-width="200" />
      <el-table-column label="回答" :show-overflow-tooltip="true" min-width="220">
        <template #default="{ row }">{{ row.answer ? row.answer.substring(0, 60) + '...' : '-' }}</template>
      </el-table-column>
      <el-table-column label="分类" prop="category" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.category || '通用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.enabled === '1' ? 'success' : 'danger'" size="small">
            {{ row.enabled === '1' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="orderNum" width="60" align="center" />
      <el-table-column label="创建时间" prop="createTime" width="160" align="center" />
      <el-table-column label="操作" width="130" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)" v-hasPermi="['business:faq:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['business:faq:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑FAQ' : '新增FAQ'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="问题" prop="question">
          <el-input v-model="form.question" placeholder="用户可能问的问题" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="回答" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="5" placeholder="标准回答" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="如：商品咨询、售后服务、配送物流" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.enabled">
            <el-radio value="1">启用</el-radio>
            <el-radio value="0">禁用</el-radio>
          </el-radio-group>
          <span class="form-tip">禁用后向量库中的该 FAQ 将被删除</span>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入FAQ" width="520px" append-to-body>
      <el-upload
        ref="uploadRef"
        :action="uploadAction"
        :headers="uploadHeaders"
        :on-success="handleImportSuccess"
        :before-upload="beforeImport"
        accept=".xlsx,.xls"
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽 Excel 文件到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            仅支持 .xlsx/.xls 格式。
            <el-button type="primary" link icon="Download" @click="downloadTemplate">下载模板</el-button>
          </div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { listFaq, getFaq, addFaq, updateFaq, delFaq, importTemplate } from '@/api/ai/faq'
import { getToken } from '@/utils/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()
const uploadAction = import.meta.env.VITE_APP_BASE_API + '/business/faq/importData'
const uploadHeaders = { Authorization: 'Bearer ' + getToken() }

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const importVisible = ref(false)
const isEdit = ref(false)
const categories = ref([])

const queryParams = reactive({ pageNum: 1, pageSize: 10, question: '', category: '', enabled: '' })
const form = reactive({ id: null, question: '', answer: '', category: '', enabled: '1', orderNum: 0 })
const rules = {
  question: [{ required: true, message: '问题不能为空', trigger: 'blur' }],
  answer: [{ required: true, message: '回答不能为空', trigger: 'blur' }]
}

onMounted(() => getList())

function getList() {
  loading.value = true
  const params = {}
  Object.keys(queryParams).forEach(k => { if (queryParams[k] !== '') params[k] = queryParams[k] })
  listFaq(params).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
    const cats = new Set(categories.value)
    list.value.forEach(r => { if (r.category) cats.add(r.category) })
    categories.value = [...cats]
  }).finally(() => loading.value = false)
}

function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() {
  queryParams.question = ''; queryParams.category = ''; queryParams.enabled = ''; queryParams.pageNum = 1
  getList()
}

function handleAdd() {
  isEdit.value = false
  Object.assign(form, { id: null, question: '', answer: '', category: '', enabled: '1', orderNum: 0 })
  formVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  getFaq(row.id).then(res => {
    Object.assign(form, res.data || res)
    formVisible.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) return
    submitting.value = true
    const api = isEdit.value ? updateFaq(form) : addFaq(form)
    api.then(res => {
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '修改成功（向量已同步更新）' : '新增成功（已自动向量化）')
        formVisible.value = false
        getList()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    }).finally(() => submitting.value = false)
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除 FAQ「${row.question}」？删除后向量库中的该条目也将被删除。`, '提示', {
    type: 'warning', confirmButtonText: '确认删除'
  }).then(() => {
    delFaq(row.id).then(res => {
      if (res.code === 200) {
        ElMessage.success('已删除（含向量库）')
        getList()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    })
  })
}

// ── 导入相关 ──

function handleImport() { importVisible.value = true }

function beforeImport(file) {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) { ElMessage.error('仅支持 .xlsx 或 .xls 格式'); return false }
  return true
}

function handleImportSuccess(res) {
  if (res.code === 200) {
    ElMessage.success(res.msg || res.data || '导入成功')
    importVisible.value = false
    getList()
  } else {
    ElMessage.error(res.msg || '导入失败')
  }
}

function downloadTemplate() {
  importTemplate().then(() => {
    window.open(import.meta.env.VITE_APP_BASE_API + '/business/faq/importTemplate', '_blank')
  })
}
</script>

<style scoped>
.form-tip { margin-left: 8px; font-size: 12px; color: #909399; }
</style>
