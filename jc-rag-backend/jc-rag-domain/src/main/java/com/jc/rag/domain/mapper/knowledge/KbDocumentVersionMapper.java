package com.jc.rag.domain.mapper.knowledge;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbDocumentVersionEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 文档版本 Mapper。
 * @author JC
 */
public interface KbDocumentVersionMapper extends BaseMapper<KbDocumentVersionEntity> {

    /**
     * 新增 (返回主键到 entity.id)
     */
    int insertVersion(KbDocumentVersionEntity entity);

    /**
     * 查询某文档最新版本号
     */
    Integer selectMaxVersionNo(@Param("documentId") Long documentId);
}
