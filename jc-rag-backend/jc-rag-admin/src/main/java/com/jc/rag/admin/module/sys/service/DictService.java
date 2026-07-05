package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.DictItemCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictQueryParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeQueryParam;
import com.jc.rag.admin.module.sys.model.vo.DictItemVO;
import com.jc.rag.admin.module.sys.model.vo.DictTypeVO;
import com.jc.rag.common.web.page.PageResponse;

/**
 * 字典管理 Service
 * @author JC
 */
public interface DictService {

    PageResponse<DictTypeVO> pageDictTypes(DictTypeQueryParam param);

    PageResponse<DictItemVO> pageDictItems(DictQueryParam param);

    Long createDictType(DictTypeCreateParam param);

    Long createDictItem(DictItemCreateParam param);
}
