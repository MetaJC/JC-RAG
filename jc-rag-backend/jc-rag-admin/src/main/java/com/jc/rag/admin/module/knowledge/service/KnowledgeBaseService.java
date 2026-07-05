package com.jc.rag.admin.module.knowledge.service;

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
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * KnowledgeBaseService 接口。
 * <p>所有方法返回严格类型 VO, 禁止使用 {@code Map<String,Object>}。
 *
 * @author JC
 */
public interface KnowledgeBaseService {

    void cleanParseMessageOnStartup();

    List<KnowledgeBaseVO> listKnowledgeBases();

    PageResponse<KnowledgeBaseVO> listKnowledgeBases(KnowledgeBaseQueryParam query);

    KnowledgeBaseVO getDefaultKnowledgeBase();

    @Transactional
    KnowledgeBaseVO setDefaultKnowledgeBase(Long kbId);

    /** 清除当前 C 端默认知识库 (置空 chat.default.kbId) */
    @Transactional
    void clearDefaultKnowledgeBase();

    @Transactional
    KnowledgeBaseVO createKnowledgeBase(KnowledgeBaseCreateParam request, Long ownerUserId);

    @Transactional
    KnowledgeBaseVO updateKnowledgeBase(Long kbId, KnowledgeBaseUpdateParam request);

    @Transactional BaseOpResultVO deleteKnowledgeBase(Long kbId);

    List<KbDocumentVO> listDocuments(Long kbId);

    PageResponse<KbDocumentVO> listDocuments(Long kbId, KnowledgeDocumentQueryParam query);

    UploadPolicyVO getUploadPolicy();

    @Transactional
    List<KbDocumentVO> uploadDocuments(Long kbId, MultipartFile[] files);

    @Transactional
    KbDocumentVO reprocessDocument(Long documentId);

    @Transactional
    KbDocumentVO updateDocument(Long documentId, KnowledgeDocumentUpdateParam request);

    @Transactional BaseOpResultVO deleteDocument(Long documentId);

    DocFileInfoVO getDocumentFileInfo(Long documentId);

    DocParseProgressVO getDocumentParseProgress(Long documentId);

    BaseOpResultVO cancelDocumentParse(Long documentId);
}
