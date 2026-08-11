<template>
  <div class="check-wrap">
    <h1 class="title">文本校验</h1>
    <p class="subtitle">粘贴一段中文文本，系统将用规则库匹配并做 AI 语境判断</p>

    <el-input
      v-model="text"
      type="textarea"
      :rows="8"
      placeholder="在此粘贴需要校验的中文文本……"
      style="margin-bottom: 12px"
    />

    <div class="check-actions">
      <el-button
        type="primary"
        :loading="loading"
        @click="handleCheck"
        :disabled="!text.trim()"
        style="background: var(--accent-bg); border-color: var(--accent-bg)"
      >
        <el-icon v-if="!loading"><Search /></el-icon>
        开始校验
      </el-button>
      <el-button @click="text = ''; result = null" :disabled="!text">清空</el-button>
      <span v-if="result" class="result-summary">
        匹配 {{ result.totalMatches }} 处，需修改 {{ result.actualIssues }} 处
        <el-tag v-if="result.mock" type="warning" size="small" effect="plain" style="margin-left: 8px">Mock</el-tag>
      </span>
    </div>

    <!-- Results -->
    <div v-if="result && result.items.length > 0" class="results-list">
      <div
        v-for="(item, idx) in result.items"
        :key="idx"
        class="result-item"
        :class="{ 'needs-fix': item.needsFix, 'no-fix': !item.needsFix }"
      >
        <div class="item-header">
          <span class="item-old" style="text-decoration: line-through">{{ item.oldWord }}</span>
          <span class="item-arrow">→</span>
          <span class="item-new">{{ item.newWord }}</span>
          <el-tag :type="item.needsFix ? 'danger' : 'success'" size="small" effect="plain" style="margin-left: 8px">
            {{ item.needsFix ? '需修改' : '无需修改' }}
          </el-tag>
        </div>
        <div class="item-context">
          上下文：……{{ item.context }}……
        </div>
        <div v-if="item.aiNote" class="item-ai-note">
          AI 判断：{{ item.aiNote }}
        </div>
        <a v-if="item.sourceUrl" :href="item.sourceUrl" target="_blank" class="item-reason">
          {{ item.reason }}
        </a>
        <span v-else class="item-reason">{{ item.reason }}</span>
      </div>
    </div>

    <el-empty
      v-if="result && result.items.length === 0"
      description="未匹配到任何非规范用法"
      style="margin-top: 40px"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { checkText } from '../api'

const text = ref('')
const loading = ref(false)
const result = ref(null)

async function handleCheck() {
  if (!text.value.trim()) return
  loading.value = true
  result.value = null

  try {
    const res = await checkText(text.value)
    result.value = res.data
    ElMessage.success(`校验完成，匹配 ${res.data.totalMatches} 处`)
  } catch (err) {
    ElMessage.error('校验失败: ' + (err.response?.data || err.message))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.check-wrap {
  max-width: 900px;
  margin: 0 auto;
  color: var(--text-secondary);
  font-size: 15px;
  padding-top: 20px;
}

.title {
  color: var(--text-primary);
  font-size: 28px;
  text-align: center;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.subtitle {
  color: var(--text-muted);
  text-align: center;
  margin: 0 0 24px 0;
  font-size: 15px;
}

.check-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.result-summary {
  color: var(--text-muted);
  font-size: 14px;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-item {
  padding: 14px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  border-left: 4px solid var(--border-color);
}

.result-item.needs-fix {
  border-left-color: var(--old-word-color);
}

.result-item.no-fix {
  border-left-color: var(--accent);
}

.item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.item-old {
  color: var(--old-word-color);
  font-size: 15px;
}

.item-arrow {
  color: var(--text-muted);
}

.item-new {
  color: var(--accent);
  font-weight: 500;
  font-size: 15px;
}

.item-context {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 4px;
}

.item-ai-note {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 4px;
}

.item-reason {
  color: var(--link-color);
  font-size: 13px;
  text-decoration: none;
}

:deep(.el-textarea__inner) {
  background-color: var(--bg-card) !important;
  color: var(--text-primary) !important;
  font-size: 15px;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}
</style>
