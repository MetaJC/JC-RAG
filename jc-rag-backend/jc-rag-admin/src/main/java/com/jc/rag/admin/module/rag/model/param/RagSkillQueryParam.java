package com.jc.rag.admin.module.rag.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * Skill 分页查询 Param。
 * <p>分页字段 {@code pageNum/pageSize} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class RagSkillQueryParam extends PageQueryParam {

    private String keyword;

    private Integer status;
}
