# TermRule - 术语规范规则生成系统

基于 Spring Boot 3 + Vue 3 的中文术语规范规则生成与校验工具。

## 功能

- **术语查询**：接入[术语在线](https://www.termonline.cn)（全国科学技术名词审定委员会），查询官方术语定义、公布年度等信息
- **规则生成**：输入旧用语和规范术语，AI 辅助生成示例句和规范原因
- **文本校验**：粘贴文本，系统自动用规则库匹配并做 AI 语境判断
- **XML 导出**：将规则导出为 LanguageTool 兼容的 XML 格式

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2 + MyBatis-Plus + MySQL 8.0 |
| 前端 | Vue 3 + Element Plus + Vite |
| AI | DeepSeek API（OpenAI 兼容接口） |
| 数据源 | 术语在线 termonline.cn API |

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+

### 后端

```bash
# 1. 复制配置模板并填入你的配置
cp src/main/resources/application.yml.example src/main/resources/application.yml

# 2. 编译运行
mvn spring-boot:run
```

不配置 DeepSeek API Key 和 termonline 凭证时，系统自动降级为 Mock 模式，可正常体验全部功能。

### 前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`

## 项目结构

```
├── src/main/java/com/language/
│   ├── controller/      # REST API
│   ├── service/         # 业务逻辑
│   ├── entity/          # 数据实体
│   ├── dto/             # 数据传输对象
│   └── config/          # 配置类
├── src/main/resources/
│   ├── application.yml.example  # 配置模板
│   └── schema.sql               # 数据库建表脚本
└── frontend/
    └── src/
        ├── views/       # 页面组件
        └── api/         # API 调用
```

## License

MIT
