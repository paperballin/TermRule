USE language_tool_ai;

-- v5.0 简化：只保留用语更新类型，移除 rule_type 和 doc_source
DROP TABLE IF EXISTS rule;

CREATE TABLE IF NOT EXISTS rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_word        VARCHAR(255) NOT NULL COMMENT '非规范/旧用语',
    new_word        VARCHAR(255) NOT NULL COMMENT '规范/新用语',
    sentence        TEXT COMMENT '示例句子',
    reason          VARCHAR(500) COMMENT '规范原因',
    source_term     VARCHAR(255) COMMENT '查询术语（termonline）',
    definition      TEXT COMMENT '术语定义',
    discipline      VARCHAR(100) COMMENT '所属学科',
    old_term_year   VARCHAR(20) COMMENT '旧用语公布年度',
    new_term_year   VARCHAR(20) COMMENT '新用语公布年度',
    source_url      VARCHAR(500) COMMENT '来源链接',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_old_new (old_word, new_word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='术语规范规则表';
