package com.language.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.language.config.TerminologyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 术语在线 (termonline.cn) 服务：查询官方审定术语。
 *
 * 数据来源：全国科学技术名词审定委员会管理的术语在线平台，
 * 覆盖100+学科、超百万条规范术语。
 *
 * 当未配置 API 凭证（appid/secret）时，自动降级为 Mock 模式。
 *
 * API 文档：https://www.termonline.cn/platFormDataOpenness
 */
@Slf4j
@Service
public class TerminologyService {

    private static final String API_URL = "https://api.termonline.cn/main-serve/business/tm/tmWord/appSearch";

    private final TerminologyConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    private final RestClient restClient = RestClient.builder()
            .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                setConnectTimeout(10000);
                setReadTimeout(15000);
            }})
            .build();

    public TerminologyService(TerminologyConfig config) {
        this.config = config;
    }

    /**
     * 术语查询结果
     */
    public record TermResult(
            String term,         // 中文术语
            String english,      // 英文术语
            String definition,   // 定义
            String discipline,   // 所属学科
            String publishYear,  // 公布年度
            String source,       // 数据来源
            String sourceUrl,    // 来源链接（术语在线搜索页）
            boolean mock         // 是否为 Mock 数据
    ) {}

    /**
     * 生成术语在线搜索页 URL。
     */
    private String buildSourceUrl(String term) {
        return "https://www.termonline.cn/search?searchText=" +
                URLEncoder.encode(term, StandardCharsets.UTF_8);
    }

    /**
     * 查询术语：调用术语在线 API 获取官方审定术语。
     * 未配置凭证时返回 Mock 数据。
     */
    public TermResult searchTerm(String term) {
        if (!isConfigured()) {
            log.info("术语在线未配置凭证，使用 Mock 模式: {}", term);
            return mockSearch(term);
        }

        try {
            String url = API_URL + "?term=" +
                    URLEncoder.encode(term, StandardCharsets.UTF_8) +
                    "&match=1&appid=" + config.getAppid() +
                    "&secret=" + config.getSecret() +
                    "&pageNo=1&pageSize=1";

            log.info("调用术语在线 API: {}", term);

            String json = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(String.class);

            return parseResponse(json, term);
        } catch (Exception e) {
            log.error("术语在线 API 调用失败，降级为 Mock 模式: {}", e.getMessage());
            return mockSearch(term);
        }
    }

    /**
     * 是否已配置 API 凭证。
     */
    public boolean isConfigured() {
        return config.getAppid() != null && !config.getAppid().isBlank()
                && config.getSecret() != null && !config.getSecret().isBlank();
    }

    /**
     * 解析 API 返回的 JSON。
     */
    private TermResult parseResponse(String json, String term) {
        try {
            JsonNode root = mapper.readTree(json);
            boolean success = root.path("success").asBoolean(false);
            if (!success) {
                log.warn("术语在线 API 返回失败: {}", json);
                return mockSearch(term);
            }

            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isEmpty()) {
                log.warn("术语在线未找到术语: {}", term);
                return mockSearch(term);
            }

            JsonNode first = data.isArray() ? data.get(0) : data;
            return new TermResult(
                    first.path("chinese").asText(term),
                    first.path("english").asText(""),
                    first.path("definition").asText(""),
                    first.path("domain").asText(""),
                    first.path("publishyear").asText(first.path("year").asText("")),
                    first.path("source").asText("术语在线"),
                    buildSourceUrl(term),
                    false
            );
        } catch (Exception e) {
            log.error("解析术语在线响应失败: {}", e.getMessage());
            return mockSearch(term);
        }
    }

    /**
     * Mock 数据：未配置凭证或 API 不可用时返回预设术语。
     * 公布年度参考全国科技名词委实际审定时间。
     */
    private TermResult mockSearch(String term) {
        String url = buildSourceUrl(term);
        return switch (term) {
            case "艾滋病" -> new TermResult(
                    "艾滋病", "AIDS",
                    "由人类免疫缺陷病毒（HIV）引起的获得性免疫缺陷综合征。",
                    "医学", "2002", "术语在线Mock", url, true);
            case "互联网" -> new TermResult(
                    "互联网", "Internet",
                    "全球性的、由采用TCP/IP协议簇的计算机互联网络组成的网络。",
                    "计算机科学技术", "2002", "术语在线Mock", url, true);
            case "因特网" -> new TermResult(
                    "因特网", "Internet",
                    "采用TCP/IP协议簇的计算机互联网络。（注：现规范术语为「互联网」）",
                    "计算机科学技术", "1997", "术语在线Mock", url, true);
            case "登录" -> new TermResult(
                    "登录", "login",
                    "用户通过身份验证进入计算机系统或网络服务的过程。",
                    "计算机科学技术", "2002", "术语在线Mock", url, true);
            case "登陆" -> new TermResult(
                    "登陆", "landing",
                    "泛指从水面或空中到达陆地。（注：与「登录」是不同含义的词）",
                    "大气科学", "1996", "术语在线Mock", url, true);
            case "坏账" -> new TermResult(
                    "坏账", "bad debt",
                    "无法收回或收回可能性极小的应收款项。",
                    "经济学", "2002", "术语在线Mock", url, true);
            case "呆账" -> new TermResult(
                    "呆账", "bad debt",
                    "逾期已久、难以收回的应收款项。（注：现规范术语为「坏账」）",
                    "经济学", "1993", "术语在线Mock", url, true);
            default -> new TermResult(
                    term, "",
                    "（Mock模式）请配置术语在线API凭证以获取真实术语定义。",
                    "通用", "", "术语在线Mock", url, true);
        };
    }
}
