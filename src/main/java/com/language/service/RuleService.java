package com.language.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.language.dto.RuleDTO;
import com.language.dto.SaveResult;
import com.language.entity.Rule;
import com.language.mapper.RuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则服务：数据库 CRUD 操作。
 *
 * 去重逻辑：相同的 old_word → new_word 映射只存一条。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleMapper ruleMapper;

    private static final int MAX_BATCH_SIZE = 100;

    @Transactional
    public SaveResult saveRules(List<RuleDTO> ruleDTOs) {
        if (ruleDTOs == null || ruleDTOs.isEmpty()) {
            return new SaveResult(0, 0, 0, List.of());
        }

        if (ruleDTOs.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("单次最多保存 " + MAX_BATCH_SIZE + " 条规则，当前 " + ruleDTOs.size() + " 条");
        }

        int saved = 0;
        int skippedDup = 0;
        int skippedInvalid = 0;
        List<String> duplicates = new ArrayList<>();

        for (RuleDTO dto : ruleDTOs) {
            // 1. 输入校验
            if (isBlank(dto.getOldWord()) || isBlank(dto.getNewWord())) {
                skippedInvalid++;
                continue;
            }

            // 2. 去重：old_word + new_word 已存在则跳过
            boolean exists = ruleMapper.exists(
                    new QueryWrapper<Rule>()
                            .eq("old_word", dto.getOldWord())
                            .eq("new_word", dto.getNewWord())
            );
            if (exists) {
                skippedDup++;
                duplicates.add(dto.getOldWord() + " → " + dto.getNewWord());
                continue;
            }

            // 3. 写入
            Rule rule = new Rule();
            rule.setOldWord(dto.getOldWord());
            rule.setNewWord(dto.getNewWord());
            rule.setSentence(dto.getSentence());
            rule.setReason(dto.getReason());
            rule.setSourceTerm(dto.getSourceTerm());
            rule.setDefinition(dto.getDefinition());
            rule.setDiscipline(dto.getDiscipline());
            rule.setOldTermYear(dto.getOldTermYear());
            rule.setNewTermYear(dto.getNewTermYear());
            rule.setSourceUrl(dto.getSourceUrl());
            rule.setCreatedAt(LocalDateTime.now());
            ruleMapper.insert(rule);
            saved++;
        }

        log.info("保存完成：新增 {} 条，跳过重复 {} 条，跳过无效 {} 条",
                saved, skippedDup, skippedInvalid);
        return new SaveResult(saved, skippedDup, skippedInvalid, duplicates);
    }

    public List<Rule> listAll() {
        return ruleMapper.selectList(
                new QueryWrapper<Rule>().orderByDesc("created_at")
        );
    }

    public List<Rule> findByTerm(String term) {
        return ruleMapper.selectList(
                new QueryWrapper<Rule>().eq("source_term", term)
                        .orderByDesc("created_at")
        );
    }

    public List<Rule> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return ruleMapper.selectBatchIds(ids);
    }

    public boolean deleteById(Long id) {
        return ruleMapper.deleteById(id) > 0;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
