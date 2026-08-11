package com.language.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 术语在线 (termonline.cn) API 配置。
 *
 * 术语在线是全国科学技术名词审定委员会管理的规范术语平台，
 * 提供100多个学科、超百万条规范术语的查询服务。
 *
 * API 接口需申请凭证：
 * 1. 访问 https://www.termonline.cn/termInterface 提交申请
 * 2. 获取 appid 和 secret
 * 3. 填入 application.yml 的 termonline 配置项
 *
 * 未配置凭证时，TerminologyService 将自动降级为 Mock 模式，
 * 返回预设的术语数据，不影响开发和功能演示。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "termonline")
public class TerminologyConfig {

    /** 术语在线 API appid（申请获取） */
    private String appid;

    /** 术语在线 API secret（申请获取） */
    private String secret;

    /** API 地址 */
    private String apiUrl = "https://api.termonline.cn/main-serve/business/tm/tmWord/appSearch";
}
