package com.jc.rag.domain.mapper.knowledge;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jc.rag.common.model.base.BaseMapper;
import com.jc.rag.domain.entity.knowledge.KbKnowledgeBaseEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 知识库 Mapper。
 * @author JC
 */
public interface KbKnowledgeBaseMapper extends BaseMapper<KbKnowledgeBaseEntity> {

    /**
     * 查询启用状态单条 (status=1)
     */
    KbKnowledgeBaseEntity selectActiveById(@Param("id") Long id);

    /**
     * 按 code 校验唯一性
     */
    int countByCode(@Param("kbCode") String kbCode, @Param("excludeId") Long excludeId);

    /**
     * 新增 (返回主键到 entity.id)
     */
    int insertKb(KbKnowledgeBaseEntity entity);

    /**
     * 更新基本信息 (kb_name / kb_desc / visibility_scope)
     */
    int updateBasicInfo(@Param("id") Long id,
                        @Param("kbName") String kbName,
                        @Param("kbDesc") String kbDesc,
                        @Param("visibilityScope") String visibilityScope);

    /**
     * 更新 client_selectable 单字段
     */
    int updateClientSelectable(@Param("id") Long id, @Param("clientSelectable") Integer clientSelectable);

    /**
     * 更新 visibility_scope 单字段
     */
    int updateVisibilityScope(@Param("id") Long id, @Param("visibilityScope") String visibilityScope);

    /**
     * 软删: status=0
     */
    int disableById(@Param("id") Long id);

    /**
     * 分页查询 (keyword 匹配 kb_code/kb_name)
     */
    IPage<KbKnowledgeBaseEntity> pageList(IPage<KbKnowledgeBaseEntity> page,
                                           @Param("keyword") String keyword);

    /**
     * 分页总数
     */
    long countList(@Param("keyword") String keyword);

    /**
     * 列出所有启用的 KB, 并对 defaultKbId 加计算列 isDefault (供 C 端会话选择器).
     */
    List<Map<String, Object>> listActiveWithDefault(@Param("defaultKbId") Long defaultKbId);

    /**
     * 列出公开且允许客户端选择的 KB (供 SELECTABLE 模式).
     */
    List<KbKnowledgeBaseEntity> listPublicSelectable();

    /**
     * 查第一个启用的 KB ID (默认 KB 兜底).
     */
    Long selectFirstActiveId();
}
