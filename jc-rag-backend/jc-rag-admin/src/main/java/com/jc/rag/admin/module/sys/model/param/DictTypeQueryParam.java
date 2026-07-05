package com.jc.rag.admin.module.sys.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

/**
 * 字典类型查询 Param。
 * <p>分页字段 {@code page/size} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class DictTypeQueryParam extends PageQueryParam {

    private String keyword;
}
