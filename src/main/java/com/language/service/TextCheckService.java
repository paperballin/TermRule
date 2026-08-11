package com.language.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.language.config.DeepSeekConfig;
import com.language.dto.CheckItem;
import com.language.entity.Rule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文本校验服务（方案二）。
 *
 * 流程：
 *   1. 用规则库中的 oldWord 逐个匹配用户输入的文本
 *   2. 对每个匹配到的结果，AI 做语境判断
 *   3. 返回判断结果，前端高亮显示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TextCheckService {

    private final RuleService ruleService;
    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper mapper = new ObjectMapper();

    private final org.springframework.web.client.RestClient aiRestClient =
            org.springframework.web.client.RestClient.builder()
                    .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                        setConnectTimeout(15000);
                        setReadTimeout(60000);
                    }})
                    .build();

    private static final String CONTEXT_PROMPT = """
            你是中文用语规范校对助手。我会给你一个词和它出现的上下文句子。
            请判断：在这个具体句子里，这个词是否属于非规范用法，需要替换为规范用语？

            判断依据：
            - 如果这个词在此语境下确实应该替换为规范用语，返回 needsFix=true
            - 如果这个词在此语境下是正确的用法（如"台风登陆"中的"登陆"是气象术语，不是"登录"的错误写法），返回 needsFix=false
            - 给出简短的判断理由

            以 JSON 格式返回：{"needsFix": true/false, "note": "..."}
            不要输出其他内容。
            """;

    /**
     * 校验文本：规则匹配 + AI 语境判断。
     */
    public CheckItem[] checkText(String text) {
        List<Rule> allRules = ruleService.listAll();
        if (allRules.isEmpty()) {
            return new CheckItem[0];
        }

        List<CheckItem> items = new ArrayList<>();
        for (Rule rule : allRules) {
            String oldWord = rule.getOldWord();
            if (oldWord == null || oldWord.isBlank()) continue;

            int fromIndex = 0;
            while (true) {
                int pos = text.indexOf(oldWord, fromIndex);
                if (pos < 0) break;

                int ctxStart = Math.max(0, pos - 20);
                int ctxEnd = Math.min(text.length(), pos + oldWord.length() + 20);
                String context = text.substring(ctxStart, ctxEnd);

                CheckItem item = new CheckItem();
                item.setOldWord(oldWord);
                item.setNewWord(rule.getNewWord());
                item.setPosition(pos);
                item.setContext(context);
                item.setReason(rule.getReason());
                item.setSourceUrl(rule.getSourceUrl());

                items.add(item);
                fromIndex = pos + oldWord.length();
            }
        }

        if (items.isEmpty()) {
            return new CheckItem[0];
        }

        boolean mock = isMockMode();
        for (CheckItem item : items) {
            if (mock) {
                item.setNeedsFix(true);
                item.setAiNote("Mock模式：未做语境判断，默认需要修改");
            } else {
                try {
                    aiJudgeContext(item);
                } catch (Exception e) {
                    log.error("AI 语境判断失败: {}", e.getMessage());
                    item.setNeedsFix(true);
                    item.setAiNote("AI判断失败，默认需要修改");
                }
            }
        }

        return items.toArray(new CheckItem[0]);
    }

    private void aiJudgeContext(CheckItem item) throws Exception {
        String userPrompt = "词：「" + item.getOldWord() + "」\n"
                + "规范用语：「" + item.getNewWord() + "」\n"
                + "上下文：「" + item.getContext() + "」\n"
                + "请判断这个词在此语境下是否需要替换。";

        String baseUrl = deepSeekConfig.getBaseUrl() != null
                ? deepSeekConfig.getBaseUrl()
                : "https://api.deepseek.com/v1";
        String model = deepSeekConfig.getModel() != null
                ? deepSeekConfig.getModel()
                : "deepseek-chat";

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", CONTEXT_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object")
        );

        String response = aiRestClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .header("Content-Type", "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        var root = mapper.readTree(response);
        var content = root.path("choices").path(0).path("message").path("content").asText("");

        String json = content;
        if (json.trim().startsWith("```")) {
            int start = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (start > 0 && end > start) {
                json = json.substring(start + 1, end).trim();
            }
        }

        var result = mapper.readTree(json);
        item.setNeedsFix(result.path("needsFix").asBoolean(true));
        item.setAiNote(result.path("note").asText(""));
    }

    private boolean isMockMode() {
        return deepSeekConfig.getApiKey() == null
                || deepSeekConfig.getApiKey().isBlank()
                || "mock".equalsIgnoreCase(deepSeekConfig.getApiKey());
    }
}
