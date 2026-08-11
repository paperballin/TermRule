package com.language.service;

import com.language.entity.Rule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LanguageTool XML 规则导出服务。
 *
 * 将数据库中的规则导出为 LanguageTool 可读取的 XML 格式。
 */
@Slf4j
@Service
public class LanguageToolExportService {

    public String generateXml(List<Rule> rules) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rules lang=\"zh\">\n");

        xml.append("  <category id=\"TERM_UPDATE\" name=\"\u7528\u8bed\u66f4\u65b0\u89c4\u5219\">\n");
        for (Rule rule : rules) {
            xml.append(buildRule(rule));
        }
        xml.append("  </category>\n");

        xml.append("</rules>\n");
        return xml.toString();
    }

    private String buildRule(Rule rule) {
        StringBuilder sb = new StringBuilder();
        String ruleId = "LT_RULE_" + rule.getId();
        String name = escapeXml(rule.getOldWord() + "\u2192" + rule.getNewWord());
        String reason = rule.getReason() != null ? rule.getReason() : "\u7528\u8bed\u89c4\u8303";
        String oldWord = escapeXml(rule.getOldWord());
        String newWord = escapeXml(rule.getNewWord());

        String correctSentence = rule.getSentence() != null ? escapeXml(rule.getSentence()) : newWord;
        String incorrectSentence = correctSentence.replace(
                escapeXml(rule.getNewWord()),
                "<marker>" + oldWord + "</marker>"
        );
        if (!incorrectSentence.contains("<marker>")) {
            incorrectSentence = "<marker>" + oldWord + "</marker>";
        }

        sb.append(String.format(
                "    <rule id=\"%s\" name=\"%s\">\n", ruleId, name));
        sb.append("      <pattern>\n");
        sb.append(String.format("        <token>%s</token>\n", oldWord));
        sb.append("      </pattern>\n");
        sb.append(String.format(
                "      <message>%s\uFF1A\u5E94\u4F7F\u7528\u300C%s\u300D</message>\n",
                escapeXml(reason), newWord));
        sb.append(String.format(
                "      <example type=\"incorrect\" correction=\"%s\">\n", newWord));
        sb.append(String.format("        %s\n", incorrectSentence));
        sb.append("      </example>\n");
        sb.append("      <example type=\"correct\">\n");
        sb.append(String.format("        %s\n", correctSentence));
        sb.append("      </example>\n");
        sb.append("    </rule>\n");

        return sb.toString();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
