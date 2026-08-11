import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 查询官方术语信息（用语更新规则用）
export function analyze(term) {
  return api.post('/analyze', { term })
}

// AI 辅助生成示例句和规范原因
export function generate(params) {
  return api.post('/generate', params)
}

// 文本校验
export function checkText(text) {
  return api.post('/check', { text })
}

// 规则 CRUD
export function getRules() {
  return api.get('/rules')
}

export function saveRules(rules) {
  return api.post('/rules', rules)
}

export function deleteRule(id) {
  return api.delete(`/rules/${id}`)
}

export function healthCheck() {
  return api.get('/health')
}
