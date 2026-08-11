<template>
  <el-container :class="'theme-' + theme" style="height: 100vh">
    <el-header style="background: var(--bg-header); display: flex; align-items: center; justify-content: flex-end; gap: 16px; border-bottom: 1px solid var(--border-color)">
      <div class="page-switcher">
        <button class="page-btn" :class="{ active: page === 'analyze' }" @click="page = 'analyze'">规则生成</button>
        <button class="page-btn" :class="{ active: page === 'check' }" @click="page = 'check'">文本校验</button>
      </div>
      <div class="top-actions">
        <button class="top-btn" @click="historyVisible = true">历史记录</button>
      </div>
      <div class="theme-switcher">
        <button class="theme-btn" :class="{ active: theme === 'light' }" @click="theme = 'light'">明亮</button>
        <button class="theme-btn" :class="{ active: theme === 'dark' }" @click="theme = 'dark'">夜间</button>
        <button class="theme-btn" :class="{ active: theme === 'eye' }" @click="theme = 'eye'">护眼</button>
      </div>
    </el-header>
    <el-main style="background: var(--bg-page); padding: 0; overflow-y: auto">
      <div style="padding: 16px 24px; max-width: 900px; margin: 0 auto">
        <Analyze v-if="page === 'analyze'" />
        <Check v-if="page === 'check'" />
      </div>
    </el-main>
    <!-- History drawer -->
    <el-drawer
      v-model="historyVisible"
      title="历史记录"
      size="85%"
      :with-header="true"
      :direction="'rtl'"
      class="history-drawer"
    >
      <History />
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, watch } from 'vue'
import Analyze from './views/Analyze.vue'
import Check from './views/Check.vue'
import History from './views/History.vue'

const page = ref('analyze')
const historyVisible = ref(false)
const theme = ref(localStorage.getItem('app-theme') || 'dark')

watch(theme, (val) => {
  localStorage.setItem('app-theme', val)
})
</script>

<style>
body { margin: 0; }

/* ========== Theme: Dark (夜间) ========== */
.theme-dark {
  --bg-page: #0f0f1a;
  --bg-header: #1a1a2e;
  --bg-card: #1c1c2e;
  --bg-table-header: #1a1a2e;
  --bg-table-cell: #1c1c2e;
  --border-color: #333;
  --text-primary: #e1e1e1;
  --text-secondary: #d3d1c7;
  --text-muted: #888;
  --accent: #5DCAA5;
  --accent-bg: #0F6E56;
  --link-color: #4A9EFF;
  --tab-color: #888;
  --tab-active: #5DCAA5;
  --old-word-color: #F0997B;
  --arrow-color: #5F5E5A;
  --save-btn-bg: #185FA5;
}

/* ========== Theme: Light (明亮) ========== */
.theme-light {
  --bg-page: #f5f7fa;
  --bg-header: #ffffff;
  --bg-card: #ffffff;
  --bg-table-header: #fafafa;
  --bg-table-cell: #ffffff;
  --border-color: #e4e7ed;
  --text-primary: #303133;
  --text-secondary: #606266;
  --text-muted: #909399;
  --accent: #0F6E56;
  --accent-bg: #0F6E56;
  --link-color: #409EFF;
  --tab-color: #909399;
  --tab-active: #0F6E56;
  --old-word-color: #F56C6C;
  --arrow-color: #c0c4cc;
  --save-btn-bg: #185FA5;
}

/* ========== Theme: Eye-protection (护眼) ========== */
.theme-eye {
  --bg-page: #C7EDCC;
  --bg-header: #d4edd7;
  --bg-card: #e0eee0;
  --bg-table-header: #cde8ce;
  --bg-table-cell: #e0eee0;
  --border-color: #a8c9ab;
  --text-primary: #2d3e2f;
  --text-secondary: #3a4a3c;
  --text-muted: #6a7a6c;
  --accent: #2e7d32;
  --accent-bg: #2e7d32;
  --link-color: #1565C0;
  --tab-color: #6a7a6c;
  --tab-active: #2e7d32;
  --old-word-color: #d35400;
  --arrow-color: #8a9a8c;
  --save-btn-bg: #2e7d32;
}

/* ========== Page switcher ========== */
.page-switcher {
  display: inline-flex;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  margin-right: auto;
}
.page-btn {
  padding: 6px 20px;
  border: none;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.page-btn:hover {
  background: var(--bg-table-header);
}
.page-btn.active {
  background: var(--accent-bg);
  color: #fff;
}

/* ========== Top actions & Theme switcher ========== */
.top-actions, .theme-switcher {
  display: inline-flex;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color);
}
.top-btn, .theme-btn {
  padding: 6px 16px;
  border: none;
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.top-btn:hover, .theme-btn:hover {
  background: var(--bg-table-header);
}
.top-btn:active, .theme-btn.active {
  background: var(--accent-bg);
  color: #fff;
}

/* ========== History drawer compact styling ========== */
.history-drawer .el-drawer__header {
  margin-bottom: 8px;
  padding: 12px 16px 8px;
  color: var(--text-primary);
  font-size: 16px;
}
.history-drawer .el-drawer__body {
  padding: 4px 12px 12px;
}

/* ========== Element Plus component overrides ========== */
.el-input__wrapper {
  background-color: var(--bg-card) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}
.el-input__inner {
  color: var(--text-primary) !important;
  font-size: 15px;
}
.el-input__inner::placeholder {
  color: var(--text-muted) !important;
}
.el-card {
  background-color: var(--bg-card) !important;
  color: var(--text-secondary) !important;
  border-color: var(--border-color) !important;
}
.el-card__header {
  border-bottom-color: var(--border-color) !important;
}
.el-empty__description {
  color: var(--text-muted) !important;
  font-size: 15px;
}
.el-table {
  --el-table-border-color: var(--border-color);
  --el-table-header-bg-color: var(--bg-table-header);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: var(--bg-table-cell);
  --el-table-text-color: var(--text-secondary);
  --el-table-header-text-color: var(--text-muted);
  font-size: 15px;
}
.el-table .cell {
  font-size: 15px;
}
.el-table__empty-text {
  color: var(--text-muted);
  font-size: 15px;
}
.el-button {
  font-size: 14px;
}
.el-button--small {
  font-size: 13px;
}
.el-message {
  font-size: 15px;
}
.el-tag {
  font-size: 13px;
}
.el-textarea__inner {
  background-color: var(--bg-card) !important;
  color: var(--text-primary) !important;
  font-size: 15px;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}
.el-textarea__inner::placeholder {
  color: var(--text-muted) !important;
}
</style>
