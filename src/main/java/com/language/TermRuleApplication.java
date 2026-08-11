package com.language;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.language.mapper")
public class TermRuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(TermRuleApplication.class, args);
    }
}
