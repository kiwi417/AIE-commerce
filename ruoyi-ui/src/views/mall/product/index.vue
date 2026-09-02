<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
         <el-form-item label="商品名称" prop="productName">
            <el-input
               v-model="queryParams.productName"
               placeholder="请输入商品名称"
               clearable
               style="width: 240px"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="商品分类" prop="categoryId">
            <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width: 240px">
               <el-option
                  v-for="cat in categoryOptions"
                  :key="cat.categoryId"
                  :label="cat.categoryName"
                  :value="cat.categoryId"
               />
            </el-select>
         </el-form-item>
         <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 240px">
               <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
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
               v-hasPermi="['mall:product:add']"
            >新增</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="success"
               plain
               icon="Edit"
               :disabled="single"
               @click="handleUpdate"
               v-hasPermi="['mall:product:edit']"
            >修改</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['mall:product:remove']"
            >删除</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="productList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="商品编号" align="center" prop="productId" width="90" />
         <el-table-column label="商品名称" align="center" prop="productName" :show-overflow-tooltip="true" />
         <el-table-column label="分类" align="center" prop="categoryName" width="110" />
         <el-table-column label="价格(元)" align="center" prop="price" width="100" />
         <el-table-column label="库存" align="center" prop="stock" width="90">
            <template #default="scope">
               <el-tag :type="scope.row.stock > 0 ? 'success' : 'danger'">{{ scope.row.stock }}</el-tag>
            </template>
         </el-table-column>
         <el-table-column label="单位" align="center" prop="unit" width="70" />
         <el-table-column label="货架区域" align="center" prop="shelfArea" width="110" />
         <el-table-column label="状态" align="center" prop="status" width="90">
            <template #default="scope">
               <el-switch
                  v-model="scope.row.status"
                  active-value="0"
                  inactive-value="1"
                  :disabled="!checkPermi(['mall:product:edit'])"
                  @change="handleStatusChange(scope.row)"
               ></el-switch>
            </template>
         </el-table-column>
         <el-table-column label="更新时间" align="center" prop="updateTime" width="160">
            <template #default="scope">
               <span>{{ parseTime(scope.row.updateTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" width="150" class-name="small-padding fixed-width">
            <template #default="scope">
               <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['mall:product:edit']">修改</el-button>
               <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['mall:product:remove']">删除</el-button>
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
      <el-dialog :title="title" v-model="open" width="650px" append-to-body>
         <el-form ref="productRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="商品名称" prop="productName">
               <el-input v-model="form.productName" placeholder="请输入商品名称" />
            </el-form-item>
            <el-form-item label="商品分类" prop="categoryId">
               <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                  <el-option
                     v-for="cat in categoryOptions"
                     :key="cat.categoryId"
                     :label="cat.categoryName"
                     :value="cat.categoryId"
                  />
               </el-select>
            </el-form-item>
            <el-form-item label="价格(元)" prop="price">
               <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 200px" />
            </el-form-item>
            <el-form-item label="库存" prop="stock">
               <el-input-number v-model="form.stock" :min="0" :precision="0" :step="1" style="width: 200px" />
            </el-form-item>
            <el-form-item label="单位" prop="unit">
               <el-input v-model="form.unit" placeholder="如：包 / 瓶 / 个 / 提" style="width: 200px" />
            </el-form-item>
            <el-form-item label="条码" prop="barcode">
               <el-input v-model="form.barcode" placeholder="请输入商品条码" style="width: 300px" />
            </el-form-item>
            <el-form-item label="货架区域" prop="shelfArea">
               <el-input v-model="form.shelfArea" placeholder="如：A区-01-03" style="width: 300px" />
            </el-form-item>
            <el-form-item label="标签" prop="tags">
               <el-input v-model="form.tags" placeholder="逗号分隔，如：零食,膨化,追剧" />
            </el-form-item>
            <el-form-item label="图片地址" prop="imageUrl">
               <el-input v-model="form.imageUrl" placeholder="请输入图片地址（可留空）" />
            </el-form-item>
            <el-form-item label="状态" prop="status">
               <el-radio-group v-model="form.status">
                  <el-radio v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
               </el-radio-group>
            </el-form-item>
            <el-form-item label="商品描述" prop="description">
               <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
            </el-form-item>
            <el-form-item label="备注" prop="remark">
               <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入内容" />
            </el-form-item>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button type="primary" @click="submitForm">确 定</el-button>
               <el-button @click="cancel">取 消</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="Product">
import { listProduct, getProduct, delProduct, addProduct, updateProduct } from "@/api/mall/product"
import { listCategory } from "@/api/mall/category"
import { checkPermi } from "@/utils/permission"

const { proxy } = getCurrentInstance()

const statusOptions = ref([
  { label: "上架", value: "0" },
  { label: "下架", value: "1" }
])

const productList = ref([])
const categoryOptions = ref([])
const open = ref(false)
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

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
    categoryId: [{ required: true, message: "商品分类不能为空", trigger: "change" }],
    price: [{ required: true, message: "价格不能为空", trigger: "blur" }],
    stock: [{ required: true, message: "库存不能为空", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询商品列表 */
function getList() {
  loading.value = true
  listProduct(queryParams.value).then(response => {
    productList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 加载全部分类（商品表单与搜索下拉共用） */
function getCategoryOptions() {
  listCategory({ pageNum: 1, pageSize: 100 }).then(response => {
    categoryOptions.value = response.rows
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
    productId: undefined,
    productName: undefined,
    categoryId: undefined,
    price: undefined,
    stock: 0,
    unit: undefined,
    barcode: undefined,
    shelfArea: undefined,
    tags: undefined,
    imageUrl: undefined,
    status: "0",
    description: undefined,
    remark: undefined
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

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.productId)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加商品"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const productId = row.productId || ids.value
  getProduct(productId).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改商品"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["productRef"].validate(valid => {
    if (valid) {
      if (form.value.productId != undefined) {
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
  const productIds = row.productId || ids.value
  proxy.$modal.confirm('是否确认删除商品编号为"' + productIds + '"的数据项？').then(function () {
    return delProduct(productIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 上下架状态切换（后台直接保存，失败回滚开关显示） */
function handleStatusChange(row) {
  const newStatus = row.status
  const oldStatus = newStatus === "0" ? "1" : "0"
  updateProduct({ productId: row.productId, status: newStatus }).then(() => {
    proxy.$modal.msgSuccess("状态已更新")
  }).catch(() => {
    row.status = oldStatus
  })
}

getCategoryOptions()
getList()
</script>
