package com.language.controller;

import com.language.dto.*;
import com.language.entity.Rule;
import com.language.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API 控制器。
 *
 * 端点：
 *   POST   /api/analyze        查询官方术语信息
 *   POST   /api/generate       AI 辅助生成示例句和规范原因
 *   POST   /api/check          文本校验（规则匹配 + AI 语境判断）
 *   GET    /api/rules           查询所有已保存规则
 *   GET    /api/rules/term      按术语查询规则
 *   POST   /api/rules           批量保存规则
 *   DELETE /api/rules/{id}      删除规则
 *   GET    /api/rules/export    导出规则为 XML
 *   GET    /api/health          健康检查
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RuleController {

    private final TerminologyService terminologyService;
    private final AiService aiService;
    private final RuleService ruleService;
    private final LanguageToolExportService exportService;
    private final TextCheckService textCheckService;

    /**
     * 查询官方术语信息。
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(@RequestBody AnalyzeRequest request) {
        String term = request.getTerm();
        log.info("查询术语: {}", term);

        TerminologyService.TermResult termResult = terminologyService.searchTerm(term);

        AnalyzeResponse response = new AnalyzeResponse();
        response.setTerm(termResult.term());
        response.setEnglish(termResult.english());
        response.setDefinition(termResult.definition());
        response.setDiscipline(termResult.discipline());
        response.setPublishYear(termResult.publishYear());
        response.setSourceUrl(termResult.sourceUrl());
        response.setMock(termResult.mock());

        return ResponseEntity.ok(response);
    }

    /**
     * AI 辅助生成示例句和规范原因。
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(@RequestBody Map<String, String> body) {
        String oldWord = body.get("oldWord");
        String newWord = body.get("newWord");
        String definition = body.get("definition");
        String oldYear = body.get("oldYear");
        String newYear = body.get("newYear");

        if (oldWord == null || oldWord.isBlank() || newWord == null || newWord.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("AI 辅助生成: {} → {}", oldWord, newWord);
        Map<String, String> result = aiService.generateSentenceAndReason(
                oldWord, newWord, definition, oldYear, newYear);
        result.put("mock", String.valueOf(aiService.isMockMode()));
        return ResponseEntity.ok(result);
    }

    /**
     * 文本校验。
     */
    @PostMapping("/check")
    public ResponseEntity<CheckResponse> checkText(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("文本校验，文本长度: {}", text.length());
        CheckItem[] items = textCheckService.checkText(text);

        long actualIssues = java.util.Arrays.stream(items).filter(CheckItem::isNeedsFix).count();

        CheckResponse response = new CheckResponse();
        response.setText(text);
        response.setTotalMatches(items.length);
        response.setActualIssues((int) actualIssues);
        response.setItems(java.util.Arrays.asList(items));
        response.setMock(aiService.isMockMode());

        return ResponseEntity.ok(response);
    }

    /**
     * 查询所有已保存规则。
     */
    @GetMapping("/rules")
    public ResponseEntity<List<Rule>> listRules() {
        return ResponseEntity.ok(ruleService.listAll());
    }

    /**
     * 按术语查询规则。
     */
    @GetMapping("/rules/term")
    public ResponseEntity<List<Rule>> findByTerm(@RequestParam String term) {
        return ResponseEntity.ok(ruleService.findByTerm(term));
    }

    /**
     * 批量保存规则到数据库。
     */
    @PostMapping("/rules")
    public ResponseEntity<SaveResult> saveRules(@RequestBody List<RuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (rules.size() > 100) {
            return ResponseEntity.badRequest().build();
        }
        try {
            SaveResult result = ruleService.saveRules(rules);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除规则。
     */
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<String> deleteRule(@PathVariable Long id) {
        boolean ok = ruleService.deleteById(id);
        return ok
                ? ResponseEntity.ok("删除成功")
                : ResponseEntity.notFound().build();
    }

    /**
     * 导出规则为 XML 格式。
     */
    @GetMapping("/rules/export")
    public ResponseEntity<String> exportRules(
            @RequestParam(value = "ids", required = false) String ids) {

        List<Rule> rules;
        if (ids != null && !ids.isBlank()) {
            try {
                List<Long> idList = java.util.Arrays.stream(ids.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .toList();
                rules = ruleService.findAllById(idList);
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body("ids 参数格式错误，请传入逗号分隔的数字");
            }
        } else {
            rules = ruleService.listAll();
        }

        if (rules.isEmpty()) {
            return ResponseEntity.badRequest().body("没有可导出的规则");
        }

        String xml = exportService.generateXml(rules);
        log.info("导出 {} 条规则为 XML", rules.size());

        return ResponseEntity.ok()
                .header("Content-Type", "application/xml; charset=UTF-8")
                .header("Content-Disposition",
                        "attachment; filename=\"termrule-rules.xml\"")
                .body(xml);
    }

    /**
     * 健康检查。
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
