package com.language.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.language.config.DeepSeekConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * AI 辅助服务。
 *
 * 核心原则：AI 只做语言生成（写示例句子、写规范原因），不做事实判断。
 * 用户自己判定"哪个词是非规范用法"，AI 负责把这个判断润色成完整的规则。
 *
 * 当未配置 DeepSeek API Key 时，返回 Mock 文本。
 */
@Slf4j
@Service
public class AiService {

    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            你是中文术语规范辅助工具。用户会给你一对词：旧术语和规范术语，以及术语定义和公布年度。
            你的任务是：
            1. 写一句包含规范术语的自然中文句子（示例句）
            2. 写一条简短的规范原因说明，注明术语规范的依据

            要求：
            1. 示例句要自然通顺，不要解释，就是一句正常的话
            2. 规范原因控制在30字以内
            3. 以 JSON 格式返回：{"sentence": "...", "reason": "..."}
            4. 不要输出其他任何内容
            """;

    public AiService(DeepSeekConfig deepSeekConfig) {
        this.deepSeekConfig = deepSeekConfig;
    }

    /**
     * 生成示例句子和规范原因。
     *
     * @param oldWord    旧用语/非规范用法
     * @param newWord    新用语/规范术语
     * @param definition 术语定义
     * @param oldYear    旧术语公布年度
     * @param newYear    新术语公布年度
     * @return Map 包含 "sentence" 和 "reason" 两个 key
     */
    public Map<String, String> generateSentenceAndReason(
            String oldWord, String newWord,
            String definition, String oldYear, String newYear) {

        if (isMockMode()) {
            return mockGenerate(oldWord, newWord, oldYear, newYear);
        }

        try {
            String userPrompt = buildPrompt(oldWord, newWord, definition, oldYear, newYear);
            String aiResponse = callDeepSeek(SYSTEM_PROMPT, userPrompt);
            return parseResponse(aiResponse);
        } catch (Exception e) {
            log.error("AI 生成失败，降级为 Mock: {}", e.getMessage());
            return mockGenerate(oldWord, newWord, oldYear, newYear);
        }
    }

    /**
     * 判断是否运行在 Mock 模式。
     */
    public boolean isMockMode() {
        return deepSeekConfig.getApiKey() == null
                || deepSeekConfig.getApiKey().isBlank()
                || "mock".equalsIgnoreCase(deepSeekConfig.getApiKey());
    }

    private String buildPrompt(String oldWord, String newWord,
                               String definition, String oldYear, String newYear) {
        StringBuilder sb = new StringBuilder();
        sb.append("旧术语：").append(oldWord).append("\n");
        sb.append("规范术语：").append(newWord).append("\n");
        if (definition != null && !definition.isBlank()) {
            sb.append("术语定义：").append(definition).append("\n");
        }
        if (oldYear != null && !oldYear.isBlank()) {
            sb.append("旧术语公布年度：").append(oldYear).append("\n");
        }
        if (newYear != null && !newYear.isBlank()) {
            sb.append("规范术语公布年度：").append(newYear).append("\n");
        }
        sb.append("\n请写一句包含「").append(newWord).append("」的示例句，并给出规范原因。");
        return sb.toString();
    }

    private final RestClient aiRestClient = RestClient.builder()
            .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                setConnectTimeout(15000);
                setReadTimeout(60000);
            }})
            .build();

    private String callDeepSeek(String systemPrompt, String userPrompt) {
        String baseUrl = deepSeekConfig.getBaseUrl() != null
                ? deepSeekConfig.getBaseUrl()
                : "https://api.deepseek.com/v1";
        String model = deepSeekConfig.getModel() != null
                ? deepSeekConfig.getModel()
                : "deepseek-chat";
        double temp = deepSeekConfig.getTemperature() > 0
                ? deepSeekConfig.getTemperature() : 0.3;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", temp,
                "response_format", Map.of("type", "json_object")
        );

        log.info("调用 DeepSeek 生成示例句: {}", userPrompt.lines().findFirst().orElse(""));

        return aiRestClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseResponse(String aiResponse) throws Exception {
        var root = mapper.readTree(aiResponse);
        var content = root.path("choices").path(0).path("message").path("content").asText("");

        String json = content;
        if (json.trim().startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) {
                json = json.substring(start + 1, end).trim();
            }
        }

        Map<String, String> result = mapper.readValue(json, Map.class);
        result.putIfAbsent("sentence", "");
        result.putIfAbsent("reason", "用语规范");
        return result;
    }

    /**
     * Mock 生成。
     */
    private Map<String, String> mockGenerate(String oldWord, String newWord,
                                              String oldYear, String newYear) {
        log.info("Mock 模式生成: {} → {}", oldWord, newWord);

        String sentence = "请使用规范用语「" + newWord + "」替代「" + oldWord + "」。";
        String reason = (newYear != null && !newYear.isBlank())
                ? "术语规范（" + newYear + "年公布）"
                : "术语规范";

        return Map.of("sentence", sentence, "reason", reason);
    }
}
