package com.jc.rag.admin.module.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.security.AhoCorasickAutomaton;
import com.jc.rag.common.security.AhoCorasickAutomaton.WordEntry;
import com.jc.rag.common.security.SensitiveFilterEngine;
import com.jc.rag.domain.entity.sys.SysSensitiveWordEntity;
import com.jc.rag.domain.mapper.sys.SysSensitiveWordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 系统敏感词服务 (含 Aho-Corasick 内存引擎)。
 * <p>核心职责:
 * <ol>
 *   <li>落库 CRUD</li>
 *   <li>启动全量加载 + 每 5 分钟定时刷新</li>
 *   <li>增删改后立即 reload (保证 Admin 操作即时生效)</li>
 *   <li>提供 filter(text) 接口给 ChatService / Python 调用</li>
 * </ol>
 *
 * @author JC
 */
@Service
public class SensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordService.class);

    @Autowired
    private SysSensitiveWordMapper mapper;

    /** 自动机原子引用, reload 时整体替换, 读路径无锁 */
    private final AtomicReference<AhoCorasickAutomaton> acRef = new AtomicReference<>(buildEmpty());

    @PostConstruct
    public void init() {
        reload();
    }

    /** 定时刷新 (5 分钟) */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void scheduledReload() {
        reload();
    }

    /**
     * 全量重新加载自动机。
     */
    public synchronized void reload() {
        try {
            List<SysSensitiveWordEntity> list = mapper.selectList(
                new QueryWrapper<SysSensitiveWordEntity>().eq("enabled", 1));
            List<WordEntry> entries = list.stream()
                .map(e -> new WordEntry(e.getWord(), e))
                .collect(Collectors.toList());
            AhoCorasickAutomaton ac = entries.isEmpty()
                ? buildEmpty()
                : AhoCorasickAutomaton.build(entries);
            acRef.set(ac);
            log.info("[SensitiveWord] reloaded {} words", entries.size());
        } catch (Exception e) {
            log.error("[SensitiveWord] reload failed, keep previous state", e);
        }
    }

    private AhoCorasickAutomaton buildEmpty() {
        return AhoCorasickAutomaton.build(Collections.emptyList());
    }

    /**
     * 获取当前生效的敏感词过滤引擎 (供 ChatService 调用)。
     * <p>返回的是不可变快照, 调用方无需加锁, 后续 reload 不影响本次 filter 结果。
     */
    public SensitiveFilterEngine currentEngine() {
        return new SensitiveFilterEngine(acRef.get());
    }

    // ============================== CRUD ==============================

    public IPage<SysSensitiveWordEntity> page(long page, long size, String keyword, String riskLevel) {
        QueryWrapper<SysSensitiveWordEntity> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like("word", keyword.trim());
        }
        if (riskLevel != null && !riskLevel.isBlank()) {
            qw.eq("risk_level", riskLevel);
        }
        qw.orderByDesc("id");
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    public SysSensitiveWordEntity create(SysSensitiveWordEntity entity, Long currentUserId) {
        validate(entity, true);
        if (mapper.selectCount(new QueryWrapper<SysSensitiveWordEntity>().eq("word", entity.getWord())) > 0) {
            throw new AssertException(ResultCode.CONFLICT, "sensitive.word.duplicate");
        }
        entity.setId(null);
        if (entity.getEnabled() == null) entity.setEnabled(1);
        entity.setCreatedBy(currentUserId);
        mapper.insert(entity);
        reload();
        return entity;
    }

    public SysSensitiveWordEntity update(SysSensitiveWordEntity entity) {
        if (entity.getId() == null) {
            throw new AssertException(ResultCode.PARAM_INVALID, "common.id.required");
        }
        validate(entity, false);
        SysSensitiveWordEntity existing = mapper.selectById(entity.getId());
        if (existing == null) {
            throw new AssertException(ResultCode.NOT_FOUND, "sensitive.word.not.found");
        }
        // word 唯一性校验
        if (!existing.getWord().equals(entity.getWord())
            && mapper.selectCount(new QueryWrapper<SysSensitiveWordEntity>().eq("word", entity.getWord())) > 0) {
            throw new AssertException(ResultCode.CONFLICT, "sensitive.word.duplicate");
        }
        existing.setWord(entity.getWord());
        existing.setRiskLevel(entity.getRiskLevel());
        existing.setCategory(entity.getCategory());
        existing.setAction(entity.getAction());
        existing.setEnabled(entity.getEnabled());
        existing.setDescription(entity.getDescription());
        mapper.updateById(existing);
        reload();
        return existing;
    }

    public void delete(Long id) {
        SysSensitiveWordEntity existing = mapper.selectById(id);
        if (existing == null) {
            throw new AssertException(ResultCode.NOT_FOUND, "sensitive.word.not.found");
        }
        mapper.deleteById(id);
        reload();
    }

    public void toggleEnabled(Long id) {
        SysSensitiveWordEntity existing = mapper.selectById(id);
        if (existing == null) {
            throw new AssertException(ResultCode.NOT_FOUND, "sensitive.word.not.found");
        }
        existing.setEnabled(existing.getEnabled() != null && existing.getEnabled() == 1 ? 0 : 1);
        mapper.updateById(existing);
        reload();
    }

    /**
     * 批量导入 (按行 word|risk|action|category 解析)。
     */
    public int batchImport(List<String> lines, Long currentUserId) {
        int count = 0;
        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            String[] parts = trimmed.split("\\|");
            if (parts.length < 2) continue;
            String word = parts[0].trim();
            String risk = parts[1].trim().toUpperCase();
            String action = parts.length >= 3 ? parts[2].trim().toUpperCase() : SysSensitiveWordEntity.ACTION_DENY;
            String category = parts.length >= 4 ? parts[3].trim().toUpperCase() : null;
            String desc = parts.length >= 5 ? parts[4].trim() : null;
            if (mapper.selectCount(new QueryWrapper<SysSensitiveWordEntity>().eq("word", word)) > 0) {
                continue; // 跳过已存在
            }
            SysSensitiveWordEntity e = new SysSensitiveWordEntity();
            e.setWord(word);
            e.setRiskLevel(risk);
            e.setAction(action);
            e.setCategory(category);
            e.setDescription(desc);
            e.setEnabled(1);
            e.setCreatedBy(currentUserId);
            mapper.insert(e);
            count++;
        }
        reload();
        return count;
    }

    /** 导出全部启用词 (供 Python 拉取) */
    public List<SysSensitiveWordEntity> exportEnabled() {
        return mapper.selectList(new QueryWrapper<SysSensitiveWordEntity>().eq("enabled", 1));
    }

    private void validate(SysSensitiveWordEntity e, boolean isCreate) {
        if (e == null) {
            throw new AssertException(ResultCode.PARAM_INVALID, "common.param.invalid");
        }
        if (e.getWord() == null || e.getWord().isBlank()) {
            throw new AssertException(ResultCode.PARAM_INVALID, "sensitive.word.empty");
        }
        if (e.getWord().length() > 100) {
            throw new AssertException(ResultCode.PARAM_INVALID, "sensitive.word.too.long");
        }
        if (!isValidLevel(e.getRiskLevel())) {
            throw new AssertException(ResultCode.PARAM_INVALID, "sensitive.word.invalid.level");
        }
        if (!isValidAction(e.getAction())) {
            throw new AssertException(ResultCode.PARAM_INVALID, "sensitive.word.invalid.action");
        }
    }

    private boolean isValidLevel(String l) {
        return SysSensitiveWordEntity.LEVEL_HIGH.equals(l)
            || SysSensitiveWordEntity.LEVEL_MEDIUM.equals(l)
            || SysSensitiveWordEntity.LEVEL_LOW.equals(l);
    }

    private boolean isValidAction(String a) {
        return SysSensitiveWordEntity.ACTION_DENY.equals(a)
            || SysSensitiveWordEntity.ACTION_ALERT.equals(a)
            || SysSensitiveWordEntity.ACTION_MASK.equals(a)
            || SysSensitiveWordEntity.ACTION_PASS.equals(a);
    }
}
