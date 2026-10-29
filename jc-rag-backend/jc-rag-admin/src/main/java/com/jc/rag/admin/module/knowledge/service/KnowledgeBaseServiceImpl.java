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


package com.jc.rag.admin.module.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc.rag.common.config.RuntimeConfigDefaults;
import com.jc.rag.domain.mapper.sys.SysConfigMapper;
import com.jc.rag.admin.config.ExternalServiceProperties;
import com.jc.rag.admin.config.KnowledgeUploadProperties;
import com.jc.rag.admin.config.props.StorageProperties;
import com.jc.rag.common.web.page.PageHelper;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.common.web.page.TimeFormatUtil;
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
import com.jc.rag.admin.module.knowledge.exception.KnowledgeTaskConflictException;
import com.jc.rag.admin.module.knowledge.exception.KnowledgeUploadException;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.exception.DataConflictException;
import com.jc.rag.common.exception.ParamInvalidException;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.domain.enums.knowledge.KbStatusEnum;
import com.jc.rag.domain.enums.knowledge.KbPermLevelEnum;
import com.jc.rag.domain.enums.knowledge.VisibilityScopeEnum;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.domain.entity.knowledge.KbDocumentEntity;
import com.jc.rag.domain.entity.knowledge.KbDocumentVersionEntity;
import com.jc.rag.domain.entity.knowledge.KbKnowledgeBaseEntity;
import com.jc.rag.domain.mapper.knowledge.KbDocumentChunkTaskMapper;
import com.jc.rag.domain.mapper.knowledge.KbDocumentMapper;
import com.jc.rag.domain.mapper.knowledge.KbDocumentVersionMapper;
import com.jc.rag.domain.mapper.knowledge.KbKnowledgeBaseMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import com.jc.rag.admin.module.sys.service.OperationLogService;
import com.jc.rag.admin.security.DbUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * KnowledgeBaseService 实现
 *
 * @author JC
 */
@Service
public class KnowledgeBaseServiceImpl
        extends com.jc.rag.common.web.page.AbstractPageService<KnowledgeBaseVO, KnowledgeBaseQueryParam, KbKnowledgeBaseEntity>
        implements KnowledgeBaseService {

    @Override
    protected IPage<KbKnowledgeBaseEntity> pageQuery(KnowledgeBaseQueryParam q, IPage<KbKnowledgeBaseEntity> page) {
        return kbKnowledgeBaseMapper.pageList(page, PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    protected long countQuery(KnowledgeBaseQueryParam q) {
        return kbKnowledgeBaseMapper.countList(PageHelper.buildLikeClause(q.getKeyword()));
    }

    @Override
    protected KnowledgeBaseVO toVO(KbKnowledgeBaseEntity e) {
        Long defaultKbId = loadLongConfig("chat.default.kbId");
        KnowledgeBaseVO v = new KnowledgeBaseVO();
        v.setId(e.getId());
        v.setKbCode(e.getKbCode());
        v.setKbName(e.getKbName());
        v.setKbDesc(e.getKbDesc());
        v.setVisibilityScope(e.getVisibilityScope());
        v.setClientSelectable(e.getClientSelectable());
        v.setOwnerUserId(e.getOwnerUserId());
        v.setStatus(e.getStatus());
        v.setIsDefault(defaultKbId != null && defaultKbId.equals(e.getId()));
        v.setCreatedAt(e.getCreatedAt());
        v.setUpdatedAt(e.getUpdatedAt());
        return v;
    }

    /** 列表场景: 补 myPermLevel 字段 */
    private KnowledgeBaseVO toVOWithPerm(KbKnowledgeBaseEntity e, KbPermLevelEnum perm) {
        KnowledgeBaseVO v = toVO(e);
        v.setMyPermLevel(perm == null ? 0 : perm.getCode());
        return v;
    }


    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
    private static final ExecutorService PARSE_EXECUTOR = Executors.newFixedThreadPool(2);

    private final KbKnowledgeBaseMapper kbKnowledgeBaseMapper;
    private final KbDocumentMapper kbDocumentMapper;
    private final KbDocumentVersionMapper kbDocumentVersionMapper;
    private final KbDocumentChunkTaskMapper kbDocumentChunkTaskMapper;
    private final SysConfigMapper sysConfigMapper;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final ExternalServiceProperties externalServiceProperties;
    private final KnowledgeUploadProperties knowledgeUploadProperties;
    private final OperationLogService operationLogService;
    private final KbPermissionService kbPermissionService;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseServiceImpl(
        KbKnowledgeBaseMapper kbKnowledgeBaseMapper,
        KbDocumentMapper kbDocumentMapper,
        KbDocumentVersionMapper kbDocumentVersionMapper,
        KbDocumentChunkTaskMapper kbDocumentChunkTaskMapper,
        SysConfigMapper sysConfigMapper,
        MinioClient minioClient,
        StorageProperties storageProperties,
        ExternalServiceProperties externalServiceProperties,
        KnowledgeUploadProperties knowledgeUploadProperties,
        OperationLogService operationLogService,
        KbPermissionService kbPermissionService,
        ObjectMapper objectMapper
    ) {
        this.kbKnowledgeBaseMapper = kbKnowledgeBaseMapper;
        this.kbDocumentMapper = kbDocumentMapper;
        this.kbDocumentVersionMapper = kbDocumentVersionMapper;
        this.kbDocumentChunkTaskMapper = kbDocumentChunkTaskMapper;
        this.sysConfigMapper = sysConfigMapper;
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
        this.externalServiceProperties = externalServiceProperties;
        this.knowledgeUploadProperties = knowledgeUploadProperties;
        this.operationLogService = operationLogService;
        this.kbPermissionService = kbPermissionService;
        this.objectMapper = objectMapper;
    }

    /** 取当前登录 DbUserDetails, 无登录态则抛未授权. */
    private DbUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof DbUserDetails u)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return u;
    }

    /** 当前用户绑定的 roleIds (缓存到方法栈, 单次请求复用) */
    private List<Long> currentRoleIds() {
        return kbPermissionService.loadCurrentUserRoleIds(currentUser().getUserId());
    }

    /**
     * 启动时清洗历史脏数据：parse_message 中存储的 JSON 提取纯文本 msg。
     */
    @PostConstruct
    public void cleanParseMessageOnStartup() {
        try {
            List<Map<String, Object>> dirtyRows = kbDocumentMapper.listDirtyParseMessages();
            int cleaned = 0;
            for (Map<String, Object> row : dirtyRows) {
                String raw = (String) row.get("parse_message");
                if (raw == null || raw.isBlank()) continue;
                String trimmed = raw.trim();
                String extracted = trimmed;
                try {
                    if (trimmed.startsWith("{") && trimmed.contains("\"msg\"")) {
                        Map<String, Object> parsed = objectMapper.readValue(trimmed, new TypeReference<>() {});
                        Object innerMsg = parsed.get("msg");
                        if (innerMsg != null) extracted = String.valueOf(innerMsg);
                    } else if (trimmed.startsWith("{") && trimmed.contains("msg=")) {
                        int start = trimmed.indexOf("msg=") + 4;
                        int end = trimmed.indexOf(',', start);
                        if (end < 0) end = trimmed.indexOf('}', start);
                        if (end > start) extracted = trimmed.substring(start, end).trim();
                    }
                } catch (Exception ignore) {
                    continue;
                }
                if (!extracted.equals(trimmed)) {
                    kbDocumentMapper.updateParseMessage(((Number) row.get("id")).longValue(), trimForDb(extracted));
                    cleaned++;
                }
            }
            if (cleaned > 0) {
                log.info("已清洗 {} 条文档的 parse_message 脏数据", cleaned);
            }
        } catch (Exception e) {
            log.warn("parse_message 清洗失败（可忽略，不影响功能）: {}", e.getMessage());
        }
    }

    /**
     * 查询所有启用状态的知识库列表。
     */
    @Override
    public List<KnowledgeBaseVO> listKnowledgeBases() {
        return listKnowledgeBases(KnowledgeBaseQueryParam.all()).getRecords();
    }

    @Override
    public PageResponse<KnowledgeBaseVO> listKnowledgeBases(KnowledgeBaseQueryParam query) {
        PageResponse<KnowledgeBaseVO> page = pageList(query);
        DbUserDetails user = currentUser();
        List<Long> roleIds = currentRoleIds();
        // 过滤: 保留当前用户有 VIEW 及以上的 KB
        java.util.Map<Long, KbPermLevelEnum> permMap = kbPermissionService.batchResolvePermLevel(
                page.getRecords().stream().map(v -> {
                    KbKnowledgeBaseEntity e = new KbKnowledgeBaseEntity();
                    e.setId(v.getId());
                    e.setVisibilityScope(v.getVisibilityScope());
                    e.setOwnerUserId(v.getOwnerUserId());
                    return e;
                }).collect(Collectors.toList()), user, roleIds);
        List<KnowledgeBaseVO> kept = new java.util.ArrayList<>();
        for (KnowledgeBaseVO v : page.getRecords()) {
            KbPermLevelEnum perm = permMap.getOrDefault(v.getId(), KbPermLevelEnum.NONE);
            if (perm.covers(KbPermLevelEnum.VIEW)) {
                v.setMyPermLevel(perm.getCode());
                kept.add(v);
            }
        }
        page.setRecords(kept);
        page.setTotal((long) kept.size());
        return page;
    }

    /**
     * 读取当前 C 端默认知识库。
     */
    @Override
    public KnowledgeBaseVO getDefaultKnowledgeBase() {
        Long defaultKbId = loadLongConfig("chat.default.kbId");
        if (defaultKbId == null) {
            List<KnowledgeBaseVO> list = listKnowledgeBases();
            return list.isEmpty() ? null : list.get(0);
        }
        try {
            return getKnowledgeBaseById(defaultKbId);
        } catch (Exception ex) {
            List<KnowledgeBaseVO> list = listKnowledgeBases();
            return list.isEmpty() ? null : list.get(0);
        }
    }

    /**
     * 设置当前 C 端默认知识库。
     * <p>仅超管/developer 可设置; 同时知识库必须为 PUBLIC, 私有 KB 不可作为默认。
     */
    @Override
    @Transactional
    public KnowledgeBaseVO setDefaultKnowledgeBase(Long kbId) {
        DbUserDetails user = currentUser();
        if (!kbPermissionService.isDeveloper(user) && !kbPermissionService.isSuperAdmin(user)) {
            throw new AssertException(ResultCode.FORBIDDEN, "kb.default.onlyAdmin");
        }
        KbKnowledgeBaseEntity kb = kbPermissionService.loadActiveKb(kbId);
        if (!VisibilityScopeEnum.PUBLIC.name().equalsIgnoreCase(kb.getVisibilityScope())) {
            throw new AssertException(ResultCode.PARAM_INVALID, "kb.default.mustPublic");
        }
        sysConfigMapper.upsertConfig(
            "chat",
            "chat.default.kbId",
            String.valueOf(kbId),
            "NUMBER",
            0,
            "C 端默认知识库"
        );
        operationLogService.log("知识库管理", "SET_DEFAULT_KB", "设置默认知识库:" + kb.getKbName() + " (id=" + kbId + ")", "SUCCESS");
        return getKnowledgeBaseById(kbId);
    }

    /**
     * 清除默认知识库配置。
     */
    @Override
    @Transactional
    public void clearDefaultKnowledgeBase() {
        DbUserDetails user = currentUser();
        if (!kbPermissionService.isDeveloper(user) && !kbPermissionService.isSuperAdmin(user)) {
            throw new AssertException(ResultCode.FORBIDDEN, "kb.default.onlyAdmin");
        }
        sysConfigMapper.upsertConfig(
            "chat",
            "chat.default.kbId",
            "",
            "NUMBER",
            0,
            "C 端默认知识库 (已清空)"
        );
        operationLogService.log("知识库管理", "CLEAR_DEFAULT_KB", "清除默认知识库配置", "SUCCESS");
    }

    /**
     * 创建知识库。
     * <p>ownerUserId 由 Controller 从当前登录用户传入, 谁创建归谁。
     * <p>新建默认 PRIVATE, clientSelectable=0。
     */
    @Override
    @Transactional
    public KnowledgeBaseVO createKnowledgeBase(KnowledgeBaseCreateParam request, Long ownerUserId) {
        if (request == null) {
            throw new ParamInvalidException("common.param.invalid", "request");
        }
        if (ownerUserId == null) {
            throw new AssertException(ResultCode.PARAM_INVALID, "common.param.invalid", "ownerUserId");
        }
        // 仅 ADMIN/developer 可创建 (登录态已限制为 ADMIN, 此处二次保护)
        DbUserDetails user = currentUser();
        if (!kbPermissionService.isDeveloper(user)
                && !kbPermissionService.isSuperAdmin(user)
                && !kbPermissionService.isAdminUser(user)) {
            throw new AssertException(ResultCode.FORBIDDEN, "kb.create.forbidden");
        }
        String kbCode = request.getKbCode() != null && !request.getKbCode().isBlank()
            ? request.getKbCode().trim()
            : "KB" + System.currentTimeMillis();
        KbKnowledgeBaseEntity entity = new KbKnowledgeBaseEntity();
        entity.setKbCode(kbCode);
        entity.setKbName(request.getKbName());
        entity.setKbDesc(request.getKbDesc());
        entity.setVisibilityScope(VisibilityScopeEnum.PRIVATE.name());
        entity.setClientSelectable(0);
        entity.setOwnerUserId(ownerUserId);
        entity.setStatus(KbStatusEnum.ENABLED.getCode());
        if (kbKnowledgeBaseMapper.insertKb(entity) <= 0) {
            throw new DataConflictException("kb.create.failed");
        }
        operationLogService.log("知识库管理", "CREATE_KB", "创建知识库:" + request.getKbName(), "SUCCESS");
        return getKnowledgeBaseById(entity.getId());
    }

    /**
     * 更新知识库基本信息（名称、描述、可见范围、clientSelectable）。
     */
    @Override
    @Transactional
    public KnowledgeBaseVO updateKnowledgeBase(Long kbId, KnowledgeBaseUpdateParam request) {
        KbKnowledgeBaseEntity existing = kbKnowledgeBaseMapper.selectActiveById(kbId);
        if (existing == null) {
            throw new ResourceNotFoundException("kb.not.found", kbId);
        }
        DbUserDetails user = currentUser();
        List<Long> roleIds = currentRoleIds();
        String newScope = request.getVisibilityScope() == null || request.getVisibilityScope().isBlank()
            ? existing.getVisibilityScope()
            : request.getVisibilityScope().trim();
        if (VisibilityScopeEnum.parse(newScope) == null) {
            throw new ParamInvalidException("kb.visibility.invalid", newScope);
        }
        // 可见性变更校验
        if (newScope != null && !newScope.equalsIgnoreCase(existing.getVisibilityScope())) {
            kbPermissionService.assertCanChangeVisibility(existing, user, roleIds, newScope);
        }
        // clientSelectable 仅超管/developer 可调整
        Integer newClientSelectable = request.getClientSelectable();
        boolean clientSelectableChanged = newClientSelectable != null
                && !newClientSelectable.equals(existing.getClientSelectable());
        if (clientSelectableChanged) {
            kbPermissionService.assertCanChangeClientSelectable(user);
            if (!VisibilityScopeEnum.PUBLIC.name().equalsIgnoreCase(newScope)
                    && !VisibilityScopeEnum.PUBLIC.name().equalsIgnoreCase(existing.getVisibilityScope())) {
                throw new AssertException(ResultCode.PARAM_INVALID, "kb.clientSelectable.onlyPublic");
            }
        }
        if (kbKnowledgeBaseMapper.updateBasicInfo(kbId, request.getKbName(), request.getKbDesc(), newScope) <= 0) {
            throw new DataConflictException("kb.update.failed");
        }
        if (clientSelectableChanged) {
            kbKnowledgeBaseMapper.updateClientSelectable(kbId, newClientSelectable);
        }
        operationLogService.log(
            "知识库管理",
            "UPDATE_KB",
            "更新知识库:" + existing.getKbName() + " -> " + request.getKbName()
                    + " | scope: " + existing.getVisibilityScope() + " -> " + newScope
                    + (clientSelectableChanged ? " | clientSelectable: " + newClientSelectable : ""),
            "SUCCESS"
        );
        return getKnowledgeBaseById(kbId);
    }

    /**
     * 删除知识库。
     *
     * <p>说明：
     * <ul>
     *   <li>知识库采用 status=0 逻辑删除</li>
     *   <li>文档采用 deleted=1 逻辑删除，并同步清理 MinIO/ES 中的实际数据</li>
     * </ul>
     */
    @Override
    @Transactional
    public BaseOpResultVO deleteKnowledgeBase(Long kbId) {
        KbKnowledgeBaseEntity kb = kbKnowledgeBaseMapper.selectActiveById(kbId);
        if (kb == null) {
            throw new ResourceNotFoundException("kb.not.found", kbId);
        }
        // 鉴权: 需 MANAGE
        DbUserDetails user = currentUser();
        kbPermissionService.assertPerm(kb, user, currentRoleIds(), KbPermLevelEnum.MANAGE);

        List<Map<String, Object>> docs = kbDocumentMapper.listMinioInfoByKbId(kbId);

        // 先标记数据库记录为删除，避免前端继续显示；后续再做对象存储与索引清理。
        kbDocumentMapper.markDeletedByKbId(kbId);
        if (kbKnowledgeBaseMapper.disableById(kbId) <= 0) {
            throw new DataConflictException("kb.delete.failed");
        }

        // 清理 MinIO 中对应文档对象（失败不回滚数据库，避免删除流程卡死）。
        for (Map<String, Object> doc : docs) {
            removeMinioObject(String.valueOf(doc.get("minioBucket")), String.valueOf(doc.get("minioObjectName")));
        }
        // 清理 ES 中该知识库对应的 chunk 索引（失败仅记录日志）。
        invokePythonKnowledgeDelete(kbId, null);

        operationLogService.log("知识库管理", "DELETE_KB", "删除知识库:" + kb.getKbName(), "SUCCESS");
        BaseOpResultVO vo = BaseOpResultVO.success(kbId, "知识库及关联文档已删除");
        vo.setExtraCount(docs.size());
        return vo;
    }

    /**
     * 查询指定知识库下的文档列表 (仅返回未删除文档)。
     */
    @Override
    public List<KbDocumentVO> listDocuments(Long kbId) {
        return listDocuments(kbId, KnowledgeDocumentQueryParam.all()).getRecords();
    }

    /**
     * 查询指定知识库下的文档列表 (分页+搜索)。
     */
    @Override
    public PageResponse<KbDocumentVO> listDocuments(Long kbId, KnowledgeDocumentQueryParam query) {
        KbKnowledgeBaseEntity kb = kbPermissionService.loadActiveKb(kbId);
        // 鉴权: 列表需 VIEW
        kbPermissionService.assertPerm(kb, currentUser(), currentRoleIds(), KbPermLevelEnum.VIEW);
        String parseStatus = query.getParseStatus();
        String parseStatusFilter = (parseStatus == null || parseStatus.isBlank())
            ? null
            : parseStatus.trim().toUpperCase();
        String like = PageHelper.buildLikeClause(query.getKeyword());
        IPage<KbDocumentEntity> page = PageHelper.pageOf(query);
        page = kbDocumentMapper.pageList(page, kbId, like, parseStatusFilter);
        page.setTotal(kbDocumentMapper.countList(kbId, like, parseStatusFilter));
        return PageHelper.toPageResponse(page, this::toDocumentVO);
    }

    /** KbDocumentEntity -> KbDocumentVO. */
    private KbDocumentVO toDocumentVO(KbDocumentEntity e) {
        KbDocumentVO v = new KbDocumentVO();
        org.springframework.beans.BeanUtils.copyProperties(e, v);
        return v;
    }

    @Override
    public UploadPolicyVO getUploadPolicy() {
        long maxFileSizeBytes = knowledgeUploadProperties.getMaxFileSize().toBytes();
        long maxRequestSizeBytes = knowledgeUploadProperties.getMaxRequestSize().toBytes();
        List<String> allowedExtensions = knowledgeUploadProperties.getAllowedExtensions().stream()
            .filter(item -> item != null && !item.isBlank())
            .map(item -> item.trim().toLowerCase())
            .distinct()
            .sorted(Comparator.naturalOrder())
            .toList();
        UploadPolicyVO vo = new UploadPolicyVO();
        vo.setMaxFileSizeBytes(maxFileSizeBytes);
        vo.setMaxRequestSizeBytes(maxRequestSizeBytes);
        vo.setMaxFileSizeLabel(formatBytes(maxFileSizeBytes));
        vo.setMaxRequestSizeLabel(formatBytes(maxRequestSizeBytes));
        vo.setAllowedExtensions(allowedExtensions);
        vo.setAccept(allowedExtensions.stream().map(item -> "." + item).collect(Collectors.joining(",")));
        vo.setParseTimeoutSeconds(knowledgeUploadProperties.getParseTimeout().toSeconds());
        return vo;
    }

    private void invokePythonDeleteByDocumentId(String documentId) throws Exception {
        String pythonRagBaseUrl = resolvePythonRagBaseUrl();
        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("documentId", documentId);
        String jsonBody = objectMapper.writeValueAsString(bodyMap);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(normalizeBaseUrl(pythonRagBaseUrl) + "/knowledge/delete"))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
            .build();
        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     * 上传多个文档到知识库。
     *
     * <p>流程：
     * <ol>
     *   <li>校验知识库是否存在</li>
     *   <li>校验文件非空、类型、大小</li>
     *   <li>写入 MinIO</li>
     *   <li>写入 MySQL 文档元数据、版本记录、索引任务记录</li>
     *   <li>调用 Python /knowledge/parse 完成解析与写入 ES</li>
     * </ol>
     */
    @Override
    @Transactional
    public List<KbDocumentVO> uploadDocuments(Long kbId, MultipartFile[] files) {
        KbKnowledgeBaseEntity kb = kbKnowledgeBaseMapper.selectActiveById(kbId);
        if (kb == null) {
            throw new ResourceNotFoundException("kb.upload.notfound", kbId);
        }
        // 鉴权: 上传/添加文档需 MANAGE
        kbPermissionService.assertPerm(kb, currentUser(), currentRoleIds(), KbPermLevelEnum.MANAGE);
        validateUploadFiles(files);
        ensureBucketExists();
        for (MultipartFile file : files) {
            saveDocument(kbId, file);
        }
        return listDocuments(kbId);
    }

    /**
     * 重建文档索引（从 MinIO 读取原始文件内容重新解析并写入 ES）。
     */
    @Override
    @Transactional
    public KbDocumentVO reprocessDocument(Long documentId) {
        Map<String, Object> document = kbDocumentMapper.selectForReprocess(documentId);
        // 鉴权: 重建索引需 EDIT
        Long kbId = asLong(document.get("kb_id"));
        KbKnowledgeBaseEntity kb = kbPermissionService.loadActiveKb(kbId);
        kbPermissionService.assertPerm(kb, currentUser(), currentRoleIds(), KbPermLevelEnum.EDIT);

        validateDocumentReprocessable(documentId, document);
        claimDocumentForReprocess(documentId);
        resetChunkTask(documentId);
        registerAfterCommit(() -> submitParseJob(documentId, buildParsePayload(document)));
        operationLogService.log("知识库管理", "REPROCESS_DOCUMENT", "重建文档索引已提交:" + document.get("file_name"), "SUCCESS");

        return toDocumentVOFromMap(kbDocumentMapper.selectDocSummary(documentId));
    }

    /**
     * 更新文档显示名称（仅更新数据库 file_name 字段）。
     */
    @Override
    @Transactional
    public KbDocumentVO updateDocument(Long documentId, KnowledgeDocumentUpdateParam request) {
        Map<String, Object> existing = kbDocumentMapper.selectFileInfo(documentId);
        if (existing == null) {
            throw new ResourceNotFoundException("doc.not.found", documentId);
        }
        // 鉴权: 需 EDIT
        Long kbId = asLong(existing.get("kb_id"));
        KbKnowledgeBaseEntity kb = kbPermissionService.loadActiveKb(kbId);
        kbPermissionService.assertPerm(kb, currentUser(), currentRoleIds(), KbPermLevelEnum.EDIT);
        kbDocumentMapper.updateFileName(documentId, request.getFileName());
        operationLogService.log(
            "知识库管理",
            "UPDATE_DOCUMENT",
            "更新文档名称:" + existing.get("file_name") + " -> " + request.getFileName(),
            "SUCCESS"
        );
        return toDocumentVOFromMap(kbDocumentMapper.selectDocSummary(documentId));
    }

    /**
     * 删除文档（逻辑删除 + 清理 MinIO/ES）。
     */
    @Override
    @Transactional
    public BaseOpResultVO deleteDocument(Long documentId) {
        Map<String, Object> doc = kbDocumentMapper.selectFileInfo(documentId);
        // 鉴权: 需 MANAGE
        Long kbId = asLong(doc.get("kb_id"));
        KbKnowledgeBaseEntity kb = kbPermissionService.loadActiveKb(kbId);
        kbPermissionService.assertPerm(kb, currentUser(), currentRoleIds(), KbPermLevelEnum.MANAGE);
        kbDocumentMapper.markDeleted(documentId);
        removeMinioObject(String.valueOf(doc.get("minio_bucket")), String.valueOf(doc.get("minio_object_name")));
        invokePythonKnowledgeDelete(null, documentId);
        operationLogService.log("知识库管理", "DELETE_DOCUMENT", "删除文档:" + doc.get("file_name"), "SUCCESS");
        return BaseOpResultVO.success(documentId, "文档已删除");
    }

    /** kb_document Map 行 -> KbDocumentVO. */
    private KbDocumentVO toDocumentVOFromMap(Map<String, Object> m) {
        KbDocumentVO v = new KbDocumentVO();
        v.setId(asLong(m.get("id")));
        v.setFileName((String) m.get("file_name"));
        v.setFileExt((String) m.get("file_ext"));
        v.setFileSize(asLong(m.get("file_size")));
        v.setContentType((String) m.get("content_type"));
        Object versionNo = m.get("version_no");
        v.setVersionNo(versionNo == null ? null : Integer.valueOf(String.valueOf(versionNo)));
        v.setDocType((String) m.get("doc_type"));
        v.setSecurityLevel((String) m.get("security_level"));
        v.setParseStatus((String) m.get("parse_status"));
        v.setParseMessage((String) m.get("parse_message"));
        Object pageCount = m.get("page_count");
        v.setPageCount(pageCount == null ? null : Integer.valueOf(String.valueOf(pageCount)));
        Object chunkCount = m.get("chunk_count");
        v.setChunkCount(chunkCount == null ? null : Integer.valueOf(String.valueOf(chunkCount)));
        v.setUploadedAt(asDateTime(m.get("uploaded_at")));
        v.setUpdatedAt(asDateTime(m.get("updated_at")));
        return v;
    }

    /**
     * 保存单个文档：
     * <ul>
     *   <li>写入 MinIO</li>
     *   <li>写入 kb_document、kb_document_version、kb_document_chunk_task</li>
     *   <li>调用 Python /knowledge/parse 写入 ES</li>
     * </ul>
     */
    private void saveDocument(Long kbId, MultipartFile file) {
        String ext = extractExtension(file.getOriginalFilename());
        String bucket = storageProperties.getMinio().getBucket().getDocuments();
        String objectName = buildObjectName(kbId, file.getOriginalFilename());
        String fileHash;
        try {
            // 1) 对象存储：原始文件写入 MinIO，后续支持重建索引从 MinIO 回读。
            try (DigestInputStream digestStream = new DigestInputStream(file.getInputStream(), MessageDigest.getInstance("SHA-256"))) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .stream(digestStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
                );
                fileHash = toHex(digestStream.getMessageDigest().digest());
            }
        } catch (Exception ex) {
            throw new DataConflictException("kb.upload.minio.failed");
        }

        // 2) 元数据入库：文档主表 + 版本表 + 索引任务表
        KbDocumentEntity docEntity = new KbDocumentEntity();
        docEntity.setKbId(kbId);
        docEntity.setFileName(file.getOriginalFilename());
        docEntity.setFileExt(ext);
        docEntity.setFileSize(file.getSize());
        docEntity.setContentType(file.getContentType());
        docEntity.setMinioBucket(bucket);
        docEntity.setMinioObjectName(objectName);
        docEntity.setFileHash(fileHash);
        docEntity.setVersionNo(1);
        docEntity.setSourcePath(file.getOriginalFilename());
        docEntity.setDocType("GENERAL");
        docEntity.setSecurityLevel("INTERNAL");
        docEntity.setParseStatus("PENDING");
        docEntity.setParseMessage("文档已上传，等待后台解析");
        docEntity.setPageCount(0);
        docEntity.setChunkCount(0);
        docEntity.setUploadedBy(1L);
        docEntity.setDeleted(0);
        if (kbDocumentMapper.insertDoc(docEntity) <= 0) {
            throw new DataConflictException("doc.create.failed");
        }

        Long documentId = docEntity.getId();
        KbDocumentVersionEntity versionEntity = new KbDocumentVersionEntity();
        versionEntity.setDocumentId(documentId);
        versionEntity.setVersionNo(1);
        versionEntity.setMinioObjectName(objectName);
        versionEntity.setFileHash(fileHash);
        versionEntity.setCreatedBy(1L);
        versionEntity.setRemark("初始上传版本");
        kbDocumentVersionMapper.insertVersion(versionEntity);
        kbDocumentChunkTaskMapper.insertInitial(documentId, "INDEX", "PENDING");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", documentId);
        payload.put("kbId", kbId);
        payload.put("fileName", file.getOriginalFilename());
        payload.put("fileExt", ext);
        payload.put("contentType", file.getContentType());
        payload.put("versionNo", 1);
        payload.put("docType", "GENERAL");
        payload.put("securityLevel", "INTERNAL");
        payload.put("minioBucket", bucket);
        payload.put("minioObjectName", objectName);
        payload.put("chunkSize", loadIntegerConfig("rag.chunk.size", RuntimeConfigDefaults.CHUNK_SIZE));
        payload.put("chunkOverlap", loadIntegerConfig("rag.chunk.overlap", RuntimeConfigDefaults.CHUNK_OVERLAP));
        payload.put("ragOverride", buildRagOverrideForPython());
        registerAfterCommit(() -> submitParseJob(documentId, payload));
        operationLogService.log("知识库管理", "UPLOAD_DOCUMENT", "上传文档并提交后台解析:" + file.getOriginalFilename(), "SUCCESS");
    }

    /**
     * 调用 Python /knowledge/parse 进行解析与索引写入。
     *
     * <p>当前协议只传 MinIO 对象位置与文档元数据，不再传 contentBase64 大包。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> invokePythonKnowledgeParse(Map<String, Object> payload) {
        Object documentId = payload.get("documentId");
        Object fileName = payload.get("fileName");
        Object kbId = payload.get("kbId");
        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            String url = normalizeBaseUrl(externalServiceProperties.getPython().getRag().getBaseUrl()) + "/knowledge/parse";
            log.info(
                "Call python parse: url={}, kbId={}, documentId={}, fileName={}, jsonLen={}",
                url,
                kbId,
                documentId,
                fileName,
                requestBody.length()
            );
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(knowledgeUploadProperties.getParseTimeout())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            String responseBody = response.body();
            if (statusCode >= 400) {
                log.warn("Call python parse failed: status={}, responseBody={}", statusCode, responseBody);
                throw new DataConflictException("kb.upload.python.failed");
            }

            if (responseBody == null || responseBody.isBlank()) {
                throw new DataConflictException("kb.upload.python.failed");
            }

            return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        } catch (HttpTimeoutException ex) {
            Duration timeout = knowledgeUploadProperties.getParseTimeout();
            log.warn(
                "Call python parse timed out: timeout={}, kbId={}, documentId={}, fileName={}",
                timeout,
                kbId,
                documentId,
                fileName
            );
            throw new DataConflictException("doc.parse.timeout");
        } catch (Exception ex) {
            log.warn("Call python parse failed: {}", ex.getMessage());
            throw new DataConflictException("kb.upload.python.failed");
        }
    }

    /**
     * 调用 Python /knowledge/delete 清理 ES 中的索引数据。
     *
     * <p>该调用为尽力而为（best-effort），失败只记录日志，不影响数据库删除流程。
     */
    private void invokePythonKnowledgeDelete(Long kbId, Long documentId) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (kbId != null) {
                payload.put("kbId", kbId);
            }
            if (documentId != null) {
                payload.put("documentId", documentId);
            }
            String requestBody = objectMapper.writeValueAsString(payload);
            String url = normalizeBaseUrl(externalServiceProperties.getPython().getRag().getBaseUrl()) + "/knowledge/delete";
            log.info("Call python delete: url={}, kbId={}, documentId={}, jsonLen={}", url, kbId, documentId, requestBody.length());
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                log.warn("Call python delete failed: status={}, responseBody={}", response.statusCode(), response.body());
            }
        } catch (Exception ex) {
            log.warn("Call python delete failed: {}", ex.getMessage());
        }
    }

    /**
     * 删除 MinIO 中的对象（best-effort）。
     */
    private void removeMinioObject(String bucket, String objectName) {
        try {
            if (bucket == null || bucket.isBlank() || objectName == null || objectName.isBlank()) {
                return;
            }
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
        } catch (Exception ex) {
            log.warn("Remove minio object failed: bucket={}, object={}, err={}", bucket, objectName, ex.getMessage());
        }
    }

    /**
     * 规范化外部服务 baseUrl，避免末尾多余斜杠导致拼接错误。
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 更新文档解析结果到数据库。
     */
    private void updateDocumentParseResult(Long documentId, Map<String, Object> response, String status) {
        int chunkCount = response.get("chunkCount") instanceof Number number ? number.intValue() : 0;
        int pageCount = response.get("pageCount") instanceof Number number ? number.intValue() : 0;
        Object messageObj = response.get("message");
        String message;
        if (messageObj == null) {
            message = "索引完成";
        } else if (messageObj instanceof String s) {
            String trimmed = s.trim();
            if (trimmed.startsWith("{") && trimmed.contains("\"msg\"")) {
                try {
                    Map<String, Object> parsed = objectMapper.readValue(trimmed, new TypeReference<>() {});
                    Object innerMsg = parsed.get("msg");
                    message = innerMsg != null ? String.valueOf(innerMsg) : trimmed;
                } catch (Exception e) {
                    message = trimmed;
                }
            } else {
                message = s;
            }
        } else if (messageObj instanceof Map map) {
            Object msgField = map.get("msg");
            if (msgField != null) {
                message = String.valueOf(msgField);
            } else {
                try {
                    message = objectMapper.writeValueAsString(map);
                } catch (Exception e) {
                    message = String.valueOf(map);
                }
            }
        } else {
            try {
                message = objectMapper.writeValueAsString(messageObj);
            } catch (Exception e) {
                message = String.valueOf(messageObj);
            }
        }
        kbDocumentMapper.updateParseState(documentId, status, trimForDb(message), chunkCount, pageCount);
    }

    private Map<String, Object> buildParsePayload(Map<String, Object> document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("documentId", document.get("id"));
        payload.put("kbId", document.get("kb_id"));
        payload.put("fileName", document.get("file_name"));
        payload.put("fileExt", document.get("file_ext"));
        payload.put("contentType", document.get("content_type"));
        payload.put("versionNo", document.get("version_no"));
        payload.put("docType", document.get("doc_type"));
        payload.put("securityLevel", document.get("security_level"));
        payload.put("minioBucket", document.get("minio_bucket"));
        payload.put("minioObjectName", document.get("minio_object_name"));
        payload.put("chunkSize", loadIntegerConfig("rag.chunk.size", RuntimeConfigDefaults.CHUNK_SIZE));
        payload.put("chunkOverlap", loadIntegerConfig("rag.chunk.overlap", RuntimeConfigDefaults.CHUNK_OVERLAP));
        payload.put("ragOverride", buildRagOverrideForPython());
        return payload;
    }

    private void submitParseJob(Long documentId, Map<String, Object> payload) {
        PARSE_EXECUTOR.submit(() -> runParseJob(documentId, payload));
    }

    private void runParseJob(Long documentId, Map<String, Object> payload) {
        String fileName = String.valueOf(payload.getOrDefault("fileName", documentId));
        try {
            markChunkTaskRunning(documentId);
            updateDocumentParseState(documentId, "PROCESSING", "后台正在解析文档并建立索引");
            Map<String, Object> response = invokePythonKnowledgeParse(payload);
            Object messageObj = response.get("message");
            boolean isErrorResponse = false;
            if (messageObj instanceof Map map && "error".equals(map.get("phase"))) {
                isErrorResponse = true;
            } else if (messageObj instanceof String s && s.contains("解析失败")) {
                isErrorResponse = true;
            }
            if (isErrorResponse) {
                String errMsg = messageObj instanceof Map map ? String.valueOf(map.getOrDefault("msg", "解析失败")) : String.valueOf(messageObj);
                updateDocumentParseState(documentId, "FAILED", trimForDb("解析失败: " + errMsg));
                completeChunkTask(documentId, "FAILED", errMsg);
                operationLogService.log("知识库管理", "PARSE_DOCUMENT", "后台解析失败:" + fileName, "FAILED");
            } else {
                updateDocumentParseResult(documentId, response, "SUCCESS");
                completeChunkTask(documentId, "SUCCESS", null);
                operationLogService.log("知识库管理", "PARSE_DOCUMENT", "后台解析完成:" + fileName, "SUCCESS");
            }
        } catch (Exception ex) {
            String message = trimForDb("解析失败: " + ex.getMessage());
            updateDocumentParseState(documentId, "FAILED", message);
            completeChunkTask(documentId, "FAILED", message);
            operationLogService.log("知识库管理", "PARSE_DOCUMENT", "后台解析失败:" + fileName, "FAILED");
            log.warn("Background parse failed: documentId={}, fileName={}, err={}", documentId, fileName, ex.getMessage());
        }
    }

    private void updateDocumentParseState(Long documentId, String status, String message) {
        kbDocumentMapper.updateParseState(documentId, status, trimForDb(message), 0, 0);
    }

    private void validateDocumentReprocessable(Long documentId, Map<String, Object> document) {
        if (document == null || document.isEmpty()) {
            throw new ResourceNotFoundException("doc.not.found", documentId);
        }
        Object deletedValue = document.get("deleted");
        boolean deleted = deletedValue instanceof Number number
            ? number.intValue() != 0
            : "1".equals(String.valueOf(deletedValue));
        if (deleted) {
            throw new DataConflictException("doc.parse.failed", "文档已删除");
        }
        String parseStatus = String.valueOf(document.getOrDefault("parse_status", "")).trim().toUpperCase();
        if ("PENDING".equals(parseStatus) || "PROCESSING".equals(parseStatus)) {
            throw new KnowledgeTaskConflictException("doc.reprocess.failed");
        }
    }

    private void claimDocumentForReprocess(Long documentId) {
        int updated = kbDocumentMapper.claimForReprocess(documentId);
        if (updated > 0) {
            return;
        }
        String currentStatus = kbDocumentMapper.selectParseStatus(documentId);
        if ("PENDING".equalsIgnoreCase(currentStatus) || "PROCESSING".equalsIgnoreCase(currentStatus)) {
            throw new KnowledgeTaskConflictException("doc.reprocess.failed");
        }
        throw new DataConflictException("doc.reprocess.failed");
    }

    private void resetChunkTask(Long documentId) {
        int updated = kbDocumentChunkTaskMapper.resetForReprocess(documentId, "INDEX", "PENDING");
        if (updated == 0) {
            kbDocumentChunkTaskMapper.insertInitial(documentId, "INDEX", "PENDING");
        }
    }

    private void markChunkTaskRunning(Long documentId) {
        kbDocumentChunkTaskMapper.markRunning(documentId);
    }

    private void completeChunkTask(Long documentId, String status, String errorMessage) {
        kbDocumentChunkTaskMapper.complete(documentId, status, trimForDb(errorMessage));
    }

    private void registerAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private String trimForDb(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.length() > 900 ? text.substring(0, 900) : text;
    }

    /**
     * 获取文档的 MinIO 文件流信息，供下载或预览。
     */
    @Override
    public DocFileInfoVO getDocumentFileInfo(Long documentId) {
        Map<String, Object> row = kbDocumentMapper.selectFileInfo(documentId);
        if (row == null || row.isEmpty()) {
            throw new ResourceNotFoundException("doc.not.found", documentId);
        }
        String bucket = readString(row, "minioBucket", "minio_bucket");
        String objectName = readString(row, "minioObjectName", "minio_object_name");
        if (bucket == null || bucket.isBlank() || objectName == null || objectName.isBlank()) {
            log.warn("Document {} missing MinIO info: row={}", documentId, row);
            throw new DataConflictException("kb.download.minio.failed");
        }
        java.io.InputStream stream;
        try {
            stream = minioClient.getObject(
                io.minio.GetObjectArgs.builder().bucket(bucket).object(objectName).build()
            );
        } catch (Exception e) {
            log.warn("MinIO getObject failed: bucket={}, object={}", bucket, objectName, e);
            throw new DataConflictException("kb.download.minio.failed");
        }
        DocFileInfoVO vo = new DocFileInfoVO();
        vo.setStream(stream);
        // selectFileInfo 列无别名, Map 键为 snake_case, 需双键兼容读取 (否则文件名返回 "null")
        String contentType = readString(row, "contentType", "content_type");
        vo.setContentType(contentType != null ? contentType : "application/octet-stream");
        String fileName = readString(row, "fileName", "file_name");
        vo.setFileName(fileName != null ? fileName : ("doc-" + documentId));
        Object fileSize = row.get("fileSize") != null ? row.get("fileSize") : row.get("file_size");
        vo.setFileSize(asLong(fileSize));
        Object uploadedAt = row.get("uploadedAt") != null ? row.get("uploadedAt") : row.get("uploaded_at");
        vo.setUploadedAt(asDateTime(uploadedAt));
        return vo;
    }

    private String readString(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = row.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return null;
    }

    /**
     * 查询文档解析进度（含阶段详情+处理日志）。
     *
     * <p>先读取数据库中的文档状态，再根据文档类型构建阶段列表，并调用 Python 获取实时日志。
     */
    @Override
    public DocParseProgressVO getDocumentParseProgress(Long documentId) {
        Map<String, Object> doc = kbDocumentMapper.selectParseProgress(documentId);

        String parseStatus = String.valueOf(doc.getOrDefault("parse_status", "")).trim().toUpperCase();
        String fileExt = String.valueOf(doc.getOrDefault("file_ext", "txt")).trim().toLowerCase();

        Map<String, Object> pyResult = fetchFromPython(documentId);
        if (pyResult != null && hasMeaningfulProgress(pyResult)) {
            return mergePythonProgress(documentId, doc, pyResult, parseStatus, fileExt);
        }

        return buildFallbackProgress(documentId, doc, parseStatus, fileExt);
    }

    /** 合并 Python 端实时进度, 保留 Map 形态字段. */
    @SuppressWarnings("unchecked")
    private DocParseProgressVO mergePythonProgress(Long documentId, Map<String, Object> doc,
                                                   Map<String, Object> pyResult, String parseStatus, String fileExt) {
        DocParseProgressVO vo = new DocParseProgressVO();
        vo.setDocumentId(documentId);
        Object stages = pyResult.get("stages");
        vo.setStages(stages instanceof List ? (List<Map<String, Object>>) stages : List.of());
        Object logs = pyResult.get("logs");
        vo.setLogs(logs instanceof List ? (List<Map<String, Object>>) logs : List.of());
        Object progress = pyResult.get("overallProgress");
        if (progress instanceof Number n) vo.setOverallProgress(n.intValue());
        vo.setCurrentStage(pyResult.get("currentStage") == null ? "" : String.valueOf(pyResult.get("currentStage")));
        vo.setFileName(doc.get("file_name") == null ? null : String.valueOf(doc.get("file_name")));
        vo.setFileExt(fileExt);
        Object fileSize = doc.get("file_size");
        vo.setFileSize(asLong(fileSize));
        vo.setParseStatus(parseStatus);
        vo.setUpdatedAt(java.time.LocalDateTime.now());
        return vo;
    }

    private Map<String, Object> fetchFromPython(Long documentId) {
        try {
            String pythonRagBaseUrl = resolvePythonRagBaseUrl();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(pythonRagBaseUrl) + "/system/parse-logs?documentId=" + documentId))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200 && resp.body() != null) {
                return objectMapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception ex) {
            log.debug("Fetch parse progress from python failed: doc={} err={}", documentId, ex.getMessage());
        }
        return null;
    }

    private boolean hasMeaningfulProgress(Map<String, Object> pyResult) {
        if (pyResult == null || pyResult.isEmpty()) {
            return false;
        }
        Object stagesObj = pyResult.get("stages");
        if (stagesObj instanceof java.util.List<?> stages && !stages.isEmpty()) {
            for (Object s : stages) {
                if (s instanceof Map<?, ?> stage) {
                    Object p = stage.get("progress");
                    if (p instanceof Number num && num.intValue() > 0) {
                        return true;
                    }
                    Object st = stage.get("status");
                    if ("completed".equals(st) || "running".equals(st) || "failed".equals(st)) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private DocParseProgressVO buildFallbackProgress(Long documentId, Map<String, Object> doc, String parseStatus, String fileExt) {
        DocParseProgressVO vo = new DocParseProgressVO();
        vo.setDocumentId(documentId);
        vo.setFileName(doc.get("file_name") == null ? null : String.valueOf(doc.get("file_name")));
        vo.setFileExt(fileExt);
        vo.setFileSize(asLong(doc.get("file_size")));
        vo.setParseStatus(parseStatus);

        java.util.List<Map<String, Object>> stages = buildParseStagesFallback(parseStatus, fileExt);
        vo.setStages(stages);
        vo.setOverallProgress(computeOverallProgress(parseStatus, stages));
        vo.setCurrentStage(resolveCurrentStage(stages));

        java.util.List<Map<String, Object>> logs = new java.util.ArrayList<>();
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("timestamp", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.put("stageId", "");
        log.put("level", parseStatus.equals("SUCCESS") ? "info" : parseStatus.equals("FAILED") ? "error" : "info");
        log.put("message", doc.getOrDefault("parse_message",
            parseStatus.equals("SUCCESS") ? "解析完成" : parseStatus.equals("FAILED") ? "解析失败" : "等待解析..."));
        logs.add(log);
        vo.setLogs(logs);
        vo.setUpdatedAt(java.time.LocalDateTime.now());
        return vo;
    }

    private java.util.List<Map<String, Object>> buildParseStagesFallback(String parseStatus, String fileExt) {
        java.util.List<Map<String, Object>> stages = new java.util.ArrayList<>();
        java.util.List<String> stageIds = getStageIdsByFileType(fileExt);
        String[] stageNames = {"文档下载", "文本提取", "OCR识别", "文本切块", "向量嵌入", "向量索引写入"};
        int lastIdx = stageIds.size() - 1;

        int order = 0;
        for (String stageId : stageIds) {
            Map<String, Object> stage = new LinkedHashMap<>();
            stage.put("stageId", stageId);
            int nameIdx = Math.min(order, stageNames.length - 1);
            stage.put("stageName", stageNames[nameIdx]);
            stage.put("order", order);

            if ("SUCCESS".equals(parseStatus)) {
                stage.put("status", "completed");
                stage.put("progress", 100);
            } else if ("FAILED".equals(parseStatus)) {
                if (order < lastIdx) {
                    stage.put("status", "completed");
                    stage.put("progress", 100);
                } else {
                    stage.put("status", "failed");
                    stage.put("progress", 0);
                }
            } else if ("PENDING".equals(parseStatus)) {
                stage.put("status", "pending");
                stage.put("progress", 0);
            } else if ("PROCESSING".equals(parseStatus)) {
                if (order == 0) {
                    stage.put("status", "running");
                    stage.put("progress", 5);
                } else {
                    stage.put("status", "pending");
                    stage.put("progress", 0);
                }
            } else {
                stage.put("status", "pending");
                stage.put("progress", 0);
            }
            stages.add(stage);
            order++;
        }
        return stages;
    }

    private java.util.List<String> getStageIdsByFileType(String fileExt) {
        java.util.List<String> stageIds = new java.util.ArrayList<>();
        stageIds.add("download");
        stageIds.add("extract");
        boolean needsOcr = "pdf".equals(fileExt) || "png".equals(fileExt) || "jpg".equals(fileExt)
            || "jpeg".equals(fileExt) || "tiff".equals(fileExt) || "bmp".equals(fileExt);
        if (needsOcr) {
            stageIds.add("ocr");
        }
        stageIds.add("chunk");
        stageIds.add("embedding");
        stageIds.add("es_index");
        return stageIds;
    }

    private int computeOverallProgress(String parseStatus, java.util.List<Map<String, Object>> stages) {
        if ("SUCCESS".equals(parseStatus)) return 100;
        if ("FAILED".equals(parseStatus)) return 0;
        if ("PENDING".equals(parseStatus) || stages.isEmpty()) return 0;
        int total = stages.size();
        int completed = 0;
        for (Map<String, Object> stage : stages) {
            if ("completed".equals(String.valueOf(stage.getOrDefault("status", "pending")))) completed++;
        }
        return total == 0 ? 0 : Math.min(99, (completed * 100) / total);
    }

    private String resolveCurrentStage(java.util.List<Map<String, Object>> stages) {
        for (Map<String, Object> stage : stages) {
            if ("running".equals(String.valueOf(stage.getOrDefault("status", "pending")))) {
                return String.valueOf(stage.getOrDefault("stageId", ""));
            }
        }
        for (Map<String, Object> stage : stages) {
            if ("pending".equals(String.valueOf(stage.getOrDefault("status", "pending")))) {
                return String.valueOf(stage.getOrDefault("stageId", ""));
            }
        }
        return stages.isEmpty() ? "" : String.valueOf(stages.get(stages.size() - 1).getOrDefault("stageId", ""));
    }

    private String resolvePythonRagBaseUrl() {
        Map<String, String> dynamicConfig = loadDynamicConfigInternal();
        return resolveString(dynamicConfig, "service.python.rag.baseUrl", externalServiceProperties.getPython().getRag().getBaseUrl());
    }

    private Map<String, String> loadDynamicConfigInternal() {
        try {
            List<Map<String, Object>> rows = sysConfigMapper.listAllKeyValue();
            Map<String, String> map = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Object k = row.get("configKey");
                Object v = row.get("configValue");
                if (k != null) {
                    map.put(String.valueOf(k), v == null ? null : String.valueOf(v));
                }
            }
            return map;
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private String resolveString(Map<String, String> dynamicConfig, String key, String defaultValue) {
        String direct = dynamicConfig.get(key);
        if (direct != null) {
            return direct;
        }
        int idx = key.indexOf('.');
        if (idx > 0 && idx < key.length() - 1) {
            String fallbackKey = key.substring(idx + 1);
            String fallback = dynamicConfig.get(fallbackKey);
            if (fallback != null) {
                return fallback;
            }
        }
        return defaultValue;
    }

    /**
     * 强制终止文档解析任务。
     */
    @Override
    public BaseOpResultVO cancelDocumentParse(Long documentId) {
        BaseOpResultVO vo = new BaseOpResultVO();
        vo.setTargetId(documentId);
        try {
            String pythonRagBaseUrl = resolvePythonRagBaseUrl();
            Map<String, Object> bodyMap = new LinkedHashMap<>();
            bodyMap.put("documentId", documentId);
            String jsonBody = objectMapper.writeValueAsString(bodyMap);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(pythonRagBaseUrl) + "/system/cancel-parse"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200) {
                vo.setSuccess(Boolean.TRUE);
                vo.setStatus("SUCCESS");
                vo.setMessage("任务已终止，向量索引已回滚");
            } else {
                vo.setSuccess(Boolean.FALSE);
                vo.setStatus("FAILED");
                vo.setMessage("终止请求被拒绝");
            }
        } catch (Exception ex) {
            vo.setSuccess(Boolean.FALSE);
            vo.setStatus("FAILED");
            vo.setMessage("终止失败: " + ex.getMessage());
        }
        kbDocumentMapper.forceStopIfProcessing(documentId, "任务已被用户强制终止");
        try {
            invokePythonDeleteByDocumentId(String.valueOf(documentId));
        } catch (Exception ex) {
            log.warn("Cancel: ES cleanup failed for doc={}: {}", documentId, ex.getMessage());
        }
        return vo;
    }

    /**
     * 根据 ID 查询知识库详情。
     */
    private KnowledgeBaseVO getKnowledgeBaseById(Long id) {
        KbKnowledgeBaseEntity e = kbKnowledgeBaseMapper.selectActiveById(id);
        if (e == null) {
            throw new ResourceNotFoundException("kb.not.found", id);
        }
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        vo.setId(e.getId());
        vo.setKbCode(e.getKbCode());
        vo.setKbName(e.getKbName());
        vo.setKbDesc(e.getKbDesc());
        vo.setVisibilityScope(e.getVisibilityScope());
        vo.setStatus(e.getStatus());
        vo.setCreatedAt(e.getCreatedAt());
        vo.setUpdatedAt(e.getUpdatedAt());
        return vo;
    }

    /**
     * 确保文档桶存在。
     */
    private void ensureBucketExists() {
        try {
            String bucket = storageProperties.getMinio().getBucket().getDocuments();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new DataConflictException("kb.upload.bucket.failed");
        }
    }

    /**
     * 构造对象存储的 objectName（按 kbId/日期 分目录）。
     */
    private String buildObjectName(Long kbId, String fileName) {
        return "kb-" + kbId + "/" + LocalDate.now() + "/" + UUID.randomUUID() + "-" + fileName;
    }

    /**
     * 从文件名提取扩展名（小写）。
     */
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "txt";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private void validateUploadFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new KnowledgeUploadException("upload.file.empty");
        }
        long maxFileSizeBytes = knowledgeUploadProperties.getMaxFileSize().toBytes();
        long maxRequestSizeBytes = knowledgeUploadProperties.getMaxRequestSize().toBytes();
        Set<String> allowedExtensions = new HashSet<>();
        for (String item : knowledgeUploadProperties.getAllowedExtensions()) {
            if (item != null && !item.isBlank()) {
                allowedExtensions.add(item.trim().toLowerCase());
            }
        }

        long totalBytes = 0L;
        for (MultipartFile file : files) {
            String fileName = file == null ? "" : String.valueOf(file.getOriginalFilename());
            if (file == null || file.isEmpty() || file.getSize() <= 0) {
                throw new KnowledgeUploadException("upload.file.empty.name", fileName);
            }
            long fileSize = file.getSize();
            if (fileSize > maxFileSizeBytes) {
                throw new KnowledgeUploadException("doc.upload.too.large");
            }
            totalBytes += fileSize;
            if (totalBytes > maxRequestSizeBytes) {
                throw new KnowledgeUploadException("upload.request.too.large", formatBytes(maxRequestSizeBytes));
            }
            if (!allowedExtensions.isEmpty()) {
                String extension = extractExtension(fileName);
                if (!allowedExtensions.contains(extension)) {
                    throw new KnowledgeUploadException(
                        "upload.file.type.invalid",
                        fileName + " (支持: " + String.join("、", allowedExtensions) + ")"
                    );
                }
            }
        }
    }

    /**
     * 读取整数配置（sys_config），读取失败则回退默认值。
     */
    private int loadIntegerConfig(String key, int defaultValue) {
        try {
            String value = sysConfigMapper.selectStringValue(key);
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private Long loadLongConfig(String key) {
        try {
            String value = sysConfigMapper.selectStringValue(key);
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String loadStringConfig(String key, String defaultValue) {
        try {
            String value = sysConfigMapper.selectStringValue(key);
            return value == null || value.isBlank() ? defaultValue : value;
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private boolean loadBooleanConfig(String key, boolean defaultValue) {
        String value = loadStringConfig(key, String.valueOf(defaultValue));
        String trimmed = value == null ? "" : value.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return defaultValue;
        }
        return "true".equals(trimmed) || "1".equals(trimmed) || "yes".equals(trimmed) || "y".equals(trimmed);
    }

    private Map<String, Object> buildRagOverrideForPython() {
        Map<String, Object> ragOverride = new LinkedHashMap<>();
        Map<String, Object> embedding = new LinkedHashMap<>();
        embedding.put(
            "modelPath",
            loadStringConfig("rag.embedding.model.path", RuntimeConfigDefaults.EMBEDDING_MODEL_PATH)
        );
        embedding.put("device", loadStringConfig("rag.embedding.device", RuntimeConfigDefaults.EMBEDDING_DEVICE));
        embedding.put("dims", loadIntegerConfig("rag.embedding.dims", RuntimeConfigDefaults.EMBEDDING_DIMS));
        embedding.put("batchSize", loadIntegerConfig("rag.embedding.batchSize", RuntimeConfigDefaults.EMBEDDING_BATCH_SIZE));
        embedding.put("normalizeEmbeddings", loadBooleanConfig("rag.embedding.normalizeEmbeddings", RuntimeConfigDefaults.EMBEDDING_NORMALIZE));
        ragOverride.put("embedding", embedding);
        return ragOverride;
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte item : bytes) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        double kb = 1024d;
        if (bytes >= kb * kb * kb) { // GB
            return String.format("%.1f GB", bytes / kb / kb / kb);
        }
        if (bytes >= kb * kb) { // MB
            return String.format("%.1f MB", bytes / kb / kb);
        }
        if (bytes >= kb) { // KB
            return String.format("%.1f KB", bytes / kb);
        }
        return bytes + " B";
    }

    /** 安全转换 Object -> Long. */
    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 安全转换 Object -> LocalDateTime. */
    private LocalDateTime asDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime t) {
            return t;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (value instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignore) {
            try {
                return LocalDateTime.parse(text.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ignore2) {
                return null;
            }
        }
    }
}
