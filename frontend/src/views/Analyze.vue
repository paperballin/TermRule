<template>
  <div class="analyze-wrap">
    <!-- Hero: title + search bar, centered when no results -->
    <div :class="['hero', { 'has-result': rules.length > 0 }]" :style="heroStyle">
      <h1 class="title">TermRule</h1>
      <p class="subtitle">术语规范规则生成系统</p>

      <!-- Search bar -->
      <div class="search-bar">
        <el-input
          v-model="term"
          placeholder="输入规范术语关键词（如：互联网、艾滋病、登录）"
          style="flex: 1"
          @keyup.enter="handleSearch"
        />
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSearch"
          style="background: var(--accent-bg); border-color: var(--accent-bg)"
        >
          <el-icon v-if="!loading"><Search /></el-icon>
          查询
        </el-button>
      </div>
    </div>

    <!-- Term info from termonline -->
    <el-card v-if="termInfo" shadow="never" class="term-card">
      <div class="term-header">
        <span class="term-name">{{ termInfo.term }}</span>
        <span v-if="termInfo.english" class="term-en">{{ termInfo.english }}</span>
        <el-tag v-if="termInfo.discipline" size="small" effect="plain" style="color: var(--text-muted); border-color: var(--border-color)">
          {{ termInfo.discipline }}
        </el-tag>
        <el-tag v-if="termInfo.publishYear" type="success" size="small" effect="plain">
          公布年度：{{ termInfo.publishYear }}
        </el-tag>
        <el-tag v-if="termInfo.mock" type="warning" size="small" effect="plain">Mock</el-tag>
      </div>
      <div v-if="termInfo.definition" class="term-def">
        定义：{{ termInfo.definition }}
      </div>
      <a v-if="termInfo.sourceUrl" :href="termInfo.sourceUrl" target="_blank" class="term-source">
        来源：术语在线 ↗
      </a>
    </el-card>

    <!-- Old term query -->
    <el-card v-if="termInfo" shadow="never" class="old-term-card">
      <template #header>
        <span style="color: var(--text-primary)">查询旧用语（对比公布年度）</span>
      </template>
      <div class="old-term-row">
        <el-input
          v-model="oldTermQuery"
          placeholder="输入旧用语（如：因特网）"
          style="width: 200px"
          @keyup.enter="handleSearchOldTerm"
        />
        <el-button
          :loading="loadingOldTerm"
          @click="handleSearchOldTerm"
          style="background: var(--bg-table-header); border-color: var(--border-color); color: var(--text-secondary)"
        >
          查询旧用语
        </el-button>
        <template v-if="oldTermInfo">
          <el-tag :type="oldTermInfo.publishYear ? 'info' : 'info'" size="small" effect="plain">
            {{ oldTermInfo.term }} | 公布年度：{{ oldTermInfo.publishYear || '未知' }}
          </el-tag>
        </template>
      </div>
      <div v-if="oldTermInfo && termInfo.publishYear && oldTermInfo.publishYear" class="year-compare">
        <span class="year-old">{{ oldTermInfo.term }}（{{ oldTermInfo.publishYear }}年）</span>
        <span class="year-arrow">→</span>
        <span class="year-new">{{ termInfo.term }}（{{ termInfo.publishYear }}年）</span>
        <span v-if="parseInt(termInfo.publishYear) >= parseInt(oldTermInfo.publishYear)" class="year-note">
          ✓ 规范术语公布年度更新，符合用语更新条件
        </span>
        <span v-else class="year-warn">
          ⚠ 规范术语公布年度更早，请检查是否选择正确
        </span>
      </div>
    </el-card>

    <!-- Rule input row -->
    <el-card v-if="showRuleInput" shadow="never" class="rule-card">
      <template #header>
        <span style="color: var(--text-primary)">添加规则</span>
      </template>

      <div class="rule-input-row">
        <el-input v-model="newRule.oldWord" placeholder="旧用语（如：因特网）" style="width: 200px" />
        <span class="arrow-text">→</span>
        <el-input v-model="newRule.newWord" placeholder="规范术语（如：互联网）" style="width: 200px" />
        <el-button
          :loading="generating"
          @click="handleGenerate"
          style="background: var(--accent-bg); border-color: var(--accent-bg); color: #fff"
        >
          <el-icon v-if="!generating"><MagicStick /></el-icon>
          AI 生成示例句
        </el-button>
        <el-button
          @click="addRule"
          :disabled="!newRule.oldWord || !newRule.newWord"
          style="background: var(--save-btn-bg); border-color: var(--save-btn-bg); color: #fff"
        >
          添加规则
        </el-button>
      </div>

      <div v-if="newRule.sentence" class="generated-preview">
        <div class="preview-label">AI 生成的示例句和规范原因：</div>
        <div class="preview-sentence">{{ newRule.sentence }}</div>
        <div class="preview-reason">{{ newRule.reason }}</div>
      </div>
    </el-card>

    <!-- Rules table -->
    <el-card v-if="rules.length > 0" shadow="never" class="result-card">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between">
          <span style="color: var(--text-primary)">
            已添加 {{ rules.length }} 条规则
            <el-tag v-if="generating" type="warning" size="small" effect="plain" style="margin-left: 8px">Mock</el-tag>
          </span>
          <el-button
            size="small"
            :disabled="selectedRows.length === 0"
            @click="handleSave"
            :loading="saving"
            style="background: var(--save-btn-bg); border-color: var(--save-btn-bg); color: #fff"
          >
            存入数据库 ({{ selectedRows.length }})
          </el-button>
        </div>
      </template>
      <el-table
        :data="rules"
        @selection-change="selectedRows = $event"
        style="background: transparent"
        :header-cell-style="{ background: 'var(--bg-table-header)', color: 'var(--text-muted)', borderColor: 'var(--border-color)' }"
        :cell-style="{ background: 'var(--bg-table-cell)', color: 'var(--text-secondary)', borderColor: 'var(--border-color)' }"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="oldWord" label="旧用语" width="130">
          <template #default="{ row }">
            <span style="color: var(--old-word-color); text-decoration: line-through">{{ row.oldWord }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="newWord" label="规范用语" width="130">
          <template #default="{ row }">
            <span style="color: var(--accent); font-weight: 500">{{ row.newWord }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sentence" label="示例句子" show-overflow-tooltip />
        <el-table-column prop="reason" label="规范原因" width="220">
          <template #default="{ row }">
            <a v-if="row.sourceUrl" :href="row.sourceUrl" target="_blank"
               style="color: var(--link-color); text-decoration: none; cursor: pointer"
               :title="row.sourceUrl">
              {{ row.reason }}
            </a>
            <span v-else>{{ row.reason }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button size="small" type="danger" plain @click="rules.splice($index, 1)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Government reference links -->
    <div class="ref-links">
      <div class="ref-title">参考资源：以下官方网站可查阅用语更新与术语规范</div>
      <div class="ref-list">
        <a href="https://www.termonline.cn" target="_blank" class="ref-link">
          术语在线<span class="ref-desc">· 术语查询</span>
        </a>
        <a href="https://www.cnterm.cn" target="_blank" class="ref-link">
          全国名词委<span class="ref-desc">· 新词发布</span>
        </a>
        <a href="http://www.gov.cn" target="_blank" class="ref-link">
          中国政府网<span class="ref-desc">· 政策法规</span>
        </a>
        <a href="http://www.moe.gov.cn" target="_blank" class="ref-link">
          教育部<span class="ref-desc">· 语言文字</span>
        </a>
        <a href="http://www.xinhuanet.com" target="_blank" class="ref-link">
          新华社<span class="ref-desc">· 规范用语</span>
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { analyze, generate, saveRules } from '../api'

const term = ref('')
const loading = ref(false)
const loadingOldTerm = ref(false)
const generating = ref(false)
const saving = ref(false)
const termInfo = ref(null)
const oldTermInfo = ref(null)
const oldTermQuery = ref('')
const rules = ref([])
const selectedRows = ref([])

const newRule = ref({
  oldWord: '',
  newWord: '',
  sentence: '',
  reason: '',
  sourceUrl: ''
})

const showRuleInput = computed(() => termInfo.value !== null)

const heroStyle = computed(() => rules.value.length > 0
  ? { paddingTop: '20px' }
  : { minHeight: '50vh', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'stretch' }
)

// 查询规范术语
async function handleSearch() {
  if (!term.value.trim()) {
    ElMessage.warning('请输入术语关键词')
    return
  }
  loading.value = true
  newRule.value = { oldWord: '', newWord: '', sentence: '', reason: '', sourceUrl: '' }

  try {
    const res = await analyze(term.value.trim())
    termInfo.value = res.data
    newRule.value.newWord = res.data.term || term.value.trim()
    ElMessage.success('查询完成')
  } catch (err) {
    ElMessage.error('查询失败: ' + (err.response?.data || err.message))
  } finally {
    loading.value = false
  }
}

// 查询旧用语
async function handleSearchOldTerm() {
  if (!oldTermQuery.value.trim()) {
    ElMessage.warning('请输入旧用语')
    return
  }
  loadingOldTerm.value = true
  try {
    const res = await analyze(oldTermQuery.value.trim())
    oldTermInfo.value = res.data
    newRule.value.oldWord = oldTermQuery.value.trim()
    ElMessage.success('查询完成')
  } catch (err) {
    ElMessage.error('查询失败: ' + (err.response?.data || err.message))
  } finally {
    loadingOldTerm.value = false
  }
}

// AI 辅助生成示例句
async function handleGenerate() {
  if (!newRule.value.oldWord || !newRule.value.newWord) {
    ElMessage.warning('请先填写旧用语和规范用语')
    return
  }
  generating.value = true
  newRule.value.sentence = ''
  newRule.value.reason = ''

  try {
    const params = {
      oldWord: newRule.value.oldWord,
      newWord: newRule.value.newWord,
      definition: termInfo.value?.definition,
      oldYear: oldTermInfo.value?.publishYear,
      newYear: termInfo.value?.publishYear
    }

    const res = await generate(params)
    newRule.value.sentence = res.data.sentence || ''
    newRule.value.reason = res.data.reason || '用语规范'
    ElMessage.success('AI 生成完成')
  } catch (err) {
    ElMessage.error('生成失败: ' + (err.response?.data || err.message))
  } finally {
    generating.value = false
  }
}

// 添加规则到列表
function addRule() {
  if (!newRule.value.oldWord || !newRule.value.newWord) {
    ElMessage.warning('请填写旧用语和规范用语')
    return
  }

  const rule = {
    oldWord: newRule.value.oldWord,
    newWord: newRule.value.newWord,
    sentence: newRule.value.sentence || '请使用规范用语「' + newRule.value.newWord + '」。',
    reason: newRule.value.reason || '用语规范',
    sourceTerm: termInfo.value?.term || term.value,
    definition: termInfo.value?.definition || '',
    discipline: termInfo.value?.discipline || '',
    oldTermYear: oldTermInfo.value?.publishYear || '',
    newTermYear: termInfo.value?.publishYear || '',
    sourceUrl: newRule.value.sourceUrl || termInfo.value?.sourceUrl || ''
  }

  rules.value.push(rule)

  // 清空输入
  newRule.value.oldWord = ''
  newRule.value.sentence = ''
  newRule.value.reason = ''
  ElMessage.success('已添加规则')
}

// 保存到数据库
async function handleSave() {
  if (selectedRows.value.length === 0) return
  saving.value = true
  try {
    const res = await saveRules(selectedRows.value)
    const result = res.data

    let title = '保存成功'
    let type = 'success'
    let msg = ''

    if (result.saved > 0 && result.skippedDup === 0 && result.skippedInvalid === 0) {
      msg = `成功保存 ${result.saved} 条规则。`
    } else if (result.saved === 0 && result.skippedDup > 0) {
      title = '未保存'
      type = 'warning'
      msg = `选择的 ${result.skippedDup} 条规则已存在于数据库中，未重复保存。\n\n重复规则：\n${result.duplicates.join('\n')}`
    } else {
      title = '部分保存成功'
      type = 'warning'
      let parts = [`成功保存 ${result.saved} 条规则。`]
      if (result.skippedDup > 0) {
        parts.push(`跳过 ${result.skippedDup} 条已存在的重复规则：\n${result.duplicates.join('\n')}`)
      }
      if (result.skippedInvalid > 0) {
        parts.push(`跳过 ${result.skippedInvalid} 条无效规则（旧用语或规范用语为空）。`)
      }
      msg = parts.join('\n\n')
    }

    ElMessageBox.alert(msg.replace(/\n/g, '<br>'), title, {
      dangerouslyUseHTMLString: true,
      type: type,
      confirmButtonText: '确定'
    }).then(() => {
      if (result.saved > 0) {
        ElMessageBox.confirm('是否清空当前结果？', '操作', {
          confirmButtonText: '清空', cancelButtonText: '保留', type: 'info'
        }).then(() => {
          rules.value = []
          selectedRows.value = []
        }).catch(() => {})
      }
    }).catch(() => {})
  } catch (err) {
    ElMessage.error('保存失败: ' + (err.response?.data || err.message))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.analyze-wrap {
  max-width: 900px;
  margin: 0 auto;
  color: var(--text-secondary);
  display: flex;
  flex-direction: column;
  align-items: stretch;
  font-size: 15px;
}

.title {
  color: var(--text-primary);
  font-size: 36px;
  text-align: center;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.subtitle {
  color: var(--text-muted);
  text-align: center;
  margin: 0 0 24px 0;
  font-size: 16px;
}

.search-bar {
  display: flex;
  gap: 12px;
  width: 100%;
}

.search-bar :deep(.el-input__inner) {
  font-size: 15px;
}

.term-card {
  width: 100%;
  margin-top: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.term-header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.term-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--accent);
}

.term-en {
  color: var(--text-muted);
  font-size: 14px;
}

.term-def {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.6;
}

.term-source {
  display: inline-block;
  margin-top: 8px;
  color: var(--link-color);
  font-size: 13px;
  text-decoration: none;
}

.old-term-card {
  width: 100%;
  margin-top: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.old-term-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.year-compare {
  margin-top: 10px;
  padding: 10px 12px;
  background: var(--bg-table-header);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.year-old {
  color: var(--old-word-color);
}

.year-arrow {
  color: var(--text-muted);
}

.year-new {
  color: var(--accent);
  font-weight: 500;
}

.year-note {
  color: var(--accent);
  font-size: 13px;
  margin-left: 8px;
}

.year-warn {
  color: var(--old-word-color);
  font-size: 13px;
  margin-left: 8px;
}

.rule-card {
  width: 100%;
  margin-top: 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.rule-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.arrow-text {
  color: var(--text-muted);
  font-size: 18px;
}

.generated-preview {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-table-header);
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.preview-label {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 6px;
}

.preview-sentence {
  color: var(--text-primary);
  font-size: 15px;
  margin-bottom: 4px;
}

.preview-reason {
  color: var(--text-muted);
  font-size: 14px;
}

.result-card {
  width: 100%;
  margin-top: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
}

.result-card :deep(.el-table) {
  font-size: 15px;
}

.result-card :deep(.el-table .cell) {
  font-size: 15px;
}

/* Reference links section */
.ref-links {
  margin-top: 28px;
  padding: 16px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.ref-title {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 10px;
}

.ref-list {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  justify-content: space-between;
}

.ref-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: var(--bg-table-header);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: var(--link-color);
  text-decoration: none;
  font-size: 13px;
  white-space: nowrap;
  transition: all 0.2s;
}

.ref-link:hover {
  border-color: var(--accent);
  background: var(--bg-card);
}

.ref-desc {
  color: var(--text-muted);
  font-size: 11px;
}
</style>
