package com.jc.rag.admin.module.knowledge.controller;

import com.jc.rag.admin.module.knowledge.model.param.KnowledgeBaseCreateParam;
import com.jc.rag.admin.module.knowledge.model.param.KnowledgeBaseQueryParam;
import com.jc.rag.admin.module.knowledge.model.param.KnowledgeBaseUpdateParam;
import com.jc.rag.admin.module.knowledge.model.param.KnowledgeDocumentQueryParam;
import com.jc.rag.admin.module.knowledge.model.param.KnowledgeDocumentUpdateParam;
import com.jc.rag.admin.module.knowledge.model.vo.DocFileInfoVO;
import com.jc.rag.admin.module.knowledge.model.vo.DocParseProgressVO;
import com.jc.rag.admin.module.knowledge.model.vo.KbDocumentVO;
import com.jc.rag.admin.module.knowledge.model.vo.KnowledgeBaseVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.admin.module.knowledge.model.vo.UploadPolicyVO;
import com.jc.rag.admin.module.knowledge.service.KnowledgeBaseService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.common.security.CoreUserDetails;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识库管理接口。
 * <p>提供知识库与文档的 CRUD、上传与重建索引能力。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /** 查询知识库列表 (分页+搜索) */
    @GetMapping
    public R<PageResponse<KnowledgeBaseVO>> listKnowledgeBases(KnowledgeBaseQueryParam query) {
        return R.ok(knowledgeBaseService.listKnowledgeBases(query));
    }

    /** 获取当前 C 端默认知识库配置 */
    @GetMapping("/default")
    public R<KnowledgeBaseVO> getDefaultKnowledgeBase() {
        return R.ok(knowledgeBaseService.getDefaultKnowledgeBase());
    }

    /** 创建知识库 */
    @PostMapping
    public R<KnowledgeBaseVO> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseCreateParam request,
                                                   Authentication auth) {
        return R.ok(knowledgeBaseService.createKnowledgeBase(request, currentUserId(auth)));
    }

    /** 更新知识库基本信息 */
    @PutMapping("/{kbId}")
    public R<KnowledgeBaseVO> updateKnowledgeBase(
        @PathVariable Long kbId,
        @Valid @RequestBody KnowledgeBaseUpdateParam request
    ) {
        return R.ok(knowledgeBaseService.updateKnowledgeBase(kbId, request));
    }

    /** 将指定知识库设为 C 端默认知识库 */
    @PutMapping("/{kbId}/default")
    public R<KnowledgeBaseVO> setDefaultKnowledgeBase(@PathVariable Long kbId) {
        return R.ok(knowledgeBaseService.setDefaultKnowledgeBase(kbId));
    }

    /** 删除知识库 (逻辑删除 + 清理 MinIO/ES) */
    @DeleteMapping("/{kbId}")
    public R<BaseOpResultVO> deleteKnowledgeBase(@PathVariable Long kbId) {
        return R.ok(knowledgeBaseService.deleteKnowledgeBase(kbId));
    }

    /** 查询知识库下文档列表 (分页+搜索) */
    @GetMapping("/{kbId}/documents")
    public R<PageResponse<KbDocumentVO>> listDocuments(
            @PathVariable Long kbId,
            KnowledgeDocumentQueryParam query) {
        return R.ok(knowledgeBaseService.listDocuments(kbId, query));
    }

    /** 获取当前上传策略 (大小限制、支持格式等) */
    @GetMapping("/upload-policy")
    public R<UploadPolicyVO> getUploadPolicy() {
        return R.ok(knowledgeBaseService.getUploadPolicy());
    }

    /** 上传文档到指定知识库 (支持多文件) */
    @PostMapping("/{kbId}/documents/upload")
    public R<List<KbDocumentVO>> uploadDocuments(
        @PathVariable Long kbId,
        @RequestParam("files") MultipartFile[] files
    ) {
        return R.ok(knowledgeBaseService.uploadDocuments(kbId, files));
    }

    /** 触发文档重建索引 */
    @PostMapping("/documents/{documentId}/reprocess")
    public R<KbDocumentVO> reprocessDocument(@PathVariable Long documentId) {
        return R.ok(knowledgeBaseService.reprocessDocument(documentId));
    }

    /** 更新文档显示名称 */
    @PutMapping("/documents/{documentId}")
    public R<KbDocumentVO> updateDocument(
        @PathVariable Long documentId,
        @Valid @RequestBody KnowledgeDocumentUpdateParam request
    ) {
        return R.ok(knowledgeBaseService.updateDocument(documentId, request));
    }

    /** 删除文档 (逻辑删除 + 清理 MinIO/ES) */
    @DeleteMapping("/documents/{documentId}")
    public R<BaseOpResultVO> deleteDocument(@PathVariable Long documentId) {
        return R.ok(knowledgeBaseService.deleteDocument(documentId));
    }

    /** 下载 / 预览文档文件 (流式输出, 唯一允许 void + HttpServletResponse 的场景) */
    @GetMapping("/documents/{documentId}/file")
    public void downloadDocument(@PathVariable Long documentId, HttpServletResponse response) {
        DocFileInfoVO fileInfo = knowledgeBaseService.getDocumentFileInfo(documentId);
        String contentType = fileInfo.getContentType() == null ? "application/octet-stream" : fileInfo.getContentType();
        String fileName = fileInfo.getFileName() == null ? ("doc-" + documentId) : fileInfo.getFileName();

        response.setContentType(contentType);
        if (fileInfo.getFileSize() != null && fileInfo.getFileSize() > 0) {
            response.setContentLengthLong(fileInfo.getFileSize());
        }
        // RFC 5987 filename* 需百分号编码; URLEncoder 会把空格编成 '+', 需替换为 %20
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
            "attachment; filename=\"" + fileName.replace("\"", "'") + "\"; filename*=UTF-8''" + encoded);

        try (InputStream stream = fileInfo.getStream();
             OutputStream out = response.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = stream.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Exception e) {
            throw new com.jc.rag.common.exception.DataConflictException("doc.upload.invalid");
        }
    }

    /** 查询文档解析进度 (含阶段详情+处理日志) */
    @GetMapping("/documents/{documentId}/parse-progress")
    public R<DocParseProgressVO> getParseProgress(@PathVariable Long documentId) {
        return R.ok(knowledgeBaseService.getDocumentParseProgress(documentId));
    }

    /** 强制终止文档解析任务 */
    @PostMapping("/documents/{documentId}/cancel-parse")
    public R<BaseOpResultVO> cancelParse(@PathVariable Long documentId) {
        return R.ok(knowledgeBaseService.cancelDocumentParse(documentId));
    }

    private static Long currentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CoreUserDetails cu)) {
            throw new com.jc.rag.common.exception.AssertException(
                com.jc.rag.common.enums.ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return cu.getUserId();
    }
}
