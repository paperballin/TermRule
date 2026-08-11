package com.language.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleDTO {
    private String oldWord;
    private String newWord;
    private String sentence;
    private String reason;

    private String sourceTerm;
    private String definition;
    private String discipline;
    private String oldTermYear;
    private String newTermYear;

    private String sourceUrl;
}
