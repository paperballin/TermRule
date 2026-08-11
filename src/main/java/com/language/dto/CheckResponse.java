package com.language.dto;

import lombok.Data;

import java.util.List;

/**
 * 文本校验响应。
 */
@Data
public class CheckResponse {
    /** 原始文本 */
    private String text;
    /** 匹配到的总条数（AI 判断前） */
    private int totalMatches;
    /** AI 判断后确实需要修改的条数 */
    private int actualIssues;
    /** 校验结果列表 */
    private List<CheckItem> items;
    /** 是否为 Mock 模式 */
    private boolean mock;
}
