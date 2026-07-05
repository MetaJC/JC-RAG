package com.jc.rag.common.security;

import com.jc.rag.common.security.AhoCorasickAutomaton.MatchResult;
import com.jc.rag.common.security.AhoCorasickAutomaton.WordEntry;
import com.jc.rag.common.security.SensitiveFilterResult.Hit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 敏感词过滤引擎 (无状态, 仅依赖自动机引用)。
 * <p>WordEntry.payload 约定: SysSensitiveWordEntity 实例 (admin 模块), 通过反射读 getRiskLevel/getCategory/getAction/getId, common 模块不依赖 domain。
 *
 * @author JC
 */
public final class SensitiveFilterEngine {

    private final AhoCorasickAutomaton automaton;

    public SensitiveFilterEngine(AhoCorasickAutomaton automaton) {
        this.automaton = automaton;
    }

    public SensitiveFilterResult filter(String text) {
        if (text == null || text.isEmpty()) {
            return new SensitiveFilterResult(text, text, SensitiveFilterResult.ACTION_PASS, Collections.emptyList());
        }
        List<MatchResult> matches = automaton.scan(text);
        if (matches.isEmpty()) {
            return new SensitiveFilterResult(text, text, SensitiveFilterResult.ACTION_PASS, Collections.emptyList());
        }
        Set<String> seenWords = new HashSet<>();
        List<Hit> hits = new ArrayList<>();
        String topAction = SensitiveFilterResult.ACTION_PASS;
        int topPriority = priority(SensitiveFilterResult.ACTION_PASS);
        String masked = text;
        boolean needMask = false;
        for (MatchResult m : matches) {
            WordEntry we = m.getEntry();
            String word = we.getWord();
            if (!seenWords.add(word)) continue;
            String riskLevel = reflectString(we.getPayload(), "getRiskLevel");
            String category = reflectString(we.getPayload(), "getCategory");
            String wordAction = reflectString(we.getPayload(), "getAction");
            Long wordId = reflectLong(we.getPayload(), "getId");
            if (riskLevel == null) riskLevel = "MEDIUM";
            if (wordAction == null) wordAction = SensitiveFilterResult.ACTION_DENY;
            hits.add(new Hit(word, riskLevel, category, wordAction, wordId));
            int p = priority(wordAction);
            if (p > topPriority) {
                topPriority = p;
                topAction = wordAction;
            }
            if (SensitiveFilterResult.ACTION_MASK.equals(wordAction)) {
                needMask = true;
            }
        }
        if (needMask) {
            for (Hit h : hits) {
                if (SensitiveFilterResult.ACTION_MASK.equals(h.getAction())) {
                    masked = masked.replace(h.getWord(), repeat('*', h.getWord().length()));
                }
            }
        }
        return new SensitiveFilterResult(text, masked, topAction, hits);
    }

    private int priority(String action) {
        if (SensitiveFilterResult.ACTION_DENY.equals(action)) return 3;
        if (SensitiveFilterResult.ACTION_MASK.equals(action)) return 2;
        if (SensitiveFilterResult.ACTION_ALERT.equals(action)) return 1;
        return 0;
    }

    private static String reflectString(Object obj, String method) {
        if (obj == null) return null;
        try {
            Object v = obj.getClass().getMethod(method).invoke(obj);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long reflectLong(Object obj, String method) {
        if (obj == null) return null;
        try {
            Object v = obj.getClass().getMethod(method).invoke(obj);
            if (v instanceof Number n) return n.longValue();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String repeat(char c, int n) {
        char[] arr = new char[n];
        java.util.Arrays.fill(arr, c);
        return new String(arr);
    }
}
