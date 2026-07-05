package com.jc.rag.domain.mapper.rag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rag.RagPromptEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Prompt Mapper。
 * @author JC
 */
public interface RagPromptMapper extends BaseMapper<RagPromptEntity> {

    long countPrompts(@Param("scope") String scope, @Param("keyword") String keyword);

    IPage<RagPromptEntity> pagePrompts(IPage<RagPromptEntity> page,
                                       @Param("scope") String scope,
                                       @Param("keyword") String keyword);

    List<RagPromptEntity> listPrompts(
            @Param("scope") String scope,
            @Param("keyword") String keyword
    );

    Map<String, Object> getPrompt(@Param("id") Long id);

    int insertPrompt(Map<String, Object> row);

    int updatePrompt(@Param("id") Long id, @Param("row") Map<String, Object> row);

    int deletePrompt(@Param("id") Long id);
}
