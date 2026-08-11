package com.language.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rule")
public class Rule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String oldWord;
    private String newWord;
    private String sentence;
    private String reason;

    /** 查询术语（termonline 查询的关键词） */
    private String sourceTerm;
    /** 术语定义 */
    private String definition;
    /** 所属学科 */
    private String discipline;
    /** 旧用语公布年度（termonline） */
    private String oldTermYear;
    /** 新用语（规范术语）公布年度（termonline） */
    private String newTermYear;

    /** 来源链接（termonline搜索页URL） */
    private String sourceUrl;

    private LocalDateTime createdAt;
}
