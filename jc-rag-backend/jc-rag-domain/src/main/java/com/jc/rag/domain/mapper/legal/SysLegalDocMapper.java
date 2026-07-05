package com.jc.rag.domain.mapper.legal;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.legal.SysLegalDocEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 法律文档 Mapper (admin/user 共用)。
 *
 * @author JC
 */
public interface SysLegalDocMapper extends BaseMapper<SysLegalDocEntity> {

    /** 列出全部法律文档 (无分页, 数量极少) */
    List<Map<String, Object>> listAll();

    /** 按 docType 查询单条 */
    List<Map<String, Object>> findByDocType(@Param("docType") String docType);

    /** 按 docType 查询已发布的单条 (公共接口) */
    Map<String, Object> findPublishedByDocType(@Param("docType") String docType);

    /** 按 docType 更新 */
    int updateByDocType(@Param("docType") String docType,
                        @Param("title") String title,
                        @Param("contentMd") String contentMd,
                        @Param("status") String status,
                        @Param("updatedBy") Long updatedBy);

    /** 新增 */
    int insertDoc(@Param("docType") String docType,
                  @Param("title") String title,
                  @Param("contentMd") String contentMd,
                  @Param("status") String status,
                  @Param("updatedBy") Long updatedBy);

    /** 发布: 设置 status=PUBLISHED, published_at/published_by */
    int publish(@Param("docType") String docType,
                @Param("userId") Long userId);

    /** 首次发布时设置 published_at (COALESCE 保证只第一次写) */
    int markPublished(@Param("docType") String docType,
                      @Param("userId") Long userId);
}
