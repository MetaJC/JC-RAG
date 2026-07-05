package com.jc.rag.domain.mapper.knowledge;

import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbDocumentChunkTaskEntity;
import org.apache.ibatis.annotations.Param;

/**
 * 文档切块任务 Mapper。
 * @author JC
 */
public interface KbDocumentChunkTaskMapper extends BaseMapper<KbDocumentChunkTaskEntity> {

    /**
     * 插入初始任务
     */
    int insertInitial(@Param("documentId") Long documentId,
                      @Param("taskType") String taskType,
                      @Param("taskStatus") String taskStatus);

    /**
     * UPSERT: 重置任务 (retry_count+1, 清空时间)
     */
    int resetForReprocess(@Param("documentId") Long documentId,
                          @Param("taskType") String taskType,
                          @Param("taskStatus") String taskStatus);

    /**
     * 标记任务运行中
     */
    int markRunning(@Param("documentId") Long documentId);

    /**
     * 完成任务 (SUCCESS / FAILED)
     */
    int complete(@Param("documentId") Long documentId,
                 @Param("status") String status,
                 @Param("errorMessage") String errorMessage);

    /**
     * 查询任务当前状态
     */
    String selectTaskStatus(@Param("documentId") Long documentId);
}
