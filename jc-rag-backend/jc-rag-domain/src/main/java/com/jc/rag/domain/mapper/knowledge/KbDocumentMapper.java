package com.jc.rag.domain.mapper.knowledge;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbDocumentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 文档元数据 Mapper。
 * @author JC
 */
public interface KbDocumentMapper extends BaseMapper<KbDocumentEntity> {

    /** 单条详情 (含 minio 访问信息) */
    Map<String, Object> selectFileInfo(@Param("id") Long id);

    /** 重建索引所需字段 */
    Map<String, Object> selectForReprocess(@Param("id") Long id);

    /** 解析进度字段子集 */
    Map<String, Object> selectParseProgress(@Param("id") Long id);

    /** 文档基本信息 (含 chunk_count / page_count) */
    Map<String, Object> selectDocSummary(@Param("id") Long id);

    /** 列出脏 parse_message (供启动时清洗) */
    List<Map<String, Object>> listDirtyParseMessages();

    /** 清洗单条 parse_message */
    int updateParseMessage(@Param("id") Long id, @Param("parseMessage") String parseMessage);

    /** 新增 (返回主键到 entity.id) */
    int insertDoc(KbDocumentEntity entity);

    /** 软删单条 */
    int markDeleted(@Param("id") Long id);

    /** 按 KB 软删全部 */
    int markDeletedByKbId(@Param("kbId") Long kbId);

    /** 重命名 */
    int updateFileName(@Param("id") Long id, @Param("fileName") String fileName);

    /** 解析完成 / 失败状态机写入 */
    int updateParseState(@Param("id") Long id,
                         @Param("status") String status,
                         @Param("message") String message,
                         @Param("chunkCount") Integer chunkCount,
                         @Param("pageCount") Integer pageCount);

    /** CAS: 仅 PROCESSING 状态才能标记 PENDING (重处理) */
    int markRunning(@Param("id") Long id);

    /** CAS 重建索引: NOT IN (PENDING/PROCESSING) 改为 PROCESSING */
    int claimForReprocess(@Param("id") Long id);

    /** CAS: 仅 PROCESSING 状态才能强制停止 */
    int forceStopIfProcessing(@Param("id") Long id, @Param("message") String message);

    /** 读单条 parse_status (CAS 后验证) */
    String selectParseStatus(@Param("id") Long id);

    /** KB 下文档列表 (含/不含已删除) */
    List<KbDocumentEntity> listByKbId(@Param("kbId") Long kbId,
                                      @Param("includeDeleted") boolean includeDeleted);

    /** KB 下文档 + minio 信息 (删除 KB 时清理用) */
    List<Map<String, Object>> listMinioInfoByKbId(@Param("kbId") Long kbId);

    /** 分页查询 (keyword 匹配 file_name, parseStatus 可选) */
    IPage<KbDocumentEntity> pageList(IPage<KbDocumentEntity> page,
                                     @Param("kbId") Long kbId,
                                     @Param("keyword") String keyword,
                                     @Param("parseStatus") String parseStatus);

    /** 分页总数 */
    long countList(@Param("kbId") Long kbId,
                   @Param("keyword") String keyword,
                   @Param("parseStatus") String parseStatus);
}
