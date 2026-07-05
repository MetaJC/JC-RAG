package com.jc.rag.domain.mapper.rag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.rag.RagSkillEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * Skill Mapper。
 * @author JC
 */
public interface RagSkillMapper extends BaseMapper<RagSkillEntity> {

    long countSkills(@Param("status") Integer status, @Param("keyword") String keyword);

    IPage<RagSkillEntity> pageSkills(IPage<RagSkillEntity> page,
                                     @Param("status") Integer status,
                                     @Param("keyword") String keyword);

    List<RagSkillEntity> listSkills(
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );

    int insertSkill(Map<String, Object> row);

    int updateSkill(@Param("id") Long id, @Param("row") Map<String, Object> row);

    int updateSkillStatus(@Param("id") Long id, @Param("status") Integer status);

    int deleteSkill(@Param("id") Long id);
}
