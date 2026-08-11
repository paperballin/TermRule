package com.language.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本校验单条结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckItem {
    /** 非规范用词 */
    private String oldWord;
    /** 规范术语 */
    private String newWord;
    /** 在原文中的位置（从0开始） */
    private int position;
    /** 匹配到的原文片段上下文 */
    private String context;
    /** AI 语境判断：true=确实是非规范用法，false=此语境下合法，无需修改 */
    private boolean needsFix;
    /** AI 语境判断说明 */
    private String aiNote;
    /** 规范原因 */
    private String reason;
    /** 来源链接 */
    private String sourceUrl;
}
