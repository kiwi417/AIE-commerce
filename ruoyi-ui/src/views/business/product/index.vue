<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
         <el-form-item label="商品名称" prop="productName">
            <el-input
               v-model="queryParams.productName"
               placeholder="请输入商品名称"
               clearable
               style="width: 200px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="所属分类" prop="categoryId">
            <el-select
               v-model="queryParams.categoryId"
               placeholder="请选择分类"
               clearable
               style="width: 200px"
            >
               <el-option
                  v-for="item in queryCategoryOptions"
                  :key="item.id"
                  :label="item.categoryName"
                  :value="item.id"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select
               v-model="queryParams.status"
               placeholder="商品状态"
               clearable
               style="width: 200px"
            >
               <el-option
                  v-for="dict in productStatus"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
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
               v-hasPermi="['business:product:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['business:product:remove']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['business:product:export']"
            >导出</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="info"
               plain
               icon="Upload"
               @click="handleImport"
               v-hasPermi="['business:product:import']"
            >导入</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="商品编号" align="center" prop="id" />
         <el-table-column label="商品名称" align="center" prop="productName" :show-overflow-tooltip="true" />
         <el-table-column label="条码" align="center" prop="barcode" :show-overflow-tooltip="true" />
         <el-table-column label="价格" align="center" prop="price" />
         <el-table-column label="库存" align="center" prop="stock" />
         <el-table-column label="单位" align="center" prop="unit" />
         <el-table-column label="货架区域" align="center" prop="shelfArea" :show-overflow-tooltip="true" />
         <el-table-column label="状态" align="center" prop="status">
            <template #default="scope">
               <dict-tag :options="productStatus" :value="scope.row.status" />
            </template>
         </el-table-column>
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['business:product:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['business:product:remove']">删除</el-button>
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

      <!-- 添加或修改商品对话框 -->
      <el-dialog :title="title" v-model="open" width="700px" append-to-body>
         <el-form ref="productRef" :model="form" :rules="rules" label-width="100px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="所属分类" prop="categoryId">
                     <el-select
                        v-model="form.categoryId"
                        placeholder="请选择分类"
                        style="width: 100%"
                     >
                        <el-option
                           v-for="item in formCategoryOptions"
                           :key="item.id"
                           :label="item.categoryName"
                           :value="item.id"
                        />
                     </el-select>
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item label="商品名称" prop="productName">
                     <el-input v-model="form.productName" placeholder="请输入商品名称" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="条码" prop="barcode">
                     <el-input v-model="form.barcode" placeholder="请输入条码" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item label="价格" prop="price">
                     <el-input-number v-model="form.price" :precision="2" :min="0" style="width: 100%" placeholder="请输入价格" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="库存" prop="stock">
                     <el-input-number v-model="form.stock" :min="0" style="width: 100%" placeholder="请输入库存" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item label="单位" prop="unit">
                     <el-input v-model="form.unit" placeholder="请输入单位" />
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="货架区域" prop="shelfArea">
                     <el-input v-model="form.shelfArea" placeholder="请输入货架区域" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="24">
                  <el-form-item label="描述" prop="description">
                     <el-input v-model="form.description" type="textarea" placeholder="请输入描述" />
                  </el-form-item>
               </el-col>
            </el-row>
            <el-row>
               <el-col :span="12">
                  <el-form-item label="状态" prop="status">
                     <el-radio-group v-model="form.status">
                        <el-radio
                           v-for="dict in productStatus"
                           :key="dict.value"
                           :value="dict.value"
                        >{{ dict.label }}</el-radio>
                     </el-radio-group>
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>

      <!-- 导入对话框 -->
      <el-dialog title="商品导入" v-model="importOpen" width="520px" append-to-body>
         <el-upload
            ref="uploadRef"
            :action="uploadAction"
            :headers="uploadHeaders"
            :file-list="fileList"
            drag
            :auto-upload="false"
            :before-upload="handleBeforeUpload"
         >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
               <div class="el-upload__tip">
                  请上传 .xls 或 .xlsx 格式的文件
               </div>
            </template>
         </el-upload>
         <div style="margin-top: 10px">
            <el-button type="primary" icon="Download" @click="downloadTemplate">下载模板</el-button>
         </div>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitUpload">确 定</el-button>
               <el-button @click="importOpen = false">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Product">
import { listProduct, getProduct, addProduct, updateProduct, delProduct, exportProduct, importTemplate } from "@/api/business/product"
import { listCategory } from "@/api/business/category"
import { getToken } from "@/utils/auth"

const { proxy } = getCurrentInstance()
const uploadAction = import.meta.env.VITE_APP_BASE_API + "/business/product/importData"
const uploadHeaders = ref({ Authorization: "Bearer " + getToken() })

const productList = ref([])
const open = ref(false)
const importOpen = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")
const queryCategoryOptions = ref([])
const formCategoryOptions = ref([])
const fileList = ref([])
const uploadRef = ref(null)

const productStatus = ref([
  { label: "上架", value: "0" },
  { label: "下架", value: "1" }
])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    productName: undefined,
    categoryId: undefined,
    status: undefined
  },
  rules: {
    productName: [{ required: true, message: "商品名称不能为空", trigger: "blur" }],
    price: [{ required: true, message: "价格不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 加载所有分类 */
function loadAllCategories() {
  listCategory({ pageNum: 1, pageSize: 999 }).then(response => {
    queryCategoryOptions.value = response.rows
    formCategoryOptions.value = response.rows
  })
}

/** 查询商品列表 */
function getList() {
  loading.value = true
  listProduct(queryParams.value).then(response => {
    productList.value = response.rows
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
    categoryId: undefined,
    productName: undefined,
    barcode: undefined,
    price: undefined,
    stock: 0,
    unit: "件",
    shelfArea: undefined,
    description: undefined,
    status: "0"
  }
  proxy.resetForm("productRef")
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
  title.value = "添加商品"
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
  getProduct(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["productRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addProduct(form.value).then(response => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const productIds = row.id || ids.value
  proxy.$modal.confirm('是否确认删除商品编号为"' + productIds + '"的数据项？').then(function() {
    return delProduct(productIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  proxy.$modal.confirm("是否确认导出所有商品数据项？").then(function() {
    return exportProduct(queryParams.value)
  }).then(response => {
    proxy.$modal.msgSuccess("导出成功")
    proxy.download("/business/product/export", { ...queryParams.value }, `product_${new Date().getTime()}.xlsx`)
  }).catch(() => {})
}

/** 导入按钮操作 */
function handleImport() {
  fileList.value = []
  importOpen.value = true
}

/** 上传前校验 */
function handleBeforeUpload(file) {
  const isExcel = file.name.endsWith('.xls') || file.name.endsWith('.xlsx')
  if (!isExcel) {
    proxy.$modal.msgError("只能上传 .xls 或 .xlsx 格式的文件")
    return false
  }
  return true
}

/** 提交上传 */
function submitUpload() {
  proxy.$refs["uploadRef"].submit()
}

/** 下载导入模板 */
function downloadTemplate() {
  importTemplate().then(response => {
    window.open(import.meta.env.VITE_APP_BASE_API + "/business/product/importTemplate", "_blank")
  })
}

loadAllCategories()
getList()
</script>
