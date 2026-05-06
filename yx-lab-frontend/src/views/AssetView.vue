<template>
  <div class="content-grid">
    <div class="glass-panel section-block">
      <el-tabs v-model="active">
        <el-tab-pane label="仪器台账" name="inst">
          <div class="toolbar">
            <el-button type="primary" @click="loadInstruments">刷新</el-button>
            <el-button @click="createInstrument">新增仪器</el-button>
          </div>
          <el-table :data="instruments" stripe>
            <el-table-column prop="instrumentName" label="仪器名称" />
            <el-table-column prop="instrumentModel" label="型号" />
            <el-table-column prop="ownerName" label="负责人" />
            <el-table-column prop="instrumentStatus" label="状态" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="文档台账" name="doc">
          <div class="toolbar">
            <el-button type="primary" @click="loadDocuments">刷新</el-button>
            <el-button @click="createDocument">新增文档</el-button>
          </div>
          <el-table :data="documents" stripe>
            <el-table-column prop="documentName" label="文档名称" />
            <el-table-column prop="documentCategory" label="分类" />
            <el-table-column prop="fileType" label="文件格式" />
            <el-table-column prop="fileUrl" label="文件路径" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createDocumentApi, createInstrumentApi, fetchDocumentsApi, fetchInstrumentsApi } from '../api/lab'

const active = ref('inst')
const instruments = ref([])
const documents = ref([])

async function loadInstruments() {
  instruments.value = (await fetchInstrumentsApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function loadDocuments() {
  documents.value = (await fetchDocumentsApi({ pageNum: 1, pageSize: 10 })).records || []
}

async function createInstrument() {
  await createInstrumentApi({
    instrumentName: '台式浊度仪',
    instrumentModel: 'TB-210',
    manufacturer: '阳新仪表',
    ownerName: '化验员',
    instrumentStatus: 'NORMAL',
    storageLocation: '一号实验台'
  })
  ElMessage.success('仪器已新增')
  loadInstruments()
}

async function createDocument() {
  await createDocumentApi({
    documentName: 'GB5749标准摘要',
    documentCategory: '标准规范',
    fileType: 'pdf',
    fileSize: 102400,
    fileUrl: '/uploads/gb5749.pdf'
  })
  ElMessage.success('文档已新增')
  loadDocuments()
}

onMounted(() => {
  loadInstruments()
  loadDocuments()
})
</script>
