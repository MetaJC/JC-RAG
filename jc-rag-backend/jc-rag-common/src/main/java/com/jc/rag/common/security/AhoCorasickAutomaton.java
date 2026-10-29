/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.common.security;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aho-Corasick 多模式匹配自动机 (精简版, 适用 1 万词库规模)。
 * <p>一次构建 O(Σ), 一次扫描 O(n + m), 适合敏感词批量匹配场景。
 * <p>本类为工具类, 不维护状态, 内部用 Map/数组存储, 线程安全由调用方保证 (建议整体不可变 + 全量 reload)。
 *
 * <p>使用示例:
 * <pre>{@code
 * AhoCorasickAutomaton ac = AhoCorasickAutomaton.build(words);
 * List<MatchResult> hits = ac.scan("这是一段包含敏感词1和敏感词2的文本");
 * }</pre>
 *
 * @author JC
 */
public final class AhoCorasickAutomaton {

    /** 自动机根节点 */
    private final Node root;

    private AhoCorasickAutomaton(Node root) {
        this.root = root;
    }

    /**
     * 构建自动机。
     *
     * @param words 关键词列表 (每个元素: [word, payload...])
     * @return 自动机实例
     */
    public static AhoCorasickAutomaton build(List<WordEntry> words) {
        Node root = new Node();
        // 1. 构建 Trie 树
        for (WordEntry w : words) {
            if (w == null || w.getWord() == null || w.getWord().isEmpty()) {
                continue;
            }
            Node cur = root;
            for (char c : w.getWord().toCharArray()) {
                cur = cur.children.computeIfAbsent(c, k -> new Node());
            }
            cur.output.add(w);
        }
        // 2. BFS 构建 fail 指针
        Deque<Node> queue = new ArrayDeque<>();
        root.fail = root;
        for (Node child : root.children.values()) {
            child.fail = root;
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            Node u = queue.poll();
            for (Map.Entry<Character, Node> e : u.children.entrySet()) {
                char c = e.getKey();
                Node v = e.getValue();
                Node f = u.fail;
                while (f != root && !f.children.containsKey(c)) {
                    f = f.fail;
                }
                v.fail = f.children.getOrDefault(c, root);
                v.output.addAll(v.fail.output);
                queue.add(v);
            }
        }
        return new AhoCorasickAutomaton(root);
    }

    /**
     * 扫描文本, 返回所有命中 (按文本出现顺序, 可能有重叠, 调用方自行去重/截断)。
     *
     * @param text 待扫描文本
     * @return 命中列表 (按 start 升序)
     */
    public List<MatchResult> scan(String text) {
        List<MatchResult> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        Node cur = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (cur != root && !cur.children.containsKey(c)) {
                cur = cur.fail;
            }
            cur = cur.children.getOrDefault(c, root);
            for (WordEntry w : cur.output) {
                result.add(new MatchResult(i - w.getWord().length() + 1, w));
            }
        }
        return result;
    }

    /** Trie 节点 */
    private static final class Node {
        final Map<Character, Node> children = new HashMap<>();
        Node fail;
        final List<WordEntry> output = new ArrayList<>();
    }

    /** 关键词条目 (word + 自定义 payload) */
    public static final class WordEntry {
        private final String word;
        private final Object payload;

        public WordEntry(String word, Object payload) {
            this.word = word;
            this.payload = payload;
        }

        public String getWord() {
            return word;
        }

        public Object getPayload() {
            return payload;
        }
    }

    /** 扫描结果 */
    public static final class MatchResult {
        /** 命中起始位置 (含) */
        private final int start;
        /** 命中的词条 */
        private final WordEntry entry;

        public MatchResult(int start, WordEntry entry) {
            this.start = start;
            this.entry = entry;
        }

        public int getStart() {
            return start;
        }

        public WordEntry getEntry() {
            return entry;
        }
    }
}
