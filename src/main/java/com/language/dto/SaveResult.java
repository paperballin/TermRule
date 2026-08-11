package com.language.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 批量保存结果。
 */
@Data
@AllArgsConstructor
public class SaveResult {
    /** 成功保存条数 */
    private int saved;
    /** 跳过的重复条数 */
    private int skippedDup;
    /** 跳过的无效条数 */
    private int skippedInvalid;
    /** 重复规则的描述列表 */
    private List<String> duplicates;
}
