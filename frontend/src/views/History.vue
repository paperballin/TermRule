<template>
  <div class="history-wrap">
    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="card-header">
          <span class="header-title">已保存的规则 ({{ rules.length }})</span>
          <div class="header-actions">
            <el-button
              size="small"
              type="success"
              plain
              @click="exportSelected"
              :disabled="selectedRules.length === 0"
            >
              导出选中 ({{ selectedRules.length }})
            </el-button>
            <el-button
              size="small"
              @click="exportAll"
              :disabled="rules.length === 0"
            >
              全部导出 XML
            </el-button>
            <el-button size="small" @click="loadRules" :loading="loading"
              style="background: var(--bg-table-header); border-color: var(--border-color); color: var(--text-secondary)">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="rules"
        style="background: transparent; width: 100%"
        :header-cell-style="{ background: 'var(--bg-table-header)', color: 'var(--text-muted)', borderColor: 'var(--border-color)' }"
        :cell-style="{ background: 'var(--bg-table-cell)', color: 'var(--text-secondary)', borderColor: 'var(--border-color)' }"
        @selection-change="handleSelectionChange"
        size="default"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="oldWord" label="旧用语" width="120">
          <template #default="{ row }">
            <span style="color: var(--old-word-color); text-decoration: line-through">{{ row.oldWord }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="newWord" label="规范用语" width="120">
          <template #default="{ row }">
            <span style="color: var(--accent); font-weight: 500">{{ row.newWord }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sentence" label="示例句子" show-overflow-tooltip min-width="130" />
        <el-table-column prop="reason" label="规范原因" width="170">
          <template #default="{ row }">
            <a v-if="row.sourceUrl" :href="row.sourceUrl" target="_blank"
               style="color: var(--link-color); text-decoration: none; cursor: pointer"
               :title="row.sourceUrl">
              {{ row.reason }}
            </a>
            <span v-else>{{ row.reason }}</span>
          </template>
        </el-table-column>
        <el-table-column label="公布年度" width="120">
          <template #default="{ row }">
            <span v-if="row.oldTermYear" style="font-size: 13px; color: var(--text-muted)">
              {{ row.oldTermYear }} → {{ row.newTermYear }}
            </span>
            <span v-else style="color: var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="保存时间" width="150">
          <template #default="{ row }">
            {{ row.createdAt?.replace('T', ' ').slice(0, 19) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button
              size="small"
              type="danger"
              plain
              @click="handleDelete(row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="rules.length === 0 && !loading" description="暂无已保存的规则" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRules, deleteRule } from '../api'

const rules = ref([])
const loading = ref(false)
const selectedRules = ref([])

async function loadRules() {
  loading.value = true
  try {
    const res = await getRules()
    rules.value = res.data
  } catch (err) {
    ElMessage.error('加载失败: ' + (err.response?.data || err.message))
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(val) {
  selectedRules.value = val
}

async function exportSelected() {
  const ids = selectedRules.value.map(r => r.id).join(',')
  downloadXml(`?ids=${ids}`)
}

async function exportAll() {
  downloadXml('')
}

async function downloadXml(queryParams) {
  try {
    const response = await fetch(`/api/rules/export${queryParams}`)
    if (!response.ok) {
      const text = await response.text()
      throw new Error(text)
    }
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'termrule-rules.xml'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${selectedRules.value.length || rules.value.length} 条规则`)
  } catch (err) {
    ElMessage.error('导出失败: ' + err.message)
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除这条规则？', '提示', { type: 'warning' })
    await deleteRule(id)
    ElMessage.success('删除成功')
    loadRules()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败: ' + (err.response?.data || err.message))
    }
  }
}

onMounted(loadRules)
</script>

<style scoped>
.history-wrap {
  max-width: 100%;
  margin: 0 auto;
  color: var(--text-secondary);
  font-size: 15px;
  padding: 4px;
}

.history-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.history-card :deep(.el-card__body) {
  padding: 8px;
}

.history-card :deep(.el-table) {
  font-size: 15px;
}

.history-card :deep(.el-table .cell) {
  font-size: 15px;
  padding: 0 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.header-title {
  color: var(--text-primary);
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 8px;
}
</style>
