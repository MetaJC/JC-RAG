package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.SysLegalDocSaveParam;
import com.jc.rag.admin.module.sys.model.vo.SysLegalDocVO;
import com.jc.rag.admin.module.sys.service.SysLegalDocService;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.domain.mapper.legal.SysLegalDocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户协议 / 隐私政策 Service 实现 (sys 模块)。
 * <p>每种 docType 全表唯一, 不分页; 通过 UPSERT 维护。
 *
 * @author JC
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLegalDocServiceImpl implements SysLegalDocService {

    private static final List<String> ALLOWED_DOC_TYPES = List.of("USER_AGREEMENT", "PRIVACY_POLICY");
    private static final List<String> ALLOWED_STATUS = List.of("DRAFT", "PUBLISHED");

    private final SysLegalDocMapper legalDocMapper;

    @Override
    public List<SysLegalDocVO> listAll() {
        List<Map<String, Object>> rows = legalDocMapper.listAll();
        List<SysLegalDocVO> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) out.add(toVO(row));
        return out;
    }

    @Override
    public SysLegalDocVO getByDocType(String docType) {
        if (!ALLOWED_DOC_TYPES.contains(docType)) {
            throw new BizException(ResultCode.PARAM_INVALID, "docType 不合法: " + docType);
        }
        List<Map<String, Object>> rows = legalDocMapper.findByDocType(docType);
        if (rows.isEmpty()) {
            SysLegalDocVO empty = new SysLegalDocVO();
            empty.setDocType(docType);
            empty.setStatus("DRAFT");
            empty.setTitle(defaultTitle(docType));
            return empty;
        }
        return toVO(rows.get(0));
    }

    @Override
    @Transactional
    public boolean save(SysLegalDocSaveParam param, Long userId) {
        if (param == null) throw new BizException(ResultCode.PARAM_INVALID, "参数不能为空");
        if (!ALLOWED_DOC_TYPES.contains(param.getDocType())) {
            throw new BizException(ResultCode.PARAM_INVALID, "docType 不合法");
        }
        String status = param.getStatus() == null || param.getStatus().isEmpty() ? "DRAFT" : param.getStatus();
        if (!ALLOWED_STATUS.contains(status)) {
            throw new BizException(ResultCode.PARAM_INVALID, "status 不合法");
        }
        String content = param.getContentMd() == null ? "" : param.getContentMd();

        int updated = legalDocMapper.updateByDocType(param.getDocType(), param.getTitle(), content, status, userId);
        if (updated == 0) {
            legalDocMapper.insertDoc(param.getDocType(), param.getTitle(), content, status, userId);
        }
        if ("PUBLISHED".equals(status)) {
            legalDocMapper.markPublished(param.getDocType(), userId);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean publish(String docType, Long userId) {
        if (!ALLOWED_DOC_TYPES.contains(docType)) {
            throw new BizException(ResultCode.PARAM_INVALID, "docType 不合法");
        }
        int updated = legalDocMapper.publish(docType, userId);
        if (updated == 0) {
            throw new BizException(ResultCode.NOT_FOUND, "请先在草稿中维护内容后再发布: " + docType);
        }
        return true;
    }

    @Override
    public String parseUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_INVALID, "文件不能为空");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String name = filename;
        try {
            if (name.endsWith(".md") || name.endsWith(".markdown")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            if (name.endsWith(".docx")) {
                return readDocx(file.getInputStream());
            }
            if (name.endsWith(".txt")) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }
            throw new BizException(ResultCode.PARAM_INVALID, "仅支持 .md / .markdown / .docx / .txt 文件");
        } catch (IOException e) {
            throw new BizException(ResultCode.FAIL, "文件解析失败: " + e.getMessage());
        }
    }

    /** 简化版 docx 解析: 读取段落, 标题按样式归一化为 #/##/### Markdown */
    private String readDocx(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText();
                if (text == null) text = "";
                text = text.trim();
                if (text.isEmpty()) {
                    sb.append('\n');
                    continue;
                }
                String style = p.getStyle();
                int level = headingLevel(style);
                if (level > 0) {
                    sb.append("#".repeat(level)).append(' ').append(text).append("\n\n");
                } else {
                    sb.append(text).append("\n\n");
                }
            }
        }
        return sb.toString().trim();
    }

    private int headingLevel(String style) {
        if (style == null) return 0;
        String s = style.toLowerCase();
        if (s.contains("heading1") || s.contains("title")) return 1;
        if (s.contains("heading2")) return 2;
        if (s.contains("heading3")) return 3;
        if (s.contains("heading4")) return 4;
        return 0;
    }

    private String defaultTitle(String docType) {
        return "USER_AGREEMENT".equals(docType) ? "用户协议" : "隐私政策";
    }

    private SysLegalDocVO toVO(Map<String, Object> row) {
        SysLegalDocVO v = new SysLegalDocVO();
        v.setId(toLong(row.get("id")));
        v.setDocType((String) row.get("doc_type"));
        v.setTitle((String) row.get("title"));
        v.setContentMd((String) row.get("content_md"));
        v.setSourceFilename((String) row.get("source_filename"));
        v.setStatus((String) row.get("status"));
        v.setPublishedAt(toLocal(row.get("published_at")));
        v.setPublishedBy(toLong(row.get("published_by")));
        v.setUpdatedBy(toLong(row.get("updated_by")));
        v.setCreatedAt(toLocal(row.get("created_at")));
        v.setUpdatedAt(toLocal(row.get("updated_at")));
        return v;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return null;
    }

    private static LocalDateTime toLocal(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime l) return l;
        if (o instanceof Timestamp t) return t.toLocalDateTime();
        return null;
    }
}
