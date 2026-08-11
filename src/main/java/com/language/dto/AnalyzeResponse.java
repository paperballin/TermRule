package com.language.dto;

import lombok.Data;

@Data
public class AnalyzeResponse {
    private String term;
    private String english;
    private String definition;
    private String discipline;
    private String publishYear;
    private String sourceUrl;
    private boolean mock;
}
